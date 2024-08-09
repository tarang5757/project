package ca.yorku.eecs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.neo4j.driver.v1.Values.parameters;

public class computeBaconPath implements HttpHandler {
    private final Driver driver;
    private final String baconId = "nm0000102";

    /**
     * Constructor to initialize the computeBaconPath handler with a Neo4j database.
     *
     * @param database An instance of the Neo4j class that provides the database driver.
     */
    public computeBaconPath(Neo4j database) {
        this.driver = database.getDriver();
    }

    /**
     * Sends an HTTP response to the client.
     *
     * @param exchange   The HttpExchange object that contains the request and response.
     * @param statusCode The HTTP status code to be sent (e.g., 200 for OK, 400 for Bad Request).
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
     * Handles the logic for a GET request, computing the bacon path from a given actor in the database.
     * The request must contain an "actorId" field.
     *
     * @param exchange The HttpExchange object that contains the request and response.
     */
    private void handleGet(HttpExchange r) {
        try {
        	JSONObject queryParams = Utils.getParameters(r);
            JSONObject jsonResponse = new JSONObject();
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
                        ArrayList<String> baconPath = new ArrayList<>();
                        if (actorId.equals(baconId)) {
                            baconPath.add(baconId);
                            jsonResponse.put("baconPath", baconPath);
                            sendResponse(r, 200, jsonResponse.toString());
                            return;
                        }
                        // Get the shortest path using the getPath method
                        Map<String, Object> baconPathResult = getPath(tx, actorId);
                        if (!baconPathResult.isEmpty()) {
                            Path path = (Path) baconPathResult.get("baconPath");
                            for (Node node : path.nodes()) {
                                if (node.containsKey("actorId")) {
                                    baconPath.add(node.get("actorId").asString());
                                }
                            }
                            jsonResponse.put("baconPath", baconPath);
                            sendResponse(r, 200, jsonResponse.toString());
                        } else {
                            sendResponse(r, 404, "No path found between the given actor and Kevin Bacon");
                        }
                    } else {
                        sendResponse(r, 404, "Actor or Kevin Bacon not found in the database");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(r, 500, "Internal Server Error");
        }
    }

    /**
     * Retrieves the shortest path between the given actor and Kevin Bacon in the Neo4j database.
     *
     * @param tx the transaction within which the query is run
     * @param actorId the ID of the actor for whom the path to Kevin Bacon is to be found
     * @return a map containing the length of the path divided by 2 (as baconNumber) and the path itself (as baconPath)
     */

    private static Map<String, Object> getPath(Transaction tx, String actorId) {
        // Execute a query to find the shortest path between the given actor and Kevin Bacon
        StatementResult result = tx.run(
                "MATCH p=shortestPath((a:Actor {actorId: $actorId})-[*]-(b:Actor {actorId: $baconId})) " +
                        "RETURN p AS baconPath",
                parameters("actorId", actorId, "baconId", "nm0000102")
        );

        // Get the list of records returned by the query
        List<Record> records = result.list();
        // Initialize a map to hold the results
        Map<String, Object> recordMap = new HashMap<>();
        if (!records.isEmpty()) {
            Record record = records.get(0);
            recordMap = record.asMap();
        }
        return recordMap;
    }
}
