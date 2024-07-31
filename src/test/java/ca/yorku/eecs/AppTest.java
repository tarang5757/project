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
		suite.addTest(new AppTest("addMoviePass"));
		suite.addTest(new AppTest("addMovieFail"));
		suite.addTest(new AppTest("getMoviePass"));
		suite.addTest(new AppTest("getMovieFail"));
		suite.addTest(new AppTest("addActorPass"));
		suite.addTest(new AppTest("addActorFail"));
		suite.addTest(new AppTest("getCoActorsPass"));
		suite.addTest(new AppTest("getCoActorsFail"));
		//suite.addTest(new AppTest("addRatingPass"));
		//suite.addTest(new AppTest("addRatingFail"));
		suite.addTest(new AppTest("getRatingPass"));
		suite.addTest(new AppTest("getRatingFail"));

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
	public void addMoviePass() throws JSONException {
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
		assertEquals(405, statusCode);

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

	//Returns a 200 code for successfully adding a Movie to the database
	public void getMoviePass() throws JSONException {
		JSONObject putRequest = new JSONObject();
		putRequest.put("name", "Click");
		putRequest.put("movieId", "1234");

		int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", putRequest);   //Add movie to database
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getMovie?movieId=1234", null);		//Get movie that was added
		assertEquals(200, statusCode);
	}

	//Returns a 400 code for improper request method/body. Returns a 404 code for movieId not found
	public void getMovieFail() throws JSONException {
		//TEST 1: Wrong Method Type (405)
		int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/getMovie?movieId=1234", null); 
		assertEquals(405, statusCode);

		//TEST 2: Improper Formatting (Missing/Misspelled information) (400)
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getMovie?movieasdasId=1234", null);
		assertEquals(400, statusCode);
		
		//TEST 3: Movie Does not exist (404)
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getMovie?movieId=123456", null);
		assertEquals(404, statusCode);
		
		resetDatabase();
	}

	public void addActorPass() throws JSONException {
		JSONObject request = new JSONObject();
		request.put("name", "Robert Downey JR");
		request.put("actorId", "777");

		int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", request);
		assertEquals(200, statusCode);


	}

	public void addActorFail() throws JSONException {
		//First Test case Wrong method type
		JSONObject wrongRequestType = new JSONObject();
		wrongRequestType.put("name", "test1");
		wrongRequestType.put("actorId", "234");
		int statuscode = sendRequest("GET", "http://localhost:8080/api/v1/addActor", wrongRequestType);
		assertEquals(405, statuscode);
		System.out.println("here is staus code for addActorFail: " + statuscode);


		//improper format (Missing or misspelled information)
		JSONObject incorrectFormatRequest = new JSONObject();
		incorrectFormatRequest.put("name", "incorrectformatRequest");
		incorrectFormatRequest.put("actorID", "000"); //actorID instead of actorId
		int statuscode1 = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", incorrectFormatRequest);
		System.out.println("Missing information or misspelled information. status code: " + statuscode1);
		assertEquals(400, statuscode1);

		//movie exists
		JSONObject movieExistsRequest = new JSONObject();
		movieExistsRequest.put("name", "Robert Downey JR");
		movieExistsRequest.put("actorId", "777");
		int statuscode2 = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", movieExistsRequest);
		assertEquals(400, statuscode2);
		System.out.println("Movie already exists. status code: " + statuscode2);

		resetDatabase();
	}

	public void getActorPass() throws JSONException {

	}

	public void getActorFail() throws JSONException {
		
	}
	
	public void getCoActorsPass() throws JSONException {
		//Populate DB for getCoActors tests
		JSONObject actorOneRequest = new JSONObject();
		JSONObject actorTwoRequest = new JSONObject();
		JSONObject movieRequest = new JSONObject();
		JSONObject relationshipOne = new JSONObject();
		JSONObject relationshipTwo = new JSONObject();
		JSONObject getCoactorsRequest = new JSONObject();
		actorOneRequest.put("name", "Adam Sandler");  //Request to add Adam Sandler to DB
		actorOneRequest.put("actorId", "1111");
		
		actorTwoRequest.put("name", "Kevin James");    //Request to add Kevin James to DB
		actorTwoRequest.put("actorId", "2222");
		
		movieRequest.put("name", "Grown Ups");    //Request to add Grown Ups to DB
		movieRequest.put("movieId", "3333");
		
		relationshipOne.put("movieId", "3333");   //Request to add Adam-[ACTED_IN]->Grown Ups
		relationshipOne.put("actorId", "1111");
		relationshipTwo.put("movieId", "3333");   //Request to add Kevin-[ACTED_IN]->Grown Ups
		relationshipTwo.put("actorId", "2222");
		
		getCoactorsRequest.put("actorId", "1111"); //Request for Adam Sandler's co-actors

		int dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", actorOneRequest);
		dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", actorTwoRequest);
		dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", movieRequest);
		dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationshipOne);
		dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationshipTwo);
		
		int statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getCoActors?actorId=1111", null);
		assertEquals(200, statusCode);
	}
	
	public void getCoActorsFail() throws JSONException {
		//TEST 1: Wrong Method Type (PUT instead of GET) (405)
		int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/getCoActors?actorId=1111", null); 
		assertEquals(405, statusCode);

		//TEST 2: Improper Formatting (actorID instead of actorId) (400)
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getCoActors?actorID=1111", null);
		assertEquals(400, statusCode);

		//TEST 3: Movie Does not exist (404)
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getCoActors?actorId=123456", null);
		assertEquals(404, statusCode);

		resetDatabase();
	}
	
	public void addRatingPass() {
		
	}
	
	public void addRatingFail() {
		
	}
	
	//Returns a 200 code for successful rating retrieval
	public void getRatingPass() throws JSONException {
		JSONObject request = new JSONObject();
		JSONObject ratingRequest = new JSONObject();
		request.put("name", "Goodfellas");
		request.put("movieId", "101");
		ratingRequest.put("movieId", "101");
		ratingRequest.put("rating", "10");

		int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", request);
		statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRating", ratingRequest);
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getRating?movieId=101", null);
		assertEquals(200, statusCode);
	}
	
	//Returns a 400 code for improper request method/body, or 404 for non-existent movie/rating
	public void getRatingFail() throws JSONException {
		// Add movie to DB with no rating. (movie 101 added above has a rating)
		JSONObject request = new JSONObject();
		request.put("name","Taxi Driver");
		request.put("movieId", "102");
		
		//TEST 1: Wrong Method Type (PUT instead of GET) (405)
		int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/getRating?movieId=101", null); 
		assertEquals(405, statusCode);
		
		//TEST 2: Improper Formatting (mooovieId instead of movieId) (400)
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getRating?mooovieId=101", null); 
		assertEquals(400, statusCode);
		
		//TEST 3: Movie does not exist or rating does not exist (404)
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getRaaaaating?movieId=1033", null); 
		assertEquals(404, statusCode);
		
		resetDatabase();
	}
	
}
