package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import static org.neo4j.driver.v1.Values.parameters;

public class hasRelationship implements HttpHandler {

    private final Driver driver;

    public hasRelationship(Neo4j database) {
        this.driver = database.getDriver();
    }
    private void sendResponse(HttpExchange exchange, int statusCode, String response) {
        try {
            byte[] bytes = response.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (exchange.getRequestMethod().equals("GET")) {
                handleGet(exchange);
            } else {
            	sendResponse(exchange, 400, "BAD REQUEST");
            }
        } catch (Exception e) {
        	sendResponse(exchange, 500, "INTERNAL SERVER ERROR");
            e.printStackTrace();
        }
    }
    //For GET requests with queries in the URL
    public void handleGet(HttpExchange exchange) throws IOException {
        String response = null;
        String body = Utils.convert(exchange.getRequestBody());
        int statusCode = 0;
        String movieId = "", actorId = "";
        if(body.isEmpty()) {
			URI uri = exchange.getRequestURI();
			String query = uri.getQuery();
			Map<String, String> queryParams = Utils.parseQuery(query);
			movieId = queryParams.get("movieId");
			actorId = queryParams.get("actorId");
			if(actorId == null || movieId == null || actorId.equals("") || movieId.equals("")) {
				sendResponse(exchange, 400, "BAD REQUEST"); //400 Improper formatting
			}
			
        }
        else { //For GET requests with queries in the body
        	try {
				JSONObject deserialized = new JSONObject(body);
				if (deserialized.has("movieId") && deserialized.has("actorId")) {
					movieId = deserialized.getString("movieId");
	                actorId = deserialized.getString("actorId");
				}
				else {
					sendResponse(exchange, 400, "BAD REQUEST"); //400 Improper formatting
				}
                
			} 
        	catch (JSONException e) {
        		statusCode = 400; //400 Improper formatting
			}
        }
        try (Session session = this.driver.session()) {
        	try(Transaction tx = session.beginTransaction()){
        		StatementResult resultMovie = tx.run("MATCH (m:Movie {movieId:$x}) RETURN m", parameters("x", movieId));
        		StatementResult resultActor = tx.run("MATCH (a:Actor {actorId:$x}) RETURN a", parameters("x", actorId));
        		if(!resultMovie.hasNext() || !resultActor.hasNext()) {
        			sendResponse(exchange, 404, "NOT FOUND"); //404 actor or movie doesnt exist in the DB
        		}
        	}
        }
        try (Session session = this.driver.session()) {
            StatementResult result = session.run(
                "MATCH (a:Actor {actorId:$actorId})-[r:ACTED_IN]->(m:Movie {movieId:$movieId}) RETURN r",
                parameters("actorId", actorId, "movieId", movieId)
            );
            boolean hasRelationship = result.hasNext();

            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("actorId", actorId);
            jsonResponse.put("movieId", movieId);
            jsonResponse.put("hasRelationship", hasRelationship);

            response = jsonResponse.toString();
            statusCode = 200; //200 success
        } catch (Exception e) {
            e.printStackTrace();
            statusCode = 500;
        }
        exchange.sendResponseHeaders(statusCode, response != null ? response.length() : -1);

        if (response != null) {
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
