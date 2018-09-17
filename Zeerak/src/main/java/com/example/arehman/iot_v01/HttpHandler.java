package com.example.arehman.iot_v01;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by arehman on 9/18/17.
 */

public class HttpHandler {
    /**
     * Make HTTP GET request
     *
     * @param url
     * @return
     * @throws Exception
     */
    public HttpResponse sendGet(String url) throws Exception {
        HttpURLConnection con = null;
        try {
            URL obj = new URL(url);
            HttpResponse response;

            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);

            try {
                con.setRequestProperty("User-Agent", "Mozilla/5.0");

                con.connect();

                int resp = con.getResponseCode();


                InputStream ist = null;

                if (resp == 200) {
                  ist = con.getInputStream();
                } else if  (resp == 400) {
                  ist = con.getErrorStream();
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(ist));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                System.out.println(result.toString());
                response = new HttpResponse(con.getResponseCode(), result.toString());

            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }

            return response;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

    }

}
