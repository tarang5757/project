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
	
	public addMovie(Neo4j database) {
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
	 * 200: Movie was successfully added
	 * 400: Movie ID already exists in database, or request body is improperly formatted or missing information
	 * 500: Server Error
	 */
	public void handlePut(HttpExchange r) throws IOException, JSONException {
		String body = Utils.convert(r.getRequestBody());
		JSONObject deserialized = new JSONObject(body);
		int statusCode = 0;
		
		if (deserialized.has("movieId") && deserialized.has("name")) {
            String name = deserialized.getString("name");
            String movieId = deserialized.getString("movieId");

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
            statusCode = 400;
            sendResponse(r, statusCode, "Bad Request: Missing movieId or name");
        }
	}
}