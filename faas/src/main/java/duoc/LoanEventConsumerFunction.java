package duoc;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.logging.Logger;

/**
 * Azure Function that consumes loan creation events from Azure Event Grid.
 * Acts as an event subscriber demonstrating event-driven architecture.
 * Single Responsibility: Receive and log loan created events.
 */
public class LoanEventConsumerFunction {

    private static final Gson gson = new Gson();
    private static final Logger logger = Logger.getLogger(LoanEventConsumerFunction.class.getName());

    @FunctionName("LoanEventConsumer")
    public void run(
            @EventGridTrigger(name = "event")
            String event,
            final ExecutionContext context) {

        logger.info("========================================");
        logger.info("LoanEventConsumer - New Event Received");
        logger.info("========================================");

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

                // Parse the data object for loan information
                JsonObject data = eventJson.getAsJsonObject("data");
                if (data.has("userId")) {
                    logger.info("  - User ID: " + data.get("userId").getAsString());
                }
                if (data.has("bookTitle")) {
                    logger.info("  - Book Title: " + data.get("bookTitle").getAsString());
                }
            }

            logger.info("========================================");
            logger.info("Event processed successfully");
            logger.info("========================================");

        } catch (com.google.gson.JsonSyntaxException e) {
            logger.severe("Failed to parse event JSON: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Error processing event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
