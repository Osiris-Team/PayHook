/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.payhook.paypal;

import com.google.gson.*;
import com.osiris.autoplug.core.json.exceptions.HttpErrorException;
import com.osiris.autoplug.core.json.exceptions.WrongJsonTypeException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UtilsPayPalJson {

    public JsonElement jsonFromUrl(String requestMethod, String url, JsonElement elementToSend, MyPayPal myPayPal, Integer... successCodes) throws IOException, HttpErrorException {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.addRequestProperty("User-Agent", "PayHook");
            con.addRequestProperty("Content-Type", "application/json");
            con.addRequestProperty("Authorization", "Basic "+ myPayPal.getCredBase64());
            con.addRequestProperty("return", "representation");
            con.setConnectTimeout(1000);
            con.setRequestMethod(requestMethod);
            con.setDoOutput(true);
            con.setDoInput(true);
            con.connect();

            if(elementToSend != null){
                OutputStream out = con.getOutputStream();
                try (OutputStreamWriter outWrt = new OutputStreamWriter(out)) {
                    try (BufferedReader inr = new BufferedReader(new StringReader(new Gson().toJson(elementToSend)))) {
                        String l = null;
                        while ((l = inr.readLine()) != null) {
                            outWrt.write(l);
                        }
                    }
                }
            } // After POST finishes get RESPONSE:

            int code = con.getResponseCode();
            if (code == 200 || (successCodes != null && Arrays.asList(successCodes).contains(code))) {
                InputStream in = con.getInputStream();
                if (in != null)
                    try (InputStreamReader inr = new InputStreamReader(in)) {
                        return JsonParser.parseReader(inr);
                    }
            } else {
                JsonElement response = null;
                InputStream in = con.getErrorStream();
                if (in != null)
                    try (InputStreamReader inr = new InputStreamReader(in)) {
                        response = JsonParser.parseReader(inr);
                    }
                throw new HttpErrorException(code, null, "\nurl: "+url+" \nmessage: "+con.getResponseMessage() + "\njson: \n"  + new GsonBuilder().setPrettyPrinting().create().toJson(response));
            }
        } catch (IOException | HttpErrorException e) {
            if (con != null) con.disconnect();
            throw e;
        } finally {
            if (con != null) con.disconnect();
        }
        return null;
    }

    public JsonElement postJsonAndGetResponse(String url, JsonElement element, MyPayPal context) throws IOException, HttpErrorException {
        return jsonFromUrl("POST", url, element, context, (Integer[]) null);
    }

    public JsonElement postJsonAndGetResponse(String url, JsonElement element, MyPayPal context, Integer... successCodes) throws IOException, HttpErrorException {
        return jsonFromUrl("POST", url, element, context, successCodes);
    }

    public JsonElement patchJsonAndGetResponse(String url, JsonElement element, MyPayPal context) throws IOException, HttpErrorException {
        return jsonFromUrl("PATCH", url, element, context, (Integer[]) null);
    }

    public JsonElement patchJsonAndGetResponse(String url, JsonElement element, MyPayPal context, Integer... successCodes) throws IOException, HttpErrorException {
        return jsonFromUrl("PATCH", url, element, context, successCodes);
    }

    public JsonElement deleteAndGetResponse(String url, MyPayPal context) throws IOException, HttpErrorException {
        return jsonFromUrl("DELETE", url, null, context, 204);
    }

    public JsonElement deleteAndGetResponse(String url, MyPayPal context, Integer... successCodes) throws IOException, HttpErrorException {
        return jsonFromUrl("DELETE", url, null, context, successCodes);
    }

    /**
     * Returns the json-element. This can be a json-array or a json-object.
     *
     * @param input_url The url which leads to the json file.
     * @return JsonElement
     * @throws Exception When status code other than 200.
     */
    public JsonElement getJsonElement(String input_url, MyPayPal context) throws IOException, HttpErrorException {
        return jsonFromUrl("GET", input_url, null, context, (Integer[]) null);
    }

    public JsonArray getJsonArray(String url, MyPayPal context) throws IOException, HttpErrorException, WrongJsonTypeException {
        JsonElement element = getJsonElement(url, context);
        if (element != null && element.isJsonArray()) {
            return element.getAsJsonArray();
        } else {
            throw new WrongJsonTypeException("Its not a json array! Check it out -> " + url);
        }
    }

    /**
     * Turns a JsonArray with its objects into a list.
     *
     * @param url The url where to find the json file.
     * @return A list with JsonObjects or null if there was a error with the url.
     */
    public List<JsonObject> getJsonArrayAsList(String url, MyPayPal context) throws IOException, HttpErrorException, WrongJsonTypeException {
        List<JsonObject> objectList = new ArrayList<>();
        JsonElement element = getJsonElement(url, context);
        if (element != null && element.isJsonArray()) {
            final JsonArray ja = element.getAsJsonArray();
            for (int i = 0; i < ja.size(); i++) {
                JsonObject jo = ja.get(i).getAsJsonObject();
                objectList.add(jo);
            }
            return objectList;
        } else {
            throw new WrongJsonTypeException("Its not a json array! Check it out -> " + url);
        }
    }

    /**
     * Gets a single JsonObject.
     *
     * @param url The url where to find the json file.
     * @return A JsonObject or null if there was a error with the url.
     */
    public JsonObject getJsonObject(String url, MyPayPal context) throws IOException, HttpErrorException, WrongJsonTypeException {
        JsonElement element = getJsonElement(url, context);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        } else {
            throw new WrongJsonTypeException("Its not a json object! Check it out -> " + url);
        }
    }

}
