package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.v1.*;

public class addActor implements HttpHandler {
    private final Driver driver;

    // this constructor initializes database
    public addActor(Neo4j database) {
        this.driver = database.getDriver();
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) {
        try {
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public void handle(HttpExchange exchange) {
        try {
            if ("PUT".equals(exchange.getRequestMethod())) {
                handlePutRequest(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }

        catch (IOException e) {
            sendResponse(exchange, 500, "Internal Server Error");
        }

        catch (Exception e) {
            sendResponse(exchange, 500, "Internal Server Error");

        }

    }

    private void handlePutRequest(HttpExchange exchange) throws IOException {
        try {
            String body = Utils.convert(exchange.getRequestBody());
            JSONObject deserialized = new JSONObject(body);

            String name = "";
            String actorId = "";

            if (deserialized.has("name") && deserialized.has("actorID")) {
                name = deserialized.getString("name");
                actorId = deserialized.getString("actorID");

                addActorToDatabase(name, actorId);

                JSONObject responseJSON = new JSONObject();
                responseJSON.put("name", name);
                responseJSON.put("ActorID", actorId);

                sendResponse(exchange, 200, responseJSON.toString());
            } else {
                sendResponse(exchange, 400, "Bad Request: Missing name or actorID");
            }
        } catch (JSONException e) {
            sendResponse(exchange, 400, "Bad Request: Invalid JSON");
        } catch (Exception e) {
            sendResponse(exchange, 500, "Internal Server Error");
        }
    }

    private void addActorToDatabase(String name, String id) {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                String query = "MERGE (a:Actor {actorID: $actorID}) "
                        + "ON CREATE SET a.name = $name "
                        + "ON MATCH SET a.name = $name"; // Updates the name if the actorID already exists
                tx.run(query, parameters("actorID", id, "name", name));
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging purposes
        }
    }

}