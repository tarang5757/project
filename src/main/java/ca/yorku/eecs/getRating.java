package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.types.Node;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class getRating implements HttpHandler{
	private final Driver driver;

	/**
     * Constructor to initialize the getRating handler with a Neo4j database.
     *
     * @param database An instance of the Neo4j class that provides the database driver.
     */
	public getRating(Neo4j database) {
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
		} catch(IOException e) {
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
			if (r.getRequestMethod().equals("GET")) {
				handleGet(r);
			}
			else{
				sendResponse(r, 405, "Method not allowed");
			}
		} catch (Exception e) {
			sendResponse(r, 500, "INTERNAL SERVER ERROR");
			e.printStackTrace();
		}
	}

	/**
     * Handles the logic for a GET request, attempting to retrie a rating from a movie in the Neo4j database.
     * The request must contain a "movieId" field.
     *
     * @param exchange The HttpExchange object that contains the request and response.
     */
	private void handleGet(HttpExchange r) {
		try {
			String movieId = null;
			String response = null;
			//Can accept query parameters sent in URL or request body. Request body will take precedence.
			JSONObject deserialized = Utils.getParameters(r);
			if (deserialized.has("movieId"))
				movieId = deserialized.getString("movieId");
			else {
				sendResponse(r, 400, "Request body improperly formatted or missing information"); 
				return;
			}

			//Initialize Transaction
			Session session = this.driver.session();
			Transaction tx = session.beginTransaction();
			StatementResult result = tx.run("MATCH (m:Movie {movieId:$x}) RETURN m.rating", parameters("x", movieId));
			// If there exists a movie with given movieId and a rating parameter
			if(result.hasNext()) {
				Record record = result.next();
				int rating = record.get("m.rating").asInt();
				//Put rating into response body
				JSONObject jsonResponse = new JSONObject();
				jsonResponse.put("rating", rating);

				response = jsonResponse.toString();
				sendResponse(r, 200, response);
				tx.success();
			} 
			else { //No Movie found with given movieId, or movie has no rating
				sendResponse(r, 404, "No Rating found for given movieId");
			}
			tx.close();
		} catch(JSONException e) {
			sendResponse(r, 400, "BAD REQUEST");
		} catch (Exception e) {
			sendResponse(r, 500, "INTERNAL SERVER ERROR");
		}

	}
}
