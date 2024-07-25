package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import static org.neo4j.driver.v1.Values.parameters;

public class addActor implements HttpHandler {
    private final Driver driver;

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

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                handlePut(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal Server Error");
        }
    }

    /*
     * Status Codes:
     * 200: Actor was successfully added
     * 400: Actor ID already exists in database, or request body is improperly
     * formatted or missing information
     * 500: Server Error
     */
    private void handlePut(HttpExchange exchange) throws IOException, JSONException {
        String body = Utils.convert(exchange.getRequestBody());
        JSONObject deserialized = new JSONObject(body);
        int statusCode = 0;

        if (deserialized.has("actorId") && deserialized.has("name")) {
            String name = deserialized.getString("name");
            String actorId = deserialized.getString("actorId");

            try (Session session = driver.session()) {
                session.writeTransaction(tx -> {
                    boolean actorExists = tx.run("MATCH (a:Actor {id:$x}) RETURN a", parameters("x", actorId))
                            .hasNext();
                    if (actorExists) {
                        sendResponse(exchange, 400, "Actor ID already exists");
                    } else {
                        tx.run("CREATE (a:Actor {id:$x, name:$y})", parameters("x", actorId, "y", name));
                        sendResponse(exchange, 200, "Actor was successfully added");
                    }
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal Server Error");
            }
        } else {
            statusCode = 400;
            sendResponse(exchange, statusCode, "Bad Request: Missing actorID or name");
        }
    }
}
