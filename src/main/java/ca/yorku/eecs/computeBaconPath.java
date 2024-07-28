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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.neo4j.driver.v1.Values.parameters;

public class computeBaconPath implements HttpHandler {
    private final Driver driver;

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
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            }else{
                sendResponse(r, 400, "Method not allowed");
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
            String body = Utils.convert(r.getRequestBody());
            JSONObject deserialized = new JSONObject(body);
            JSONObject jsonResponse = new JSONObject();
            String actorId;

            if (deserialized.has("actorId")) {
                actorId = deserialized.getString("actorId");
            } else {
                sendResponse(r, 400, "Request body improperly formatted or missing information");
                return;
            }

            try (Session session = this.driver.session()) {
                try (Transaction tx = session.beginTransaction()) {
                    // Check if the actor and Kevin Bacon exist in the database
                    StatementResult result = tx.run("MATCH (m:Actor {actorId:$x}) RETURN m", parameters("x", actorId));
                    StatementResult checkBacon = tx.run("MATCH (m:Actor {actorId:'nm0000102'}) RETURN m");

                    if (result.hasNext() && checkBacon.hasNext()) {
                        // Get the shortest path using the getBaconPath method
                        Map<String, Object> baconPathResult = getPath(tx, actorId);
                        if (!baconPathResult.isEmpty()) {
                            Path path = (Path) baconPathResult.get("baconPath");
                            List<String> baconPath = new ArrayList<>();
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
        StatementResult result = tx.run(
                "MATCH p=shortestPath((a:Actor {actorId: $actorId})-[*]-(b:Actor {actorId: $baconId})) " +
                        "RETURN length(p)/2 as baconNumber, p as baconPath",
                parameters("actorId", actorId, "baconId", "nm0000102")
        );
        List<Record> records = result.list();
        Map<String, Object> recordMap = new HashMap<>();
        if (!records.isEmpty()) {
            Record record = records.get(0);
            recordMap = record.asMap();
        }
        return recordMap;
    }
}