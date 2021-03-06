/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.payhook;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.osiris.payhook.exceptions.HttpErrorException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class PayPalJsonUtils {

    public JsonElement postJsonAndGetResponse(String input_url, JsonElement element, String base64EncodedCredentials) throws IOException, HttpErrorException {
        return postJsonAndGetResponse(input_url, new Gson().toJson(element), base64EncodedCredentials);
    }

    public JsonElement postJsonAndGetResponse(String input_url, String body, String base64EncodedCredentials) throws IOException, HttpErrorException {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(input_url).openConnection();
            con.addRequestProperty("User-Agent", "AutoPlug - https://autoplug.online - Request-ID: " + new Random().nextInt());
            con.addRequestProperty("Content-Type", "application/json");
            con.addRequestProperty("Authorization", "Basic " + base64EncodedCredentials);
            con.setConnectTimeout(1000);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.connect();
            try (OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream())) {
                try (BufferedReader inr = new BufferedReader(new StringReader(body))) {
                    String l = null;
                    while ((l = inr.readLine()) != null) {
                        out.write(l);
                    }
                }
            } // After POST finishes get RESPONSE:
            int code = con.getResponseCode();
            if (code == 200) {
                try (InputStreamReader inr = new InputStreamReader(con.getInputStream())) {
                    return JsonParser.parseReader(inr);
                }
            } else {
                throw new HttpErrorException(code, null, con.getResponseMessage());
            }
        } catch (IOException | HttpErrorException e) {
            if (con != null) con.disconnect();
            throw e;
        } finally {
            if (con != null) con.disconnect();
        }
    }


}
