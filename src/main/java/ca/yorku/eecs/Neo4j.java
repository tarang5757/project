package ca.yorku.eecs;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
public class Neo4j implements AutoCloseable{
	
	private Driver driver;

    // Database neo4j constructor
    public Neo4j(String url, String user, String password) {
        driver = GraphDatabase.driver(url, AuthTokens.basic(user, password));
    }

    @Override
    public void close() throws Exception {
        driver.close();
    }

    public Driver getDriver() {
        return driver;
    }

}
