package ca.yorku.eecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {
    public static String convert(InputStream inputStream) throws IOException {

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
    
    public static Map<String, String> parseQuery(String query) {
        Map<String, String> queryPairs = new HashMap<>();
        //Split parameters into pairs
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
        return queryPairs;
    }
}