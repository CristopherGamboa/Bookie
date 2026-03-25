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
 * Azure Function for retrieving all loans.
 * Handles GET requests only at the "loans" endpoint.
 * Single Responsibility: Retrieve and list loan records from the LOANS table.
 */
public class GetLoansFunction {

    @FunctionName("GetLoansFunction")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "loans")
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("GetLoansFunction - GET /loans");
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseUtil.getConnection();
            stmt = conn.createStatement();
            String query = "SELECT USER_ID, BOOK_TITLE FROM LOANS";
            rs = stmt.executeQuery(query);
            
            JsonArray loans = new JsonArray();
            while (rs.next()) {
                JsonObject loan = new JsonObject();
                loan.addProperty("userId", rs.getString("USER_ID"));
                loan.addProperty("bookTitle", rs.getString("BOOK_TITLE"));
                loans.add(loan);
            }
            
            context.getLogger().info("Retrieved " + loans.size() + " loans from database");
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(DatabaseUtil.createDataResponse(loans))
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
