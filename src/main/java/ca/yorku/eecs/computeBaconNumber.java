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

    public computeBaconNumber(Neo4j database) {
        this.driver = database.getDriver();
    }

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

    /*
     * Status Codes:
     * 200: Movie was successfully retrieved
     * 400: Request body is improperly formatted or missing information
     * 404: No movie with given movieId exists in database
     * 500: Server Error
     */
    private void handleGet(HttpExchange r) {
        try {
            URI uri = r.getRequestURI();
            String query = uri.getQuery();
            Map<String, String> queryParams = Utils.parseQuery(query);
            JSONObject jsonResponse = new JSONObject();
            String actorId;

            if (queryParams.containsKey("actorId")) {
                actorId = queryParams.get("actorId");
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
