package duoc;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.*;
import java.util.Optional;

/**
 * Azure Function for retrieving all users.
 * Handles GET requests only at the "users" endpoint.
 * Single Responsibility: Retrieve and list user records from the USERS table.
 */
public class GetUsersFunction {

    @FunctionName("GetUsersFunction")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "users")
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("GetUsersFunction - GET /users");
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            stmt = conn.createStatement();
            String query = "SELECT NAME, DOCUMENT_ID, EMAIL FROM USERS";
            rs = stmt.executeQuery(query);
            
            JsonArray users = new JsonArray();
            while (rs.next()) {
                JsonObject user = new JsonObject();
                user.addProperty("name", rs.getString("NAME"));
                user.addProperty("documentId", rs.getString("DOCUMENT_ID"));
                user.addProperty("email", rs.getString("EMAIL"));
                users.add(user);
            }
            
            context.getLogger().info("Retrieved " + users.size() + " users from database");
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(DatabaseUtil.createDataResponse(users))
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
            DatabaseUtil.closeResources(rs, stmt, conn);
        }
    }
}
