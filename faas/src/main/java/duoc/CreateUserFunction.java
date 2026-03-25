package duoc;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.sql.*;
import java.util.Optional;

/**
 * Azure Function for creating new users.
 * Handles POST requests only at the "users" endpoint.
 * Single Responsibility: Create user records in the USERS table.
 */
public class CreateUserFunction {

    private static final Gson gson = new Gson();

    @FunctionName("CreateUserFunction")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "users")
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("CreateUserFunction - POST /users");
        
        if (!request.getBody().isPresent()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body(DatabaseUtil.createErrorJson("Request body is required"))
                    .build();
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            String body = request.getBody().get();
            JsonObject userJson = gson.fromJson(body, JsonObject.class);
            
            // Validate required fields
            if (!userJson.has("name") || !userJson.has("documentId") || !userJson.has("email")) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .header("Content-Type", "application/json")
                        .body(DatabaseUtil.createErrorJson("Missing required fields: name, documentId, email"))
                        .build();
            }
            
            String name = userJson.get("name").getAsString();
            String documentId = userJson.get("documentId").getAsString();
            String email = userJson.get("email").getAsString();
            
            conn = DatabaseUtil.getConnection();
            String insertQuery = "INSERT INTO USERS (NAME, DOCUMENT_ID, EMAIL) VALUES (?, ?, ?)";
            stmt = conn.prepareStatement(insertQuery);
            stmt.setString(1, name);
            stmt.setString(2, documentId);
            stmt.setString(3, email);
            
            stmt.executeUpdate();
            
            context.getLogger().info("User created successfully: " + documentId);
            
            return request.createResponseBuilder(HttpStatus.CREATED)
                    .header("Content-Type", "application/json")
                    .body(DatabaseUtil.createSuccessJson("User created successfully"))
                    .build();
                    
        } catch (com.google.gson.JsonSyntaxException e) {
            context.getLogger().severe("Invalid JSON: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body(DatabaseUtil.createErrorJson("Invalid JSON format"))
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
}
