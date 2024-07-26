package ca.yorku.eecs;

import java.util.List;

import static org.neo4j.driver.v1.Values.parameters;

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

public class getCoActors implements HttpHandler{
	private final Driver driver;

	public getCoActors(Neo4j database) {
		this.driver = database.getDriver();
	}
	
	private void sendResponse(HttpExchange exchange, int statusCode, String response) {
        try {
            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
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
	 * 200: Successful retrieval of Actors
	 * 400: Request body improperly formatted or missing required information
	 * 404: If no actor exists in the database with the given actorId
	 * 500: Internal Server Error
	 */
	private void handleGet(HttpExchange r) {
		try {
			String response = null;
			String body = Utils.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);
			String actorId = "";
			
			if(deserialized.has("actorId")) {
				actorId = deserialized.getString("actorId");
			} else {
				sendResponse(r, 400, "Request body improperly formatted or missing information");
				return;
			}
			
			//Make query and populate list of coActors
			Session session = this.driver.session();
			Transaction tx = session.beginTransaction();
			StatementResult result = tx.run("MATCH (a:Actor {actorId:$x}) RETURN a", parameters("x", actorId));
			if(result.hasNext()) {
				Record record = result.next();
				Node actorNode = record.get("a").asNode();
				
				List<String> actors = new ArrayList<>();
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
				tx.close();
			} else { //No Actor found with given actorId
				sendResponse(r, 404, "No Actor found with given ID");
			}
				
		} catch (JSONException e) {
			sendResponse(r, 400, "BAD REQUEST");
			e.printStackTrace();
		} catch (Exception e) {
			sendResponse(r, 500, "INTERNAL SERVER ERROR");
		}
		
		
	}
	
}
