package ca.yorku.eecs;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Long;
import java.net.URI;

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


public class getMovie implements HttpHandler{
	private final Driver driver;

	public getMovie(Neo4j database) {
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
	 * 200: Movie was successfully retrieved
	 * 400: Request body is improperly formatted or missing information
	 * 404: No movie with given movieId exists in database
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
				//Query Parameters 
				URI uri = r.getRequestURI();
				String query = uri.getQuery();
				Map<String, String> queryParams = Utils.parseQuery(query);
				movieId = queryParams.get("movieId");

				if (movieId == null || movieId.isEmpty()) {
					sendResponse(r, 400, "Request body improperly formatted or missing information");
					return;
				}
			}

			Session session = this.driver.session();
			Transaction tx = session.beginTransaction();
			StatementResult result = tx.run("MATCH (m:Movie {movieId:$x}) RETURN m", parameters("x", movieId));
			if(result.hasNext()) {
				Record record = result.next();
				Node movieNode = record.get("m").asNode();
				String name = movieNode.get("name").asString();

				List<String> actors = new ArrayList<>();
				StatementResult actorsResult = tx.run("MATCH (m:Movie {movieId:$x})<-[:ACTED_IN]-(a:Actor) RETURN a.actorId",parameters("x", movieId));

				while(actorsResult.hasNext()) {
					Record actorRecord = actorsResult.next();
					String actorId = actorRecord.get("a.actorId").asString();
					actors.add(actorId);
				}

				//Manually build response body to ensure correct order
				StringBuilder jsonResponse = new StringBuilder();
				jsonResponse.append("{");
				jsonResponse.append("\"movieId\":").append("\"").append(movieId).append("\",");
				jsonResponse.append("\"name\":").append("\"").append(name).append("\",");
				jsonResponse.append("\"actors\":").append(new JSONArray(actors).toString());
				jsonResponse.append("}");

				response = jsonResponse.toString();
				sendResponse(r, 200, response);
				tx.success();
			} 
			else { //No Movie found with given movieId
				sendResponse(r, 404, "No Movie found with given ID");
			}
			tx.close();
		} catch(JSONException e) {
			sendResponse(r, 400, "BAD REQUEST");
		} catch (Exception e) {
			sendResponse(r, 500, "INTERNAL SERVER ERROR");
		}

	}
}