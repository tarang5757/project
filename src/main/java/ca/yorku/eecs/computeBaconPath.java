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

    public computeBaconPath(Neo4j database) {
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

    private void handleGet(HttpExchange r) {
        try {
            URI uri = r.getRequestURI();
            String query = uri.getQuery();

            if (query == null || query.isEmpty()) {
                sendResponse(r, 400, "Request body improperly formatted or missing information");
                return;
            }

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
