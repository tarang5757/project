package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;
import org.json.*;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import static org.neo4j.driver.v1.Values.parameters;

public class addRelationship implements HttpHandler {

    private final Driver driver;

    /**
     * Constructor to initialize the addRelationship handler with a Neo4j database.
     *
     * @param database An instance of the Neo4j class that provides the database driver.
     */
    public addRelationship(Neo4j database) {
        this.driver = database.getDriver();
    }

    /**
     * Sends an HTTP response to the client.
     *
     * @param exchange   The HttpExchange object that contains the request and response.
     * @param statusCode The HTTP status code to be sent.
     * @param response   The response body to be sent as a string.
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) {
        try {
            byte[] bytes = response.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles incoming HTTP requests. Only PUT requests are allowed; other methods
     * will result in a 405 Method Not Allowed response.
     *
     * @param exchange The HttpExchange object that contains the request and response.
     */
    @Override
    public void handle(HttpExchange exchange) {
        try {
            if ("PUT".equals(exchange.getRequestMethod())) {
                handlePutRequest(exchange);
            } else {
                sendResponse(exchange, 405, "BAD REQUEST");
            }
        } catch(IOException e) {
            sendResponse(exchange, 500, "INTERNAL SERVER ERROR");
        } catch(Exception e) {
            sendResponse(exchange, 500, "INTERNAL SERVER ERROR");
        }
    }

    /**
     * Handles the logic for a PUT request that creates an "ACTED_IN" relationship between an actor and a movie.
     * The request must contain "actorId" and "movieId" fields.
     *
     * @param exchange The HttpExchange object that contains the request and response.
     * @throws IOException If an I/O error occurs during the handling of the request.
     */
    private void handlePutRequest(HttpExchange exchange) throws IOException {
        try {
            JSONObject deserialized = Utils.getParameters(exchange);

            String actorId = "";
            String movieId = "";

            if (deserialized.has("actorId") && deserialized.has("movieId")) {
                actorId = deserialized.getString("actorId");
                movieId = deserialized.getString("movieId");

                int result = addActedInRelationship(actorId, movieId);

                if (result == 0) {
                    sendResponse(exchange, 200, "OK"); //200 success
                } else if (result == 1) {
                    sendResponse(exchange, 404, "NOT FOUND"); //404 if actor or movie doesnt exist in db
                } else if (result == 2) {
                    sendResponse(exchange, 400, "BAD REQUEST"); //400 improper formatting
                }
            } else {
                sendResponse(exchange, 400, "BAD REQUEST"); //400 improper formatting
            }
        } catch (JSONException e) {
            sendResponse(exchange, 400, "BAD REQUEST"); //400 improper formatting
        } catch (Exception e) {
            sendResponse(exchange, 500, "INTERNAL SERVER ERROR"); //500 unsuccessful request
        }
    }

    /**
     * Adds an "ACTED_IN" relationship between an actor and a movie in the database.
     *
     * @param actorId The ID of the actor.
     * @param movieId The ID of the movie.
     * @return An integer result code indicating the outcome:
     *         0 - Relationship was successfully created.
     *         1 - Actor or movie not found in the database.
     *         2 - Relationship already exists between the actor and the movie.
     *        -1 - An error occurred during the transaction.
     */
    private int addActedInRelationship(String actorId, String movieId) {
        try (Session session = driver.session()) {
            return session.writeTransaction(tx -> {
                StatementResult actorResult = tx.run("MATCH (a:Actor {actorId:$actorId}) RETURN a", parameters("actorId", actorId));
                StatementResult movieResult = tx.run("MATCH (m:Movie {movieId:$movieId}) RETURN m", parameters("movieId", movieId));

                if (!actorResult.hasNext() || !movieResult.hasNext()) {
                    return 1; 
                }
                StatementResult relationshipResult = tx.run(
                        "MATCH (a:Actor {actorId:$actorId})-[r:ACTED_IN]->(m:Movie {movieId:$movieId}) RETURN r",
                        parameters("actorId", actorId, "movieId", movieId)
                );
                if (relationshipResult.hasNext()) {
                    return 2;
                }
                tx.run("MATCH (a:Actor {actorId:$actorId}), (m:Movie {movieId:$movieId}) " +
                                "MERGE (a)-[:ACTED_IN]->(m)",
                        parameters("actorId", actorId, "movieId", movieId));
                return 0;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
