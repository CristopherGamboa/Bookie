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
import graphql.GraphQL;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.Scalars;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;

import java.sql.*;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Azure Function for GraphQL mutations on loans.
 * Handles POST requests at the "graphql/loans/mutation" endpoint.
 * Single Responsibility: Execute GraphQL mutations to create loans in the LOANS table.
 */
public class CreateLoanGraphQLFunction {

    private static final Gson gson = new Gson();
    private static final Logger logger = Logger.getLogger(CreateLoanGraphQLFunction.class.getName());
    private GraphQL graphQL;

    public CreateLoanGraphQLFunction() {
        this.graphQL = buildGraphQL();
    }

    /**
     * Builds the GraphQL schema with loan mutations
     */
    private GraphQL buildGraphQL() {
        // Define the Loan type for mutation response
        GraphQLObjectType loanType = GraphQLObjectType.newObject()
                .name("Loan")
                .field(f -> f
                        .name("userId")
                        .type(Scalars.GraphQLString))
                .field(f -> f
                        .name("bookTitle")
                        .type(Scalars.GraphQLString))
                .build();

        // Define the CreateLoanPayload type
        GraphQLObjectType createLoanPayload = GraphQLObjectType.newObject()
                .name("CreateLoanPayload")
                .field(f -> f
                        .name("loan")
                        .type(loanType))
                .field(f -> f
                        .name("success")
                        .type(Scalars.GraphQLString))
                .build();

        // Define the Mutation type with createLoan mutation
        GraphQLObjectType mutationType = GraphQLObjectType.newObject()
                .name("Mutation")
                .field(f -> f
                        .name("createLoan")
                        .type(createLoanPayload)
                        .argument(a -> a
                                .name("userId")
                                .type(Scalars.GraphQLString))
                        .argument(a -> a
                                .name("bookTitle")
                                .type(Scalars.GraphQLString))
                        .dataFetcher(environment -> {
                            String userId = environment.getArgument("userId");
                            String bookTitle = environment.getArgument("bookTitle");
                            try {
                                return createLoan(userId, bookTitle);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }))
                .build();

        
        // GraphQL exige obligatoriamente un tipo Query base para compilar el esquema
        GraphQLObjectType dummyQuery = GraphQLObjectType.newObject()
                .name("Query")
                .field(f -> f
                        .name("_dummy")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(env -> "dummy"))
                .build();

        // Create the schema (Ahora agregamos el query de mentira)
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(dummyQuery) // <-- ESTA ES LA LÍNEA NUEVA
                .mutation(mutationType)
                .build();

        return GraphQL.newGraphQL(schema).build();
    }

    /**
     * Creates a new loan in the database and publishes an event to Azure Event Grid
     */
    private java.util.Map<String, Object> createLoan(String userId, String bookTitle) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseUtil.getConnection();
            String insertQuery = "INSERT INTO LOANS (USER_ID, BOOK_TITLE) VALUES (?, ?)";
            stmt = conn.prepareStatement(insertQuery);
            stmt.setString(1, userId);
            stmt.setString(2, bookTitle);

            stmt.executeUpdate();

            // Enviar evento a Azure Event Grid después de insertar exitosamente
            try {
                publishLoanCreatedEvent(userId, bookTitle);
            } catch (Exception e) {
                // No interrumpimos la creación del préstamo si Event Grid falla
                logger.warning("Failed to publish loan created event to Event Grid: " + e.getMessage());
            }

            // Usamos Map estándar de Java en lugar de JsonObject
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            java.util.Map<String, Object> loan = new java.util.HashMap<>();
            loan.put("userId", userId);
            loan.put("bookTitle", bookTitle);
            
            payload.put("loan", loan);
            payload.put("success", "Loan created successfully");

            return payload;
        } finally {
            DatabaseUtil.closeResources(null, stmt, conn);
        }
    }

    /**
     * Publishes a LoanCreated event to Azure Event Grid
     */
    private void publishLoanCreatedEvent(String userId, String bookTitle) {
        String endpoint = System.getenv("EVENT_GRID_ENDPOINT");
        String key = System.getenv("EVENT_GRID_KEY");
        
        if (endpoint == null || key == null) {
            throw new IllegalArgumentException(
                "EVENT_GRID_ENDPOINT and EVENT_GRID_KEY environment variables must be set");
        }
        
        // Crear cliente de Event Grid
        EventGridPublisherClient<EventGridEvent> client = new EventGridPublisherClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(key))
                .buildEventGridEventPublisherClient();
        
        // Construir datos del evento
        java.util.Map<String, Object> eventData = new java.util.HashMap<>();
        eventData.put("userId", userId);
        eventData.put("bookTitle", bookTitle);
        
        // Construir y enviar el evento
        EventGridEvent event = new EventGridEvent(
                "Prestamos/Nuevos",                     // subject
                "Biblioteca.Prestamo.Creado",           // eventType
                BinaryData.fromObject(eventData),       // data (debe ser BinaryData)
                "1.0"                                   // dataVersion
        );
        
        client.sendEvent(event);
    }

    @FunctionName("CreateLoanGraphQLFunction")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "graphql/loans/mutation")
                    HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("CreateLoanGraphQLFunction - POST /graphql/loans/mutation");

        try {
            if (!request.getBody().isPresent()) {
                return createErrorResponse(request, "Request body is required");
            }

            String body = request.getBody().get();
            JsonObject requestJson = gson.fromJson(body, JsonObject.class);

            if (!requestJson.has("query")) {
                return createErrorResponse(request, "Missing 'query' field in request");
            }

            String queryString = requestJson.get("query").getAsString();
            context.getLogger().info("Executing GraphQL mutation");

            // Execute the GraphQL mutation
            var executionResult = graphQL.execute(queryString);

            // Dejamos que GraphQL arme su propia respuesta estándar (data/errors)
            String jsonResponse = gson.toJson(executionResult.toSpecification());

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(jsonResponse)
                    .build();

        } catch (com.google.gson.JsonSyntaxException e) {
            context.getLogger().severe("Invalid JSON: " + e.getMessage());
            return createErrorResponse(request, "Invalid JSON format");
        } catch (Exception e) {
            context.getLogger().severe("Unexpected error: " + e.getMessage());
            return createErrorResponse(request, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Creates a GraphQL error response
     */
    private HttpResponseMessage createErrorResponse(
            HttpRequestMessage<Optional<String>> request,
            String errorMessage) {
        JsonObject response = new JsonObject();
        JsonArray errors = new JsonArray();
        errors.add(errorMessage);
        response.add("errors", errors);

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(response.toString())
                .build();
    }
}
