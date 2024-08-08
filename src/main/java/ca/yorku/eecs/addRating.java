package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import static org.neo4j.driver.v1.Values.parameters;

public class addRating implements HttpHandler {

    private final Driver driver;

    public addRating(Neo4j database) {
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
    public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("PUT")) {
                handlePut(r);
            } else {
                sendResponse(r, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            sendResponse(r, 500, "Internal Server Error");
            e.printStackTrace();
        }
    }

    /*
     * Status Codes:
     * 200: Rating was successfully added
     * 400: Request body is improperly formatted or missing information
     * 500: Server Error
     */
    public void handlePut(HttpExchange r) throws IOException, JSONException {
        JSONObject deserialized = Utils.getParameters(r);
        int statusCode = 0;

        if (deserialized.has("movieId") && deserialized.has("rating")) {
            String movieId = deserialized.getString("movieId");
            double rating = deserialized.getDouble("rating");
            if(rating > 10 || rating < 0) {
            	sendResponse(r, 400, "Bad Request: Rating must be from 0 to 10");
            	return;
            }
            try (Session session = driver.session()) {
                session.writeTransaction(tx -> {
                    boolean movieExists = tx.run("MATCH (m:Movie {movieId: $x}) RETURN m", parameters("x", movieId))
                            .hasNext();
                    if (movieExists) {
                        tx.run("MATCH (m:Movie {movieId: $x}) SET m.rating = $y",
                                parameters("x", movieId, "y", rating));
                        sendResponse(r, 200, "Rating was successfully added");
                    } else {
                        sendResponse(r, 404, "Movie ID does not exist");
                    }
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(r, 500, "Internal Server Error");
            }
        } else {
            statusCode = 400;
            sendResponse(r, statusCode, "Bad Request: Missing movieId or rating");
        }
    }
}
