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

    /**
     * Constructor to initialize the addActor handler with a Neo4j database.
     *
     * @param database An instance of the Neo4j class that provides the database driver.
     */
    public addActor(Neo4j database) {
        this.driver = database.getDriver();
    }

    /**
     * Sends an HTTP response to the client.
     *
     * @param exchange   The HttpExchange object that contains the request and response.
     * @param statusCode The HTTP status code to be sent (e.g., 200 for OK, 400 for Bad Request).
     * @param response   The response body to be sent as a string.
     */
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

    /**
     * Handles incoming HTTP requests. Only PUT requests are allowed; other methods
     * will result in a 405 Method Not Allowed response.
     *
     * @param exchange The HttpExchange object that contains the request and response.
     */
    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (exchange.getRequestMethod().equals("PUT")) {
                handlePutRequest(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (IOException e) {
            sendResponse(exchange, 500, "Internal Server Error");
        } catch (Exception e) {
            sendResponse(exchange, 500, "Internal Server Error");

        }
    }

    /**
     * Handles the logic for a PUT request, adding an actor to the Neo4j database.
     * The request must contain "name" and "actorId" fields.
     *
     * @param exchange The HttpExchange object that contains the request and response.
     * @throws IOException If an I/O error occurs.
     */
    private void handlePutRequest(HttpExchange exchange) throws IOException {
        try {
            JSONObject deserialized = Utils.getParameters(exchange);

            if (deserialized.has("name") && deserialized.has("actorId")) {
                String name = deserialized.getString("name");
                String actorId = deserialized.getString("actorId");

                try (Session session = driver.session()) {
                    session.writeTransaction(tx -> {
                        boolean actorExists = tx.run("MATCH (a:Actor {actorId: $actorId}) RETURN a", parameters("actorId", actorId)).hasNext();
                        if (actorExists) {
                            sendResponse(exchange, 400, "Actor ID already exists");
                        } else {
                            tx.run("CREATE (a:Actor {actorId: $actorId, name: $name})", parameters("actorId", actorId, "name", name));
                            sendResponse(exchange, 200, "Actor was successfully added");
                        }
                        return null;
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "Internal Server Error");
                }
            } else {
                sendResponse(exchange, 400, "Bad Request: Missing name or actorId");
            }
        } catch (JSONException e) {
            sendResponse(exchange, 400, "Bad Request: Invalid JSON");
        } catch (Exception e) {
            sendResponse(exchange, 500, "Internal Server Error");
        }
    }
}
