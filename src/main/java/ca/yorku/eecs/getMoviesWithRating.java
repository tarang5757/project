package ca.yorku.eecs;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;

import org.json.*;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.StatementResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import static org.neo4j.driver.v1.Values.parameters;

public class getMoviesWithRating implements HttpHandler {
    private final Driver driver;

    public getMoviesWithRating(Neo4j database) {
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
            if (r.getRequestMethod().equals("GET")) {
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
     * 200: Movies were successfully retrieved
     * 400: Request body is improperly formatted or missing information
     * 404: No movies found with the given rating
     * 500: Server Error
     */
    private void handleGet(HttpExchange r) {
        try {
            String ratingParam = null;
            String response = null;
            String body = Utils.convert(r.getRequestBody());
            // Can accept query parameters sent in URL or request body. Request body will take precedence.
            if (!body.isEmpty()) {
                // Request Body
                JSONObject deserialized = new JSONObject(body);
                if (deserialized.has("rating"))
                    ratingParam = deserialized.getString("rating");
                else {
                    sendResponse(r, 400, "Request body improperly formatted or missing information");
                    return;
                }
            } else {
                // Query Parameters
                URI uri = r.getRequestURI();
                String query = uri.getQuery();
                Map<String, String> queryParams = Utils.parseQuery(query);
                ratingParam = queryParams.get("rating");

                if (ratingParam == null || ratingParam.isEmpty()) {
                    sendResponse(r, 400, "Request body improperly formatted or missing information");
                    return;
                }
            }

            double rating;
            try {
                rating = Double.parseDouble(ratingParam);
            } catch (NumberFormatException e) {
                sendResponse(r, 400, "Invalid rating format");
                return;
            }

            Session session = this.driver.session();
            Transaction tx = session.beginTransaction();
            StatementResult result = tx.run("MATCH (m:Movie) WHERE m.rating >= $rating RETURN m", parameters("rating", rating));
            List<JSONObject> movies = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                Node movieNode = record.get("m").asNode();
                String movieId = movieNode.get("movieId").asString();
                String name = movieNode.get("name").asString();
                double movieRating = movieNode.get("rating").asDouble();

                JSONObject movie = new JSONObject();
                movie.put("movieId", movieId);
                movie.put("name", name);
                movie.put("rating", movieRating);

                movies.add(movie);
            }

            if (!movies.isEmpty()) {
                sendResponse(r, 200, new JSONArray(movies).toString());
            } else {
                sendResponse(r, 404, "No Movies found with the given rating");
            }
            tx.close();
        } catch (JSONException e) {
            sendResponse(r, 400, "BAD REQUEST");
        } catch (Exception e) {
            sendResponse(r, 500, "INTERNAL SERVER ERROR");
        }
    }
}
