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

public class getMoviesWithRating implements HttpHandler {
    private final Driver driver;

    public getMoviesWithRating(Neo4j database) {
        this.driver = database.getDriver();
    }

    public void handle(HttpExchange r) throws IOException {

    }

}