package ca.yorku.eecs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class App {
    static int PORT = 8080;
    private static Neo4j database;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        String username = "neo4j";
        String password = "12345678";

        database = new Neo4j(username, password);

        // register addActor handler
        server.createContext("/api/v1/addActor", new addActor(database));
        server.createContext("/api/v1/addMovie", new addMovie(database));
        server.createContext("/api/v1/getMovie", new getMovie(database));
        server.createContext("/api/v1/addRelationship", new addRelationship(database));
        server.createContext("/api/v1/hasRelationship", new hasRelationship(database));
        server.createContext("/api/v1/addRating", new addRating(database));
        server.createContext("/api/v1/getCoActors", new getCoActors(database));
        server.createContext("/api/v1/getActor", new getActor(database));
        server.createContext("/api/v1/computeBaconPath", new computeBaconPath(database));
        server.createContext("/api/v1/computeBaconNumber", new computeBaconNumber(database));
        server.createContext("/api/v1/getRating", new getRating(database));
        server.createContext("/api/v1/getMoviesWithRating", new getMoviesWithRating(database));
        
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
    
    public static Neo4j getDatabase() {
    	return database;
    }
}