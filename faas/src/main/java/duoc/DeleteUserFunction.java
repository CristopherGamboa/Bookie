package duoc;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Azure Function for deleting users.
 * Handles DELETE requests at the "users/{userId}" endpoint.
 * 
 * Responsibilities:
 * 1. Delete user record from the USERS table (cascades to LOANS)
 * 2. Publish a user deletion event to Azure Event Grid
 * 3. Ensure database deletion is successful before attempting event publication
 * 4. Handle event grid failures gracefully (don't fail the API response if event publication fails)
 */
public class DeleteUserFunction {

    @FunctionName("DeleteUserFunction")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.DELETE},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "users/{userId}")
                HttpRequestMessage<Optional<String>> request,
            @BindingName("userId")
            final String userId,
            final ExecutionContext context) {
        
        context.getLogger().info("DeleteUserFunction - DELETE /users/" + userId);
        
        // Validate userId parameter
        if (userId == null || userId.trim().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body(DatabaseUtil.createErrorJson("userId is required"))
                    .build();
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            // Convert userId to Long for validation
            Long parsedUserId = Long.parseLong(userId);
            
            conn = DatabaseUtil.getConnection();
            
            // Delete user from USERS table (ON DELETE CASCADE will handle LOANS)
            String deleteQuery = "DELETE FROM USERS WHERE USER_ID = ?";
            stmt = conn.prepareStatement(deleteQuery);
            stmt.setLong(1, parsedUserId);
            
            int rowsAffected = stmt.executeUpdate();
            
            // Check if user was found and deleted
            if (rowsAffected == 0) {
                context.getLogger().warning("User not found: " + userId);
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .header("Content-Type", "application/json")
                        .body(DatabaseUtil.createErrorJson("User not found"))
                        .build();
            }
            
            context.getLogger().info("User deleted successfully: " + userId);
            
            // Publish event to Azure Event Grid (non-blocking, errors are logged but don't fail the response)
            publishUserDeletedEvent(parsedUserId, context);
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(DatabaseUtil.createSuccessJson("User deleted successfully"))
                    .build();
                    
        } catch (NumberFormatException e) {
            context.getLogger().severe("Invalid userId format: " + userId);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body(DatabaseUtil.createErrorJson("userId must be a valid number"))
                    .build();
        } catch (SQLException e) {
            context.getLogger().severe("Database error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body(DatabaseUtil.createErrorJson("Database error: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Unexpected error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body(DatabaseUtil.createErrorJson("Unexpected error: " + e.getMessage()))
                    .build();
        } finally {
            DatabaseUtil.closeResources(null, stmt, conn);
        }
    }

    /**
     * Publishes a user deletion event to Azure Event Grid via HTTP POST.
     * Failures in event publication are logged but do not affect the main response,
     * as the database deletion has already been committed.
     * 
     * This method:
     * - Validates Event Grid credentials before attempting connection
     * - Uses a timeout of 10 seconds to prevent hanging requests
     * - Catches all exceptions to ensure they never propagate to the response
     * - Logs all failures as warnings, not errors, since the DB deletion succeeded
     * 
     * @param userId The ID of the deleted user
     * @param context The Azure Function execution context for logging
     */
    private void publishUserDeletedEvent(Long userId, ExecutionContext context) {
        try {
            // Retrieve Event Grid credentials from environment variables
            String eventGridEndpoint = System.getenv("EVENT_GRID_ENDPOINT");
            String eventGridKey = System.getenv("EVENT_GRID_KEY");
            
            // Validate Event Grid credentials
            if (eventGridEndpoint == null || eventGridKey == null) {
                context.getLogger().warning("Event Grid credentials not found (EVENT_GRID_ENDPOINT or EVENT_GRID_KEY). Event publication skipped for userId: " + userId);
                return;
            }
            
            context.getLogger().info("Attempting to publish deletion event to Event Grid for userId: " + userId);
            
            // Create event data (JSON payload with userId)
            JsonObject eventData = new JsonObject();
            eventData.addProperty("userId", userId);
            eventData.addProperty("timestamp", OffsetDateTime.now().toString());
            
            // Create EventGridEvent object
            JsonObject eventGridEvent = new JsonObject();
            eventGridEvent.addProperty("subject", "Usuarios/Eliminados");
            eventGridEvent.addProperty("eventType", "Biblioteca.Usuario.Eliminado");
            eventGridEvent.add("data", eventData);
            eventGridEvent.addProperty("dataVersion", "1.0");
            
            // Wrap in array (Event Grid expects an array of events)
            JsonArray events = new JsonArray();
            events.add(eventGridEvent);
            
            // Create HTTP request to Event Grid with timeout
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            
            String requestBody = events.toString();
            
            HttpRequest eventGridRequest = HttpRequest.newBuilder()
                    .uri(new URI(eventGridEndpoint))
                    .header("Content-Type", "application/json")
                    .header("aeg-sas-key", eventGridKey)
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            // Send the request
            HttpResponse<String> response = client.send(eventGridRequest, HttpResponse.BodyHandlers.ofString());
            
            // Check response status
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                context.getLogger().info("User deletion event published successfully to Event Grid for userId: " + userId + " (Status: " + response.statusCode() + ")");
            } else {
                context.getLogger().warning("Event Grid returned status " + response.statusCode() + 
                        " for userId: " + userId + ". Response body: " + response.body());
            }
            
        } catch (java.net.ConnectException e) {
            // Network connection error - don't propagate since DB delete succeeded
            context.getLogger().warning("Failed to connect to Event Grid for userId: " + userId + 
                    ". Connection error: " + e.getMessage() + ". DB deletion was successful, event publication skipped.");
        } catch (java.net.SocketTimeoutException e) {
            // Timeout error - don't propagate since DB delete succeeded
            context.getLogger().warning("Event Grid request timeout for userId: " + userId + 
                    ". DB deletion was successful, event publication skipped due to timeout.");
        } catch (Exception e) {
            // Catch all other exceptions - log as warning, don't propagate
            // This ensures that even if Event Grid has issues, the API response is still successful
            context.getLogger().warning("Failed to publish user deletion event to Event Grid for userId: " + userId + 
                    ". Exception type: " + e.getClass().getSimpleName() + ", Message: " + e.getMessage() + 
                    ". DB deletion was successful, event publication failed gracefully.");
        }
    }
}
