/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.payhook.paypal;

import com.google.gson.*;
import com.osiris.jlib.json.exceptions.HttpErrorException;
import com.osiris.jlib.json.exceptions.WrongJsonTypeException;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class UtilsPayPalJson {
    // Use OkHttp because I couldn't find a way of
    // sending PATCH http requests to PayPal
    // without Java throwing exceptions...
    public static MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public static OkHttpClient CLIENT = new OkHttpClient();

    public JsonElement jsonFromUrl(String requestMethod, String url, JsonElement elementToSend, PayPalUtils payPalUtils, Integer... successCodes) throws IOException, HttpErrorException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("User-Agent", "PayHook")
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + payPalUtils.getCredBase64())
                .header("return", "representation");
        if (elementToSend != null) {
            RequestBody body = RequestBody.create(new Gson().toJson(elementToSend), JSON);
            requestBuilder.method(requestMethod, body);
        } else {
            requestBuilder.method(requestMethod, null);
        }
        try (Response response = CLIENT.newCall(requestBuilder.build()).execute()) {
            int code = response.code();
            ResponseBody body = response.body();
            if (code == 200 || (successCodes != null && Arrays.asList(successCodes).contains(code))) {
                if (body == null) return null;
                else return JsonParser.parseString(body.string());
            } else {
                throw new HttpErrorException(code, null, "\nurl: " + url + "\ncode: "+code+" \nmessage: " + response.message() + "\njson: \n"
                        + (body != null ? body.string() : ""));
            }
        }
    }

    public JsonElement postJsonAndGetResponse(String url, JsonElement element, PayPalUtils context) throws IOException, HttpErrorException {
        return jsonFromUrl("POST", url, element, context, (Integer[]) null);
    }

    public JsonElement postJsonAndGetResponse(String url, JsonElement element, PayPalUtils context, Integer... successCodes) throws IOException, HttpErrorException {
        return jsonFromUrl("POST", url, element, context, successCodes);
    }

    public JsonElement patchJsonAndGetResponse(String url, JsonElement element, PayPalUtils context) throws IOException, HttpErrorException {
        return jsonFromUrl("PATCH", url, element, context, (Integer[]) null);
    }

    public JsonElement patchJsonAndGetResponse(String url, JsonElement element, PayPalUtils context, Integer... successCodes) throws IOException, HttpErrorException {
        return jsonFromUrl("PATCH", url, element, context, successCodes);
    }

    public JsonElement deleteAndGetResponse(String url, PayPalUtils context) throws IOException, HttpErrorException {
        return jsonFromUrl("DELETE", url, null, context, 204);
    }

    public JsonElement deleteAndGetResponse(String url, PayPalUtils context, Integer... successCodes) throws IOException, HttpErrorException {
        return jsonFromUrl("DELETE", url, null, context, successCodes);
    }

    /**
     * Returns the json-element. This can be a json-array or a json-object.
     *
     * @param input_url The url which leads to the json file.
     * @return JsonElement
     * @throws Exception When status code other than 200.
     */
    public JsonElement getJsonElement(String input_url, PayPalUtils context) throws IOException, HttpErrorException {
        return jsonFromUrl("GET", input_url, null, context, (Integer[]) null);
    }

    public JsonArray getJsonArray(String url, PayPalUtils context) throws IOException, HttpErrorException, WrongJsonTypeException {
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
    public List<JsonObject> getJsonArrayAsList(String url, PayPalUtils context) throws IOException, HttpErrorException, WrongJsonTypeException {
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
    public JsonObject getJsonObject(String url, PayPalUtils context) throws IOException, HttpErrorException, WrongJsonTypeException {
        JsonElement element = getJsonElement(url, context);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        } else {
            throw new WrongJsonTypeException("Its not a json object! Check it out -> " + url);
        }
    }
}
