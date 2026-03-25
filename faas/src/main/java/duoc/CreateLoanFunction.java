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
 * Azure Function for creating new loans.
 * Handles POST requests only at the "loans" endpoint.
 * Single Responsibility: Create loan records in the LOANS table.
 */
public class CreateLoanFunction {

    private static final Gson gson = new Gson();

    @FunctionName("CreateLoanFunction")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "loans")
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("CreateLoanFunction - POST /loans");
        
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
            JsonObject loanJson = gson.fromJson(body, JsonObject.class);
            
            // Validate required fields
            if (!loanJson.has("userId") || !loanJson.has("bookTitle")) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .header("Content-Type", "application/json")
                        .body(DatabaseUtil.createErrorJson("Missing required fields: userId, bookTitle"))
                        .build();
            }
            
            String userId = loanJson.get("userId").getAsString();
            String bookTitle = loanJson.get("bookTitle").getAsString();
            
            conn = DatabaseUtil.getConnection();
            String insertQuery = "INSERT INTO LOANS (USER_ID, BOOK_TITLE) VALUES (?, ?)";
            stmt = conn.prepareStatement(insertQuery);
            stmt.setString(1, userId);
            stmt.setString(2, bookTitle);
            
            stmt.executeUpdate();
            
            context.getLogger().info("Loan created successfully for user: " + userId);
            
            return request.createResponseBuilder(HttpStatus.CREATED)
                    .header("Content-Type", "application/json")
                    .body(DatabaseUtil.createSuccessJson("Loan created successfully"))
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
