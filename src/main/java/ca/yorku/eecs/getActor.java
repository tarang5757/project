package ca.yorku.eecs;

import java.util.List;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;

import org.json.*;

import org.neo4j.driver.v1.Driver;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.StatementResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import static org.neo4j.driver.v1.Values.parameters;

public class getActor implements HttpHandler {
    private final Driver driver;

    /**
     * Constructor to initialize the getActor handler with a Neo4j database.
     *
     * @param database An instance of the Neo4j class that provides the database driver.
     */
    public getActor(Neo4j database) {
        this.driver = database.getDriver();
    }

    /**
     * Handles incoming HTTP requests. Only GET requests are allowed; other methods
     * will result in a 405 Method Not Allowed response.
     *
     * @param exchange The HttpExchange object that contains the request and response.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleGet(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed".getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal Server Error".getBytes());
        }
    }

    /**
     * Handles the logic for a GET request, retrieving an actor from the Neo4j database.
     * The request must contain an "actorId" field.
     *
     * @param exchange The HttpExchange object that contains the request and response.
     * @throws IOException If an I/O error occurs.
     * @throws JSONException If a JSON errr occurs.
     */
    private void handleGet(HttpExchange exchange) throws IOException, JSONException {
        String response = null;
        JSONObject deserialized = Utils.getParameters(exchange);
        int statusCode = 0;
        String actorId = "";

        if (deserialized.has("actorId")) {
            actorId = deserialized.getString("actorId");
        } else {
            statusCode = 400;
            sendResponse(exchange, statusCode, "Bad Request: Missing actorId".getBytes());
            return;
        }

        try (Session session = this.driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                StatementResult result = tx.run("MATCH (a:Actor {actorId:$x}) RETURN a", parameters("x", actorId));
                if (result.hasNext()) {
                    Record record = result.next();
                    Node actorNode = record.get("a").asNode();
                    String name = actorNode.get("name").asString();

                    List<String> movies = new ArrayList<>();
                    StatementResult moviesResult = tx.run(
                            "MATCH (a:Actor {actorId:$x})-[:ACTED_IN]->(m:Movie) RETURN m.movieId",
                            parameters("x", actorId));

                    while (moviesResult.hasNext()) {
                        Record movieRecord = moviesResult.next();
                        String movieId = movieRecord.get("m.movieId").asString();
                        movies.add(movieId);
                    }

                    // Manually build response body to ensure correct order
                    StringBuilder jsonResponse = new StringBuilder();
                    jsonResponse.append("{");
                    jsonResponse.append("\"actorId\":").append("\"").append(actorId).append("\",");
                    jsonResponse.append("\"name\":").append("\"").append(name).append("\",");
                    jsonResponse.append("\"movies\":").append(new JSONArray(movies).toString());
                    jsonResponse.append("}");

                    response = jsonResponse.toString();
                    statusCode = 200;
                } else { // No Actor found with given actorId
                    statusCode = 404;
                }
                tx.success();
            } catch (Exception e) {
                e.printStackTrace();
                statusCode = 500;
                session.close();
            }

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            sendResponse(exchange, statusCode, response != null ? response.getBytes() : null);
        }
    }

    /**
     * Sends an HTTP response to the client.
     *
     * @param exchange   The HttpExchange object that contains the request and response.
     * @param statusCode The HTTP status code to be sent (e.g., 200 for OK, 400 for Bad Request).
     * @param response   The response body to be sent as a string.
     */
    private void sendResponse(HttpExchange exchange, int statusCode, byte[] response) throws IOException {
        if (response == null) {
            exchange.sendResponseHeaders(statusCode, -1);
        } else {
            exchange.sendResponseHeaders(statusCode, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }
}