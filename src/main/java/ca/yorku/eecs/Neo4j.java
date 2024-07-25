package ca.yorku.eecs;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.*;

public class Neo4j {
    private String uriDb;
    private Driver driver;
    public static Config config;

    // Database neo4j constructor
    public Neo4j(String username, String password) {
        uriDb = "bolt://localhost:7687";
        config = Config.builder().withoutEncryption().build();
        driver = GraphDatabase.driver(uriDb, AuthTokens.basic(username, password), config);
    }

    public void close() {
        driver.close();
    }

    public Driver getDriver() {
        return driver;
    }

}