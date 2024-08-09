package ca.yorku.eecs;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.*;

import org.neo4j.driver.v1.Session;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
//fixed

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
		suite.addTest(new AppTest("getActorPass"));
		suite.addTest(new AppTest("getActorFail"));
		suite.addTest(new AppTest("getCoActorsPass"));
		suite.addTest(new AppTest("getCoActorsFail"));
		suite.addTest(new AppTest("getRatingPass"));
		suite.addTest(new AppTest("getRatingFail"));
		suite.addTest(new AppTest("addRatingPass"));
		suite.addTest(new AppTest("addRatingFail"));
		suite.addTest(new AppTest("computeBaconPathPass"));
		suite.addTest(new AppTest("computeBaconPathFail"));
		suite.addTest(new AppTest("computeBaconNumberPass"));
		suite.addTest(new AppTest("computeBaconNumberFail"));
		suite.addTest(new AppTest("getMoviesWithRatingPass")); 
		suite.addTest(new AppTest("getMoviesWithRatingFail")); 
		suite.addTest(new AppTest("addRelationshipPass"));
		suite.addTest(new AppTest("addRelationshipFail"));
		suite.addTest(new AppTest("hasRelationshipPass"));
		suite.addTest(new AppTest("hasRelationshipFail"));

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
		JSONObject putRequest = new JSONObject();
		JSONObject getRequest = new JSONObject();
		putRequest.put("name", "Click");
		putRequest.put("actorId", "1234");
		getRequest.put("actorId", "1234");

		int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", putRequest);   //Add movie to database
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getActor?actorId=1234", null);		//Get movie that was added
		System.out.println("getActorPass status code: " + statusCode);
		assertEquals(200, statusCode);


	}

	public void getActorFail() throws JSONException {
		//TEST 1: Wrong Method Type (PUT instead of GET) (405)
		int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/getActors?actorId=1234", null); 

		assertEquals(405, statusCode);

		//TEST 2: Improper Formatting (actorID instead of actorId) (400)
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getActor?actorID=1234", null);
		assertEquals(400, statusCode);

		//TEST 3: Movie Does not exist (404)
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getActor?actorId=12344", null);
		assertEquals(404, statusCode);

		resetDatabase();

		
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

	public void addRatingPass() throws JSONException {
		//Create JSON Object
		JSONObject addMovie = new JSONObject();
		JSONObject addRatingRequest= new JSONObject();

		//add values to fields
		addMovie.put("name", "John Wick Chapter 1");
		addMovie.put("movieId", "100");

		int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", addMovie);

		//Checking status
		assertEquals(200, statusCode);

		//add values to fields
		addRatingRequest.put("movieId", "100");
		addRatingRequest.put("rating", "5.0");

		//sending request
		int statusCode1 = sendRequest("PUT", "http://localhost:8080/api/v1/addRating", addRatingRequest);
		System.out.println("here is staus code for addRatingPass: " + statusCode1);

		//checking status
		assertEquals(200, statusCode1);
	}

	public void addRatingFail() throws JSONException {
		int statusCode = 0;
		//Movie with ID 100 already exists in DB
		
		//TEST 1: Wrong method type (405)
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/addRating?movieId=100&rating=6.0", null);
		assertEquals(405,statusCode);
		
		//TEST 2: Rating out of bounds (400)
		statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRating?movieId=100&rating=24", null);	
		assertEquals(400,statusCode);
		
		//TEST 3: Improper Formatting/Missing Info
		statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRating?mddddovieId=100&rating=6.0", null);		
		assertEquals(400,statusCode);
		
		//TEST 3: Movie Does not exist
		statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRating?movieId=1234567&rating=6.0", null);
		assertEquals(404,statusCode);
		
	}

	public void getMoviesWithRatingPass() throws JSONException{
		// 1. create testS for add movie
		// 2. create testS for add rating
		// 3. create tests for get movies with rating 

		JSONObject addMovie = new JSONObject();
		addMovie.put("name", "despicable me");
		addMovie.put("movieId", "1");

		//send request
		int statusCodeAddMovie = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", addMovie);

		//test addMovie statusCode
		assertEquals(200, statusCodeAddMovie);

		//movie 2
		JSONObject addMovie2 = new JSONObject();
		addMovie2.put("name", "despicable me 2");
		addMovie2.put("movieId", "2");

		//send request
		int statusCodeAddMovie2 = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", addMovie2);

		//movie 3
		JSONObject addMovie3 = new JSONObject();
		addMovie3.put("name", "despicable me 3");
		addMovie3.put("movieId", "3");

		//send request
		int statusCodeAddMovie3 = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", addMovie3);
		assertEquals(200, statusCodeAddMovie3);

		//add Rating for Movie 1
		JSONObject rateMovie1 = new JSONObject();
		rateMovie1.put("movieId", "1");
		rateMovie1.put("rating", "5.0");
		int statusCodeRateMovie1 = sendRequest("PUT", "http://localhost:8080/api/v1/addRating", rateMovie1);

		//add Rating for Movie 2
		JSONObject rateMovie2 = new JSONObject();
		rateMovie2.put("movieId", "2");
		rateMovie2.put("rating", "6.0");
		int statusCodeRateMovie2 = sendRequest("PUT", "http://localhost:8080/api/v1/addRating", rateMovie2);


		//add Rating for Movie 3
		JSONObject rateMovie3 = new JSONObject();
		rateMovie3.put("movieId", "3");
		rateMovie3.put("rating", "7.0");
		int statusCodeRateMovie3 = sendRequest("PUT", "http://localhost:8080/api/v1/addRating", rateMovie3);


		//getMoviesWithRating
		//sending request
		int statusCodeMovieRating = sendRequest("GET", "http://localhost:8080/api/v1/getMoviesWithRating?rating=5.0", null);
		//checking status
		assertEquals(200, statusCodeMovieRating);

	}

	public void getMoviesWithRatingFail() {
		int statusCode = 0;
		//Movie with ID 100 already exists in DB
		
		//TEST 1: Wrong method type (405)
		statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/getMoviesWithRatingFail?rating=6.0", null);
		assertEquals(405,statusCode);
		
		//TEST 3: Improper Formatting/Missing Info
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getMoviesWithRatingFail?rattttting=6.0", null);		
		assertEquals(400,statusCode);
		
		//TEST 3: Movie with applicable rating does not exist (using DB from previous test)
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/getMoviesWithRatingFail?rating=10", null);
		assertEquals(404,statusCode);
		
		resetDatabase();
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

	// computeBaconPath
	public void computeBaconPathPass() throws JSONException {
		// Populate DB with Kevin Bacon and another actor to test the shortest path
		JSONObject kevinBaconRequest = new JSONObject();
		kevinBaconRequest.put("name", "Kevin Bacon");
		kevinBaconRequest.put("actorId", "nm0000102");

		JSONObject otherActorRequest = new JSONObject();
		otherActorRequest.put("name", "Tom Hanks");
		otherActorRequest.put("actorId", "nm0000158");

		JSONObject movieRequest = new JSONObject();
		movieRequest.put("name", "Apollo 13");
		movieRequest.put("movieId", "m00001");

		JSONObject relationshipOne = new JSONObject();
		relationshipOne.put("movieId", "m00001");
		relationshipOne.put("actorId", "nm0000102");

		JSONObject relationshipTwo = new JSONObject();
		relationshipTwo.put("movieId", "m00001");
		relationshipTwo.put("actorId", "nm0000158");

		// Add Kevin Bacon and another actor
		int dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", kevinBaconRequest);
		dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", otherActorRequest);
		dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", movieRequest);
		dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationshipOne);
		dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationshipTwo);

		// Request to compute Bacon Path for Tom Hanks
		int statusCode = sendRequest("GET", "http://localhost:8080/api/v1/computeBaconPath?actorId=nm0000158", null);
		assertEquals(200, statusCode);

		// Request for Kevin Bacon himself
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/computeBaconPath?actorId=nm0000102", null);
		assertEquals(200, statusCode);

		// Add another actor with multiple paths to Kevin Bacon
		JSONObject secondActorRequest = new JSONObject();
		secondActorRequest.put("name", "Kevin Spacey");
		secondActorRequest.put("actorId", "nm0000228");

		JSONObject secondMovieRequest = new JSONObject();
		secondMovieRequest.put("name", "A Time to Kill");
		secondMovieRequest.put("movieId", "m00002");

		JSONObject relationshipThree = new JSONObject();
		relationshipThree.put("movieId", "m00002");
		relationshipThree.put("actorId", "nm0000102");

		JSONObject relationshipFour = new JSONObject();
		relationshipFour.put("movieId", "m00002");
		relationshipFour.put("actorId", "nm0000228");

		sendRequest("PUT", "http://localhost:8080/api/v1/addActor", secondActorRequest);
		sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", secondMovieRequest);
		sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationshipThree);
		sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationshipFour);

		// Request to compute Bacon Path for Kevin Spacey
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/computeBaconPath?actorId=nm0000228", null);
		assertEquals(200, statusCode);
	}
	public void computeBaconNumberPass() throws JSONException {
		// Populate DB with Kevin Bacon and another actor to test the Bacon number
		JSONObject kevinBaconRequest = new JSONObject();
		kevinBaconRequest.put("name", "Kevin Bacon");
		kevinBaconRequest.put("actorId", "nm0000102");

		JSONObject otherActorRequest = new JSONObject();
		otherActorRequest.put("name", "Tom Hanks");
		otherActorRequest.put("actorId", "nm0000158");

		JSONObject movieRequest = new JSONObject();
		movieRequest.put("name", "Apollo 13");
		movieRequest.put("movieId", "m00001");

		JSONObject relationshipOne = new JSONObject();
		relationshipOne.put("movieId", "m00001");
		relationshipOne.put("actorId", "nm0000102");

		JSONObject relationshipTwo = new JSONObject();
		relationshipTwo.put("movieId", "m00001");
		relationshipTwo.put("actorId", "nm0000158");

		// Add Kevin Bacon and another actor
		int dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", kevinBaconRequest);
		dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", otherActorRequest);
		dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", movieRequest);
		dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationshipOne);
		dummyCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationshipTwo);

		// Request to compute Bacon Number for Tom Hanks
		int statusCode = sendRequest("GET", "http://localhost:8080/api/v1/computeBaconNumber?actorId=nm0000158", null);
		assertEquals(200, statusCode);

		// Request to compute Bacon Number for Kevin Bacon himself
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/computeBaconNumber?actorId=nm0000102", null);
		assertEquals(200, statusCode);
	}

	public void computeBaconNumberFail() throws JSONException {
		// 400 BAD REQUEST - Improperly formatted request parameter
		int statusCode = sendRequest("GET", "http://localhost:8080/api/v1/computeBaconNumber?actorID=nm0000158", null);
		assertEquals(400, statusCode);

		// 400 BAD REQUEST - Missing query parameter
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/computeBaconNumber", null);
		assertEquals(400, statusCode);

		// 404 NOT FOUND - Actor not in the database
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/computeBaconNumber?actorId=unknownActor", null);
		assertEquals(404, statusCode);

		// 404 NOT FOUND - Actor with no path to Kevin Bacon
		JSONObject unrelatedActorRequest = new JSONObject();
		unrelatedActorRequest.put("name", "Unrelated Actor");
		unrelatedActorRequest.put("actorId", "nm9999999");

		sendRequest("PUT", "http://localhost:8080/api/v1/addActor", unrelatedActorRequest);

		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/computeBaconNumber?actorId=nm9999999", null);
		assertEquals(404, statusCode);

	}

	public void computeBaconPathFail() throws JSONException {
		// 400 BAD REQUEST - Improperly formatted request parameter
		int statusCode = sendRequest("GET", "http://localhost:8080/api/v1/computeBaconPath?actorID=nm0000158", null);
		assertEquals(400, statusCode);

		// 400 BAD REQUEST - Missing query parameter
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/computeBaconPath", null);
		assertEquals(400, statusCode);

		// 404 NOT FOUND - Actor not in the database
		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/computeBaconPath?actorId=unknownActor", null);
		assertEquals(404, statusCode);

		// 404 NOT FOUND - Actor with no path to Kevin Bacon
		JSONObject unrelatedActorRequest = new JSONObject();
		unrelatedActorRequest.put("name", "Unrelated Actor");
		unrelatedActorRequest.put("actorId", "nm9999999");

		sendRequest("PUT", "http://localhost:8080/api/v1/addActor", unrelatedActorRequest);

		statusCode = sendRequest("GET", "http://localhost:8080/api/v1/computeBaconPath?actorId=nm9999999", null);
		assertEquals(404, statusCode);

		resetDatabase();

	}
	public void addRelationshipPass() throws JSONException {
	    // 200 Success
	    JSONObject actor = new JSONObject();
	    JSONObject movie = new JSONObject();
	    JSONObject relationship = new JSONObject();
	    
	    actor.put("name", "J.K. Simmons");
	    actor.put("actorId", "4897837");
	    int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", actor);
	    assertEquals(200, statusCode);

	    movie.put("name", "Whiplash");
	    movie.put("movieId", "8748347");
	    statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", movie);
	    assertEquals(200, statusCode); 

	    relationship.put("actorId", "4897837");
	    relationship.put("movieId", "8748347");
	    statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationship);
	    assertEquals(200, statusCode);
	    resetDatabase();
	}

	public void addRelationshipFail() throws JSONException {
		// 404 Actor does not exist in DB
	    JSONObject movieOneRequest = new JSONObject();
	    JSONObject relationshipOneRequest = new JSONObject();
	    movieOneRequest.put("name", "Se7en");
	    movieOneRequest.put("movieId", "654323232323");
	    sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", movieOneRequest);
	    relationshipOneRequest.put("actorId", "234563232232");
	    relationshipOneRequest.put("movieId", "654323232323");
	    int statusCodeOne = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationshipOneRequest);
	    assertEquals(404, statusCodeOne);
	    
	    // 404 Movie does not exist in DB
	    JSONObject relationshipTwoRequest = new JSONObject();
	    JSONObject ActorTwoRequest = new JSONObject();
	    ActorTwoRequest.put("name", "Hugh Jackman");
	    ActorTwoRequest.put("actorId", "923893");
	    sendRequest("PUT", "http://localhost:8080/api/v1/addActor", ActorTwoRequest);
	    relationshipTwoRequest.put("actorId", "923893");
	    relationshipTwoRequest.put("movieId", "387593");
	    int statusCodeTwo = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationshipTwoRequest);
	    assertEquals(404, statusCodeTwo);
	    
	    //400 Missing Info
	    JSONObject relationshipThreeRequest = new JSONObject();
	    JSONObject ActorThreeRequest = new JSONObject();
	    JSONObject movieThreeRequest = new JSONObject();
	    ActorThreeRequest.put("name", "Cilian Murphy");
	    ActorThreeRequest.put("actorId", "3944394394");
	    sendRequest("PUT", "http://localhost:8080/api/v1/addActor", ActorThreeRequest);
	    movieThreeRequest.put("name", "Batman Begins");
	    movieThreeRequest.put("movieId", "9283945784");
	    sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", movieThreeRequest);
	    relationshipThreeRequest.put("movieId", "9283945784");
	    int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationshipThreeRequest);
	    assertEquals(400, statusCode);
	    resetDatabase();
	}
	
	public void hasRelationshipPass() throws JSONException{
        //200 Success
        JSONObject actor = new JSONObject();
        JSONObject movie = new JSONObject();
        JSONObject relationship = new JSONObject();
        JSONObject hasRelationship = new JSONObject();

        actor.put("name", "Jack Black");
        actor.put("actorId", "4897878737");
        int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", actor);
        assertEquals(200, statusCode);

        movie.put("name", "Kung-Fu Panda");
        movie.put("movieId", "874833253647");
        statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", movie);
        assertEquals(200, statusCode); 

        relationship.put("actorId", "4897878737");
        relationship.put("movieId", "874833253647");
        statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationship);
        assertEquals(200, statusCode); 
        
        hasRelationship.put("actorId", "4897878737");
        hasRelationship.put("movieId", "874833253647");
        statusCode = sendRequest("GET", "http://localhost:8080/api/v1/hasRelationship?actorId=4897878737&movieId=874833253647", null);
        assertEquals(200, statusCode); 
        resetDatabase();
    }
	
	public void hasRelationshipFail() throws JSONException{
		//400 Improper formatting for Actor 
        JSONObject actor = new JSONObject();
        JSONObject movie = new JSONObject();
        JSONObject relationship = new JSONObject();
        JSONObject hasRelationship = new JSONObject();
		
        actor.put("name", "Keanu Reeves");
        actor.put("actorId", "435");
        int statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", actor);
        assertEquals(200, statusCode);

        movie.put("name", "John Wick");
        movie.put("movieId", "8747");
        statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", movie);
        assertEquals(200, statusCode); 

        relationship.put("actorId", "435");
        relationship.put("movieId", "8747");
        statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationship);
        assertEquals(200, statusCode); 
        
        hasRelationship.put("actorId", "435");
        hasRelationship.put("movieId", "8747");
        statusCode = sendRequest("GET", "http://localhost:8080/api/v1/hasRelationship?actrId=435&movieId=8747", null);
        assertEquals(400, statusCode); 
        
        //400 Improper formatting for Movie
        JSONObject actor2 = new JSONObject();
        JSONObject movie2 = new JSONObject();
        JSONObject relationship2 = new JSONObject();
        JSONObject hasRelationship2 = new JSONObject();
		
        actor2.put("name", "Christian Bale");
        actor2.put("actorId", "4346");
        statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", actor2);
        assertEquals(200, statusCode);

        movie2.put("name", "American Psycho");
        movie2.put("movieId", "8567");
        statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", movie2);
        assertEquals(200, statusCode); 

        relationship2.put("actorId", "4346");
        relationship2.put("movieId", "8567");
        statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationship2);
        assertEquals(200, statusCode); 
        
        hasRelationship2.put("actorId", "4346");
        hasRelationship2.put("movieId", "8567");
        statusCode = sendRequest("GET", "http://localhost:8080/api/v1/hasRelationship?actorId=4346&moviId=8567", null);
        assertEquals(400, statusCode); 
        
        
		//404 Movie does not exist in DB
        JSONObject actorOne = new JSONObject();
        JSONObject movieOne = new JSONObject();
        JSONObject relationshipOne = new JSONObject();
        JSONObject hasRelationshipOne = new JSONObject();
		
        actorOne.put("name", "Willem Dafoe");
        actorOne.put("actorId", "382738");
        statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", actorOne);
        assertEquals(200, statusCode);
        
        movieOne.put("name", "Spider-Man");
        movieOne.put("movieId", "382248");
        statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", movieOne);
        assertEquals(200, statusCode);
        
        relationshipOne.put("actorId", "382738");
        relationshipOne.put("movieId", "382248");
        statusCode = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationshipOne);
        assertEquals(200, statusCode);
        
        hasRelationshipOne.put("actorId", "382738");
        hasRelationshipOne.put("movieId", "382248");
        statusCode = sendRequest("GET", "http://localhost:8080/api/v1/hasRelationship?actorId=382738&movieId=874833253647", null);
        assertEquals(404, statusCode);
        
        
        //404 Actor does not exist in DB
        JSONObject actorTwo = new JSONObject();
        JSONObject movieTwo = new JSONObject();
        JSONObject relationshipTwo = new JSONObject();
        JSONObject hasRelationshipTwo = new JSONObject();
        
        actorTwo.put("name", "Ryan Reynolds");
        actorTwo.put("actorId", "3828");
        int statusCodeTwo = sendRequest("PUT", "http://localhost:8080/api/v1/addActor", actorTwo);
        assertEquals(200, statusCodeTwo);
        
        movieTwo.put("name", "Green Lantern");
        movieTwo.put("movieId", "3868");
        statusCodeTwo = sendRequest("PUT", "http://localhost:8080/api/v1/addMovie", movieTwo);
        assertEquals(200, statusCodeTwo);
        
        relationshipTwo.put("actorId", "3828");
        relationshipTwo.put("movieId", "3868");
        statusCodeTwo = sendRequest("PUT", "http://localhost:8080/api/v1/addRelationship", relationshipTwo);
        assertEquals(200, statusCodeTwo);
        
        hasRelationshipTwo.put("actorId", "3828");
        hasRelationshipTwo.put("movieId", "3868");
        statusCode = sendRequest("GET", "http://localhost:8080/api/v1/hasRelationship?actorId=383338&movieId=3868", null);
        assertEquals(404, statusCode);
		resetDatabase();
	}
}
