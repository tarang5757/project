package ca.yorku.eecs;

import java.util.List;
import java.util.Map;

import static org.neo4j.driver.v1.Values.parameters;

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

public class getCoActors implements HttpHandler{
	private final Driver driver;

	/**
     * Constructor to initialize the getCoActors handler with a Neo4j database.
     *
     * @param database An instance of the Neo4j class that provides the database driver.
     */
	public getCoActors(Neo4j database) {
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
			}else{
				sendResponse(r, 405, "Method not allowed");
			}
		} catch (Exception e) {
			sendResponse(r, 500, "INTERNAL SERVER ERROR");
			e.printStackTrace();
		}
	}

	/**
     * Handles the logic for a GET request, retrieving co-actors of a given actor in the Neo4j database.
     * The request must contain an "actorId" field.
     *
     * @param exchange The HttpExchange object that contains the request and response.
     * @throws IOException If an I/O error occurs.
     */
	private void handleGet(HttpExchange r) {
		try {
			String actorId = null;
			String response = null;
			//Can accept query parameters sent in URL or request body. Request body will take precedence.
			JSONObject deserialized = Utils.getParameters(r);
			if (deserialized.has("actorId"))
				actorId = deserialized.getString("actorId");
			else {
				sendResponse(r, 400, "Request body improperly formatted or missing information"); 
				return;
			}

			//Make query and populate list of coActors
			Session session = this.driver.session();
			Transaction tx = session.beginTransaction();
			StatementResult result = tx.run("MATCH (a:Actor {actorId:$x}) RETURN a", parameters("x", actorId));
			//Check that there exists an actor with given Id
			if(result.hasNext()) {
				Record record = result.next();
				Node actorNode = record.get("a").asNode();

				List<String> actors = new ArrayList<>();
				// Run query for any actor who has an ACTED_IN relationship with given actor's movies
				StatementResult coStarsResult = tx.run("MATCH(a:Actor {actorId:$x})-[:ACTED_IN]->(m:Movie)<-[:ACTED_IN]-(coActor:Actor) "
						+ "RETURN coActor.actorId", parameters("x", actorId));
				while(coStarsResult.hasNext()) {
					Record actorRecord = coStarsResult.next();
					String coActorId = actorRecord.get("coActor.actorId").asString();
					actors.add(coActorId);
				}

				//Build Response Body
				JSONObject jsonResponse = new JSONObject();
				jsonResponse.put("actors:", new JSONArray(actors));
				response = jsonResponse.toString();
				sendResponse(r, 200, response);

				tx.success();
			} else { //No Actor found with given actorId
				sendResponse(r, 404, "No Actor found with given ID");
			}
			tx.close();	
		} catch (JSONException e) {
			sendResponse(r, 400, "BAD REQUEST");
			e.printStackTrace();
		} catch (Exception e) {
			sendResponse(r, 500, "INTERNAL SERVER ERROR");
		}


	}

}
