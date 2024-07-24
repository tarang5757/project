package ca.yorku.eecs;

import org.neo4j.driver.v1.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

import org.neo4j.driver.v1.Driver;

import org.neo4j.driver.v1.net.ServerAddress;
import org.neo4j.driver.*;

public class Neo4j {
    private String uriDb;
    private Driver driver;
    public static Config config;

    // Database neo4j constructor
    public Neo4j(String username, String password) {
        uriDb = "bolt://localhost:7687";
        config = Config.builder().withoutEncryption().build();
        driver = GraphDatabase.driver(uriDb, AuthTokens.basic(username, password), config);
        System.out.println("Connection with database was successfull");
    }

    public void close() {
        driver.close();
    }

    public Driver getDriver() {
        return driver;
    }

}
