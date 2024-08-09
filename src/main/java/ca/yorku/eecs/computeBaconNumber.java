package ca.yorku.eecs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import org.neo4j.driver.v1.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import static org.neo4j.driver.v1.Values.parameters;

public class computeBaconNumber implements HttpHandler {
    private final Driver driver;
    private final String baconId = "nm0000102";

    /**
     * Constructor to initialize the computeBaconNumber handler with a Neo4j database.
     *
     * @param database An instance of the Neo4j class that provides the database driver.
     */
    public computeBaconNumber(Neo4j database) {
        this.driver = database.getDriver();
    }

    /**
     * Sends an HTTP response to the client.
     *
     * @param exchange   The HttpExchange object that contains the request and response.
     * @param statusCode The HTTP status code to be sent.
     * @param response   The response body to be sent as a string.
     */
    private void sendResponse(HttpExchange r, int statusCode, String response) {
        try {
            byte[] bytes = response.getBytes();
            r.getResponseHeaders().set("Content-Type", "application/json");
            r.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = r.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles incoming HTTP requests. Only GET requests are allowed; other methods
     * will result in a 405 Method Not Allowed response.
     *
     * @param exchange The HttpExchange object that contains the request and response.
     */
    @Override
    public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equalsIgnoreCase("GET")) {
                handleGet(r);
            } else {
                sendResponse(r, 405, "Method not allowed");
            }
        } catch (Exception e) {
            sendResponse(r, 500, "INTERNAL SERVER ERROR");
            e.printStackTrace();
        }
    }

    /**
     * Handles the logic for a GET request, computing the baconNumber from a given actor in the database.
     * The request must contain an "actorId" field.
     *
     * @param exchange The HttpExchange object that contains the request and response.
     */
    private void handleGet(HttpExchange r) {
        try {
            JSONObject jsonResponse = new JSONObject();
            JSONObject queryParams = Utils.getParameters(r);
            String actorId;

            if (queryParams.has("actorId")) {
                actorId = queryParams.getString("actorId");
            } else {
                sendResponse(r, 400, "Request body improperly formatted or missing information");
                return;
            }

            try (Session session = this.driver.session()) {
                try (Transaction tx = session.beginTransaction()) {
                    // Check if the actor and Kevin Bacon exist in the database
                    StatementResult result = tx.run("MATCH (m:Actor {actorId:$x}) RETURN m", parameters("x", actorId));
                    StatementResult checkBacon = tx.run("MATCH (m:Actor {actorId:$x}) RETURN m", parameters("x", baconId));
                    
                    if (result.hasNext() && checkBacon.hasNext()) {
                        // if actor is Kevin Bacon then bacon number is 0
                        if (actorId.equals(baconId)) {
                            jsonResponse.put("baconNumber", 0);
                            sendResponse(r, 200, jsonResponse.toString());
                            return;
                        }

                        // Get the bacon number
                        StatementResult baconResult = tx.run(
                                "MATCH p=shortestPath((a:Actor {actorId: $actorId})-[*]-(b:Actor {actorId: $baconId})) " +
                                        "RETURN length(p)/2 as baconNumber",
                                parameters("actorId", actorId, "baconId", "nm0000102")
                        );

                        // result
                        if (baconResult.hasNext()) {
                            Record record = baconResult.next();
                            int baconNumber = record.get("baconNumber").asInt();
                            jsonResponse.put("baconNumber", baconNumber);
                            sendResponse(r, 200, jsonResponse.toString());
                        } else {
                            sendResponse(r, 404, "No path found between the actor and Kevin Bacon");
                        }
                    } else {
                        sendResponse(r, 404, "Actor or Kevin Bacon not found in the database");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(r, 500, "Internal server error");
        }
    }
}
