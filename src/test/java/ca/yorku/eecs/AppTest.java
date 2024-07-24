package ca.yorku.eecs;



import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest
        extends TestCase {
	
	
	public void testaddMovieSuccess() throws JSONException {
		JSONObject request = new JSONObject();
		request.put("name", "Goodfellas");
		request.put("movieId", "15000");

		//Create URL and HttpURLConnection instances
		try {
			URL url = new URL("http://localhost:8080/api/v1/addMovie");
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			//connection.setRequestProperty("Content-Type", "application/json");

			//Write request body into connection
			try(OutputStream out = connection.getOutputStream()){
				out.write(request.toString().getBytes("UTF-8"));
				out.close();
				connection.connect();
			}
			int statusCode = connection.getResponseCode();
			assertEquals(200, statusCode);
		} catch (Exception e) {
			fail();
		}
    }

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
        return new TestSuite(AppTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        assertTrue(true);
    }
    
    
}
