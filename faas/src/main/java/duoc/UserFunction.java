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
import com.google.gson.JsonArray;

import java.sql.*;
import java.util.Optional;

/**
 * Azure Function for managing users in the library system.
 * Handles GET (list users) and POST (create user) requests.
 */
public class UserFunction {

    private static final Gson gson = new Gson();

    /**
     * Handles HTTP requests for user management.
     * GET: List all users
     * POST: Create a new user
     */
    @FunctionName("UserFunction")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "users")
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("UserFunction processed a request.");
        
        try {
            HttpMethod method = request.getHttpMethod();
            
            if (method == HttpMethod.GET) {
                return handleGetUsers(request, context);
            } else if (method == HttpMethod.POST) {
                return handleCreateUser(request, context);
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
     * Handles GET requests - returns list of all users
     */
    private HttpResponseMessage handleGetUsers(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
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
            
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.add("data", users);
            
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
     * Handles POST requests - creates a new user
     * Expected JSON body: {"name": "...", "documentId": "...", "email": "..."}
     */
    private HttpResponseMessage handleCreateUser(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
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
            JsonObject userJson = gson.fromJson(body, JsonObject.class);
            
            // Validate required fields
            if (!userJson.has("name") || !userJson.has("documentId") || !userJson.has("email")) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .header("Content-Type", "application/json")
                        .body(createErrorJson("Missing required fields: name, documentId, email"))
                        .build();
            }
            
            String name = userJson.get("name").getAsString();
            String documentId = userJson.get("documentId").getAsString();
            String email = userJson.get("email").getAsString();
            
            conn = getConnection();
            String insertQuery = "INSERT INTO USERS (NAME, DOCUMENT_ID, EMAIL) VALUES (?, ?, ?)";
            stmt = conn.prepareStatement(insertQuery);
            stmt.setString(1, name);
            stmt.setString(2, documentId);
            stmt.setString(3, email);
            
            stmt.executeUpdate();
            
            JsonObject response = new JsonObject();
            response.addProperty("status", "success");
            response.addProperty("message", "User created successfully");
            
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
