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

import java.sql.*;
import java.util.Optional;

/**
 * Azure Function for GraphQL mutations on loans.
 * Handles POST requests at the "graphql/loans/mutation" endpoint.
 * Single Responsibility: Execute GraphQL mutations to create loans in the LOANS table.
 */
public class CreateLoanGraphQLFunction {

    private static final Gson gson = new Gson();
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

        // Create the schema
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .mutation(mutationType)
                .build();

        return GraphQL.newGraphQL(schema).build();
    }

    /**
     * Creates a new loan in the database
     */
    private JsonObject createLoan(String userId, String bookTitle) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseUtil.getConnection();
            String insertQuery = "INSERT INTO LOANS (USER_ID, BOOK_TITLE) VALUES (?, ?)";
            stmt = conn.prepareStatement(insertQuery);
            stmt.setString(1, userId);
            stmt.setString(2, bookTitle);

            stmt.executeUpdate();

            // Build the response
            JsonObject payload = new JsonObject();
            JsonObject loan = new JsonObject();
            loan.addProperty("userId", userId);
            loan.addProperty("bookTitle", bookTitle);
            payload.add("loan", loan);
            payload.addProperty("success", "Loan created successfully");

            return payload;
        } finally {
            DatabaseUtil.closeResources(null, stmt, conn);
        }
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

            // Build the GraphQL standard response
            JsonObject response = new JsonObject();

            if (executionResult.getErrors() != null && !executionResult.getErrors().isEmpty()) {
                JsonArray errorsArray = new JsonArray();
                executionResult.getErrors().forEach(error ->
                        errorsArray.add(error.getMessage())
                );
                response.add("errors", errorsArray);
            } else {
                JsonObject data = gson.fromJson(gson.toJson(executionResult.getData()), JsonObject.class);
                response.add("data", data);
            }

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(response.toString())
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
