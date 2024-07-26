package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.*;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest
extends TestCase {
	Neo4j database;
	
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public AppTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(new AppTest("setup"));	//Start connection and initialize driver
		suite.addTest(new AppTest("addMovieSuccess"));
		suite.addTest(new AppTest("addMovieFail"));
		return suite;
		//return new TestSuite(AppTest.class);
	}

	/**
	 * Rigourous Test :-)
	 */
	public void testApp() {
		assertTrue(true);
	}
	
	/**
	 * Starts the connection, initializes driver, and deletes all nodes currently in the
	 * database to prepare for tests.
	 */
	public void setup() {
		try {
			App.main(new String[0]);
			Thread.sleep(1000); //Allow server to start
			resetDatabase();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void resetDatabase() {
		database = App.getDatabase();
		try (Session session = database.getDriver().session()){
			session.run("MATCH(n) DETACH DELETE n");
			session.close();
		}
	}
	

	/**
	 * @param method: request type (ie. PUT, GET, ...)
	 * @param urlStr: target endpoint
	 * @param requestBody 
	 * @return status code sent in response or -1 if an error occurs
	 */
	private int sendRequest(String method, String urlStr, JSONObject requestBody) {
		try {
			URL url = new URL(urlStr);
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod(method);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json");

			//Write Request Body into connection
			if(requestBody != null) {
				try(OutputStream out = connection.getOutputStream()){
					out.write(requestBody.toString().getBytes("UTF-8"));
					out.close();
				}
			}
			connection.connect();
			return connection.getResponseCode();
		} catch(Exception e) {
			fail("Request failed:" + e.getMessage());
			return -1;
		}
	}
	
	//Returns a 200 OK code for a successful add
	public void addMovieSuccess() throws JSONException {
		JSONObject request = new JSONObject();
		request.put("name", "Goodfellas");
		request.put("movieId", "15000");
		
		int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", request);
		assertEquals(200, statusCode);
	}
	
	//Returns a 400 code for improper request method/body, or pre-existing movie
	public void addMovieFail() throws JSONException{
		//TEST 1: Wrong Method Type
		JSONObject wrongMethodRequest = new JSONObject();
		wrongMethodRequest.put("name", "Despicable Me");
		wrongMethodRequest.put("movieId", "16000");
		
		int statusCode = sendRequest("GET", "http://localhost:8080/api/v1/addMovie", wrongMethodRequest);
		assertEquals(400, statusCode);
		
		//TEST 2: Improper Formatting (Missing/Misspelled information)
		JSONObject improperFormatRequest = new JSONObject();
		improperFormatRequest.put("movieName", "Elf");  //Wrong field name
		improperFormatRequest.put("movieId", "17000");
		
		statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", improperFormatRequest);
		assertEquals(400, statusCode);
		
		//TEST 3: Movie Already Exists
		JSONObject repeatRequest = new JSONObject();
		repeatRequest.put("name", "Goodfellas");
		repeatRequest.put("movieId", "15000");
		
		//Movie was already added in addMovieSuccess
		statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", repeatRequest);
		assertEquals(400, statusCode);
		
		resetDatabase();
	}
}
