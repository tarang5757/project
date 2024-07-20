package ca.yorku.eecs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;



public class App 
{
    static int PORT = 8080;
    public static void main(String[] args) throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        
        String url = "bolt://localhost:7687";

        String user = "neo4j";
        String password = "12345678";
        Neo4j db = new Neo4j(url, user, password);


        
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}