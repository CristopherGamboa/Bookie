package duoc;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Azure Function that consumes user deletion events from Azure Event Grid.
 * Ensures data integrity by cleaning up associated loan records.
 * Single Responsibility: Receive user deletion events and clean up loan records.
 */
public class UserCleanupConsumerFunction {

    private static final Gson gson = new Gson();
    private static final Logger logger = Logger.getLogger(UserCleanupConsumerFunction.class.getName());

    @FunctionName("UserCleanupConsumer")
    public void run(
            @EventGridTrigger(name = "event")
            String event,
            final ExecutionContext context) {

        logger.info("========================================");
        logger.info("UserCleanupConsumer - New Event Received");
        logger.info("========================================");

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            if (event == null || event.isEmpty()) {
                logger.warning("Event received is null or empty");
                return;
            }

            // Parse the raw JSON event
            JsonObject eventJson = gson.fromJson(event, JsonObject.class);

            // Log the complete event
            logger.info("Raw Event JSON: " + eventJson.toString());

            // Extract key fields if available
            if (eventJson.has("eventType")) {
                logger.info("Event Type: " + eventJson.get("eventType").getAsString());
            }

            if (eventJson.has("subject")) {
                logger.info("Subject: " + eventJson.get("subject").getAsString());
            }

            if (eventJson.has("eventTime")) {
                logger.info("Event Time: " + eventJson.get("eventTime").getAsString());
            }

            if (eventJson.has("data")) {
                logger.info("Event Data: " + eventJson.get("data").toString());

                // Parse the data object for user information
                JsonObject data = eventJson.getAsJsonObject("data");
                
                String eventType = eventJson.has("eventType") ? 
                        eventJson.get("eventType").getAsString() : "";
                
                if (data.has("userId")) {
                    logger.info("  - User ID: " + data.get("userId").getAsString());
                }

                // Check if this is a user deletion event
                if ("Biblioteca.Usuario.Eliminado".equals(eventType)) {
                    if (data.has("userId")) {
                        String userId = data.get("userId").getAsString();
                        logger.info("Processing user deletion event for userId: " + userId);
                        cleanupUserLoans(userId, context);
                    } else {
                        logger.warning("User deletion event is missing userId field");
                    }
                } else {
                    logger.info("Event is not a user deletion event");
                }
            }

            logger.info("========================================");
            logger.info("Event processed successfully");
            logger.info("========================================");

        } catch (com.google.gson.JsonSyntaxException e) {
            logger.severe("Failed to parse event JSON: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Error processing event: " + e.getMessage());
        }
    }

    /**
     * Deletes all loan records associated with a user from the database.
     * This ensures that when a user is deleted, their loan records are cleaned up automatically.
     */
    private void cleanupUserLoans(String userId, ExecutionContext context) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseUtil.getConnection();
            
            String deleteSql = "DELETE FROM LOANS WHERE USER_ID = ?";
            stmt = conn.prepareStatement(deleteSql);
            stmt.setString(1, userId);
            
            int rowsDeleted = stmt.executeUpdate();
            
            if (rowsDeleted > 0) {
                logger.info("Cleaned up " + rowsDeleted + " loan record(s) for user: " + userId);
            } else {
                logger.info("No loan records found to delete for user: " + userId);
            }

        } catch (SQLException e) {
            logger.severe("Database error while cleaning up user loans: " + e.getMessage());
            if (context != null) {
                context.getLogger().severe("SQL Error: " + e.getSQLState() + " - " + e.getMessage());
            }
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                logger.severe("Error closing database resources: " + e.getMessage());
            }
        }
    }
}
