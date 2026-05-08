package duoc;

import com.google.gson.JsonObject;
import java.sql.*;

/**
 * Utility class for database operations and common functions.
 * Provides shared functionality for all Azure Functions.
 */
public class DatabaseUtil {

    /**
     * Establishes a database connection using credentials from environment variables
     */
    public static Connection getConnection() throws SQLException {
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
    public static void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    /**
     * Creates a JSON error response with format: {"error": "mensaje"}
     * 
     * @param message the error message to include in the response
     * @return JSON string with error information
     */
    public static String createErrorJson(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        return error.toString();
    }

    /**
     * Creates a JSON success response with format: {"success": "mensaje"}
     * 
     * @param message the success message to include in the response
     * @return JSON string with success information
     */
    public static String createSuccessJson(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("success", message);
        return response.toString();
    }

    /**
     * Creates a JSON response with data array
     */
    public static String createDataResponse(com.google.gson.JsonArray data) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.add("data", data);
        return response.toString();
    }
}
