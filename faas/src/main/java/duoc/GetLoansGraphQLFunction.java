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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Azure Function for GraphQL queries on loans.
 * Handles POST requests at the "graphql/loans/query" endpoint.
 * Single Responsibility: Execute GraphQL queries on the LOANS table.
 */
public class GetLoansGraphQLFunction {

    private static final Gson gson = new Gson();
    private GraphQL graphQL;

    public GetLoansGraphQLFunction() {
        this.graphQL = buildGraphQL();
    }

    /**
     * Builds the GraphQL schema with loan queries
     */
    private GraphQL buildGraphQL() {
        // Define the Loan type
        GraphQLObjectType loanType = GraphQLObjectType.newObject()
                .name("Loan")
                .field(f -> f
                        .name("userId")
                        .type(Scalars.GraphQLString))
                .field(f -> f
                        .name("bookTitle")
                        .type(Scalars.GraphQLString))
                .build();

        // Define the Query type with loans field
        GraphQLObjectType queryType = GraphQLObjectType.newObject()
                .name("Query")
                .field(f -> f
                        .name("loans")
                        .type(graphql.schema.GraphQLList.list(loanType))
                        .dataFetcher(environment -> {
                            try {
                                return fetchLoans();
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }))
                .build();

        // Create the schema
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();

        return GraphQL.newGraphQL(schema).build();
    }

    /**
     * Fetches all loans from the database
     */
    private List<JsonObject> fetchLoans() throws SQLException {
        List<JsonObject> loans = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            stmt = conn.createStatement();
            String query = "SELECT USER_ID, BOOK_TITLE FROM LOANS";
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                JsonObject loan = new JsonObject();
                loan.addProperty("userId", rs.getString("USER_ID"));
                loan.addProperty("bookTitle", rs.getString("BOOK_TITLE"));
                loans.add(loan);
            }
        } finally {
            DatabaseUtil.closeResources(rs, stmt, conn);
        }

        return loans;
    }

    @FunctionName("GetLoansGraphQLFunction")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "graphql/loans/query")
                    HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("GetLoansGraphQLFunction - POST /graphql/loans/query");

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
            context.getLogger().info("Executing GraphQL query");

            // Execute the GraphQL query
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
