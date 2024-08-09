package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;
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

    /**
     * Constructor to initialize the hasRelationship handler with a Neo4j database.
     *
     * @param database An instance of the Neo4j class that provides the database driver.
     */
    public hasRelationship(Neo4j database) {
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
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
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
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (exchange.getRequestMethod().equals("GET")) {
                handleGet(exchange);
            } else {
                sendResponse(exchange, 405, "BAD REQUEST");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "INTERNAL SERVER ERROR");
            e.printStackTrace();
        }
    }

    /**
     * Handles the logic for a GET request, determining if there is a ACTED_IN relationship between an actor and movie.
     * The request must contain "movieId" and "actorId" fields.
     *
     * @param exchange The HttpExchange object that contains the request and response.
     * @throws IOException If an I/O error occurs.
     */
    public void handleGet(HttpExchange exchange) throws IOException {
        String response = null;
        int statusCode = 200;
        String movieId = "", actorId = "";

        try {
            JSONObject deserialized = Utils.getParameters(exchange);

            if (deserialized.has("movieId") && deserialized.has("actorId")) {
                movieId = deserialized.getString("movieId");
                actorId = deserialized.getString("actorId");
                //check if given actor and movie exist in db
                try (Session session = this.driver.session()) {
                    try (Transaction tx = session.beginTransaction()) {
                        StatementResult resultMovie = tx.run("MATCH (m:Movie {movieId:$x}) RETURN m", parameters("x", movieId));
                        StatementResult resultActor = tx.run("MATCH (a:Actor {actorId:$x}) RETURN a", parameters("x", actorId));
                        
                        if (!resultMovie.hasNext() || !resultActor.hasNext()) {
                            sendResponse(exchange, 404, "NOT FOUND");	//status code 404 if an actor or movie doesnt exist in the db
                            return;
                        }
                    }
                }
                // check if relationship exists between given actor and movie
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
                } catch (Exception e) {
                    e.printStackTrace();
                    statusCode = 500; //status code 500 for server error
                }
            } else {
                statusCode = 400; //status code 400 for improper formatting
                response = "BAD REQUEST";
            }

        } catch (JSONException e) {
            statusCode = 400;
            response = "BAD REQUEST";
        }

        sendResponse(exchange, statusCode, response);
    }
}
