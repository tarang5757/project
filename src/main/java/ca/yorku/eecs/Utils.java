package ca.yorku.eecs;

import java.io.BufferedReader;


import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
	public static String convert(InputStream inputStream) throws IOException {

		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
			return br.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	public static Map<String, String> parseQuery(String query) {
		Map<String, String> queryPairs = new HashMap<>();
		//Split parameters into pairs
		if(query != null && !query.isEmpty()) {
			String[] params = query.split("&");
			for (String param : params) {
				int idx = param.indexOf("=");
				try {
					//Use URL decoder to decode the parameter's special characters.
					String key = URLDecoder.decode(param.substring(0, idx), "UTF-8");
					String value = URLDecoder.decode(param.substring(idx + 1), "UTF-8");
					queryPairs.put(key, value);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return queryPairs;
	}

	public static JSONObject getParameters(HttpExchange r) {
		JSONObject deserialized = null;
		try {
			String body = convert(r.getRequestBody());
			//Can accept query parameters sent in URL or request body. Request body will take precedence.
			if(!body.isEmpty()) {
				//Request Body
				deserialized = new JSONObject(body);
			}
			else {
				//If no body is given, look for query parameters 
				URI uri = r.getRequestURI();
				String query = uri.getQuery();
				Map<String, String> params = parseQuery(query);
				deserialized = new JSONObject(params);
			}
		} catch(JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return deserialized;
	}
}