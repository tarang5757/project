package ca.yorku.eecs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class App {
    static int PORT = 3000;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        String username = "neo4j";
        String password = "12345678";

        Neo4j database = new Neo4j(username, password);

        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}