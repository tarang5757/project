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

    // initialize the database
    public getActor(Neo4j database) {
        this.driver = database.getDriver();
    }

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

    /*
     * Status Codes:
     * 200: Actor was successfully retrieved
     * 400: Request body is improperly formatted or missing information
     * 404: No actor with given actorId exists in database
     * 500: Server Error
     */
    private void handleGet(HttpExchange exchange) throws IOException, JSONException {
        String response = null;
        String body = Utils.convert(exchange.getRequestBody());
        JSONObject deserialized = new JSONObject(body);
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
                StatementResult result = tx.run("MATCH (a:Actor {id:$x}) RETURN a", parameters("x", actorId));
                if (result.hasNext()) {
                    Record record = result.next();
                    Node actorNode = record.get("a").asNode();
                    String name = actorNode.get("name").asString();

                    List<String> movies = new ArrayList<>();
                    StatementResult moviesResult = tx.run(
                            "MATCH (a:Actor {id:$x})-[:ACTED_IN]->(m:Movie) RETURN m.id",
                            parameters("x", actorId));

                    while (moviesResult.hasNext()) {
                        Record movieRecord = moviesResult.next();
                        String movieId = movieRecord.get("m.id").asString();
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