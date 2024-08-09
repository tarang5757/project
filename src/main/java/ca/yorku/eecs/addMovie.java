package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.time.Instant;
import static org.neo4j.driver.v1.Values.parameters;

public class addMovie implements HttpHandler{
	private final Driver driver;
	
	/**
     * Constructor to initialize the addMovie handler with a Neo4j database.
     *
     * @param database An instance of the Neo4j class that provides the database driver.
     */
	public addMovie(Neo4j database) {
        this.driver = database.getDriver();
    }
	
	/**
     * Sends an HTTP response to the client.
     *
     * @param exchange   The HttpExchange object that contains the request and response.
     * @param statusCode The HTTP status code to be sent.
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
	
	/**
     * Handles the logic for a PUT request, adding a movie to the Neo4j database.
     * The request must contain "name" and "movieId" fields.
     *
     * @param exchange The HttpExchange object that contains the request and response.
     * @throws IOException If an I/O error occurs.
     * @throws JSONException If a JSON error occurs.
     */
	public void handlePut(HttpExchange r) throws IOException, JSONException {
		// Take parameters from request body
		JSONObject deserialized = Utils.getParameters(r);
		
		/* Check that necessary parameters were given
		 * Does NOT accept URL query parameters unlike GET requests
		 */
		if (deserialized.has("movieId") && deserialized.has("name")) {
            String name = deserialized.getString("name");
            String movieId = deserialized.getString("movieId");

            // Start session and run query to check if movieId already exists. If not, Create movie with given parameters
            try (Session session = driver.session()) {
                session.writeTransaction(tx -> {
                    boolean movieExists = tx.run("MATCH (m:Movie {movieId:$x}) RETURN m", parameters("x", movieId)).hasNext();
                    if (movieExists) {
                        sendResponse(r, 400, "Movie ID already exists");
                    } else {
                        tx.run("CREATE (m:Movie {movieId:$x, name:$y})", parameters("x", movieId, "y", name));
                        sendResponse(r, 200, "Movie was successfully added");
                    }
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(r, 500, "Internal Server Error");
            }
        } else {
            sendResponse(r, 400, "Bad Request: Missing movieId or name");
        }
	}
}