package main.java.duoc;

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
import com.google.gson.JsonArray;

import java.sql.*;
import java.util.Optional;

/**
 * Azure Function for managing loans in the library system.
 * Handles GET (list loans) and POST (create loan) requests.
 */
public class LoanFunction {

    private static final Gson gson = new Gson();

    /**
     * Handles HTTP requests for loan management.
     * GET: List all loans
     * POST: Create a new loan
     */
    @FunctionName("LoanFunction")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "loans")
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("LoanFunction processed a request.");
        
        try {
            HttpMethod method = request.getHttpMethod();
            
            if (method == HttpMethod.GET) {
                return handleGetLoans(request, context);
            } else if (method == HttpMethod.POST) {
                return handleCreateLoan(request, context);
            } else {
                return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED)
                        .body(createErrorJson("Method not allowed"))
                        .build();
            }
        } catch (Exception e) {
            context.getLogger().severe("Error processing request: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorJson("Internal server error: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Handles GET requests - returns list of all loans
     */
    private HttpResponseMessage handleGetLoans(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
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
            
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("data", loans);
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(response.toString())
                    .build();
                    
        } catch (SQLException e) {
            context.getLogger().severe("Database error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body(createErrorJson("Database error: " + e.getMessage()))
                    .build();
        } finally {
            closeResources(rs, stmt, conn);
        }
    }

    /**
     * Handles POST requests - creates a new loan
     * Expected JSON body: {"userId": "...", "bookTitle": "..."}
     */
    private HttpResponseMessage handleCreateLoan(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        if (!request.getBody().isPresent()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body(createErrorJson("Request body is required"))
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
                        .body(createErrorJson("Missing required fields: userId, bookTitle"))
                        .build();
            }
            
            String userId = loanJson.get("userId").getAsString();
            String bookTitle = loanJson.get("bookTitle").getAsString();
            
            conn = getConnection();
            String insertQuery = "INSERT INTO LOANS (USER_ID, BOOK_TITLE) VALUES (?, ?)";
            stmt = conn.prepareStatement(insertQuery);
            stmt.setString(1, userId);
            stmt.setString(2, bookTitle);
            
            stmt.executeUpdate();
            
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.addProperty("message", "Loan created successfully");
            
            return request.createResponseBuilder(HttpStatus.CREATED)
                    .header("Content-Type", "application/json")
                    .body(response.toString())
                    .build();
                    
        } catch (com.google.gson.JsonSyntaxException e) {
            context.getLogger().severe("Invalid JSON: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body(createErrorJson("Invalid JSON format"))
                    .build();
        } catch (SQLException e) {
            context.getLogger().severe("Database error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body(createErrorJson("Database error: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Unexpected error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body(createErrorJson("Unexpected error: " + e.getMessage()))
                    .build();
        } finally {
            closeResources(null, stmt, conn);
        }
    }

    /**
     * Establishes a database connection using credentials from environment variables
     */
    private Connection getConnection() throws SQLException {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new SQLException("Database credentials not found in environment variables");
        }
        
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Closes database resources safely
     */
    private void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            // Log but don't throw
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    /**
     * Creates a JSON error response
     */
    private String createErrorJson(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("status", "error");
        error.addProperty("message", message);
        return error.toString();
    }
}
