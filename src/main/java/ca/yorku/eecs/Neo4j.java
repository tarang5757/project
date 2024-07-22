package ca.yorku.eecs;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
public class Neo4j implements AutoCloseable{
	
	public static Config config;
	private Driver driver;
	private String url;

    // Database neo4j constructor
    public Neo4j() {
        url = "bolt://localhost:7687";
        config = Config.builder().withoutEncryption().build();
    	driver = GraphDatabase.driver(url, AuthTokens.basic("neo4j", "12345678"), config);
    }

    @Override
    public void close() throws Exception {
        driver.close();
    }

    public Driver getDriver() {
        return driver;
    }

}
