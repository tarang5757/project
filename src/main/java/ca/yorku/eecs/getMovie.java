package ca.yorku.eecs;

import java.util.List;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Long;
import org.json.*;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.StatementResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.time.Instant;
import static org.neo4j.driver.v1.Values.parameters;

public class getMovie implements HttpHandler {
    private final Driver driver;

    public getMovie(Neo4j database) {
        this.driver = database.getDriver();
    }

    @Override
    public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            } else {
                r.sendResponseHeaders(400, -1);
            }
        } catch (Exception e) {
            r.sendResponseHeaders(500, -1);
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
    public void handleGet(HttpExchange r) throws IOException, JSONException {
        String response = null;
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized = new JSONObject(body);
        int statusCode = 0;
        String movieId = "";

        if (deserialized.has("movieId")) {
            movieId = deserialized.getString("movieId");
        } else {
            statusCode = 400;
            r.sendResponseHeaders(statusCode, -1);
            return;
        }

        try (Session session = this.driver.session()) {
            try (Transaction tx = session.beginTransaction()) {
                StatementResult result = tx.run("MATCH (m:Movie {movieId:$x}) RETURN m", parameters("x", movieId));
                if (result.hasNext()) {
                    Record record = result.next();
                    Node movieNode = record.get("m").asNode();
                    String name = movieNode.get("name").asString();

                    List<String> actors = new ArrayList<>();
                    StatementResult actorsResult = tx.run(
                            "MATCH (m:Movie {movieId:$x})<-[:ACTED_IN]-(a:Actor) RETURN a.actorId",
                            parameters("x", movieId));

                    while (actorsResult.hasNext()) {
                        Record actorRecord = actorsResult.next();
                        String actorId = actorRecord.get("a.actorId").asString();
                        actors.add(actorId);
                    }

                    // Manually build response body to ensure correct order
                    StringBuilder jsonResponse = new StringBuilder();
                    jsonResponse.append("{");
                    jsonResponse.append("\"movieId\":").append("\"").append(movieId).append("\",");
                    jsonResponse.append("\"name\":").append("\"").append(name).append("\",");
                    jsonResponse.append("\"actors\":").append(new JSONArray(actors).toString());
                    jsonResponse.append("}");

                    response = jsonResponse.toString();
                    statusCode = 200;
                } else { // No Movie found with given movieId
                    statusCode = 404;
                }
                tx.success();
            } catch (Exception e) {
                e.printStackTrace();
                statusCode = 500;
                session.close();
            }

            r.getResponseHeaders().add("Content-Type", "application/json");
            r.sendResponseHeaders(statusCode, response != null ? response.length() : -1);

            if (response != null) {
                OutputStream os = r.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }

        }
    }

}
