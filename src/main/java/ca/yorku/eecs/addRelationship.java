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

    public addRelationship(Neo4j database) {
        this.driver = database.getDriver();
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) {
        try {
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if ("PUT".equals(exchange.getRequestMethod())) {
                handlePutRequest(exchange);
            } else {
                sendResponse(exchange, 400, "BAD REQUEST");
            }
        } catch(IOException e) {
            sendResponse(exchange, 500, "INTERNAL SERVER ERROR");
        } catch(Exception e) {
            sendResponse(exchange, 500, "INTERNAL SERVER ERROR");
        }
    }

    private void handlePutRequest(HttpExchange exchange) throws IOException {
        try {
            String body = Utils.convert(exchange.getRequestBody());
            JSONObject deserialized = new JSONObject(body);

            String actorId = "";
            String movieId = "";

            if (deserialized.has("actorId") && deserialized.has("movieId")) {
                actorId = deserialized.getString("actorId");
                movieId = deserialized.getString("movieId");

                int result = addActedInRelationship(actorId, movieId);

                if (result == 0) {
                    sendResponse(exchange, 200, "OK");
                } else if (result == 1) {
                    sendResponse(exchange, 404, "NOT FOUND");
                } else if (result == 2) {
                    sendResponse(exchange, 400, "BAD REQUEST");
                }
            } else {
                sendResponse(exchange, 400, "BAD REQUEST");
            }
        } catch (JSONException e) {
            sendResponse(exchange, 400, "BAD REQUEST");
        } catch (Exception e) {
            sendResponse(exchange, 500, "INTERNAL SERVER ERROR");
        }
    }

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
