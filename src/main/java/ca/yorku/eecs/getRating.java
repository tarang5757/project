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

	public getRating(Neo4j database) {
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
				sendResponse(r, 405, "Method not allowed");
			}
		} catch (Exception e) {
			sendResponse(r, 500, "INTERNAL SERVER ERROR");
			e.printStackTrace();
		}
	}
	
	/*
	 * Status Codes:
	 * 200: Rating was successfully retrieved
	 * 400: Request body is improperly formatted or missing information
	 * 404: No movie with given movieId exists in database, or movie has no rating
	 * 500: Server Error
	 */
	private void handleGet(HttpExchange r) {
		try {
			String movieId = null;
			String response = null;
			String body = Utils.convert(r.getRequestBody());
			//Can accept query parameters sent in URL or request body. Request body will take precedence.
			if(!body.isEmpty()) {
				//Request Body
				JSONObject deserialized = new JSONObject(body);
				if (deserialized.has("movieId"))
					movieId = deserialized.getString("movieId");
				else {
					sendResponse(r, 400, "Request body improperly formatted or missing information"); 
					return;
				}
			}
			else {
				//If no body is given, look for query parameters 
				URI uri = r.getRequestURI();
				String query = uri.getQuery();
				Map<String, String> queryParams = Utils.parseQuery(query);
				movieId = queryParams.get("movieId");

				if (movieId == null || movieId.isEmpty()) {
					sendResponse(r, 400, "Request body improperly formatted or missing information");
					return;
				}
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
