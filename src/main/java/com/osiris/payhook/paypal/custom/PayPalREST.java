/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.payhook.paypal.custom;


import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.osiris.autoplug.core.json.exceptions.HttpErrorException;
import com.osiris.autoplug.core.json.exceptions.WrongJsonTypeException;
import com.osiris.autoplug.core.logger.AL;
import com.osiris.autoplug.webserver.database.queries.QueriesUser;
import com.osiris.autoplug.webserver.objects.UserOrder;
import com.osiris.autoplug.webserver.payment.paypal.PayPalRefund;
import com.osiris.payhook.Product;
import com.osiris.payhook.paypal.codec.binary.Base64;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

/**
 * PayPals' Java SDKs don't cover the complete REST API. <br>
 * This class aims to close those gaps. <br>
 */
public class PayPalREST {
    public static String BASE_URL;
    public static String LIVE_V1_SANDBOX_BASE_URL = "https://api-m.sandbox.paypal.com/v1";
    public static String LIVE_V1_LIVE_BASE_URL = "https://api-m.paypal.com/v1";
    private final String clientId;
    private final String clientSecret;
    private final Mode mode;
    private String credBase64 = "";
    private UtilsPayPal utils = new UtilsPayPal();
    private UtilsPayPalJson utilsJson = new UtilsPayPalJson();

    public PayPalREST(String clientId, String clientSecret, Mode mode) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        credBase64 = Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes());
        this.mode = mode;
        if (mode == Mode.LIVE)
            BASE_URL = LIVE_V1_LIVE_BASE_URL;
        else
            BASE_URL = LIVE_V1_SANDBOX_BASE_URL;
    }

    public PayPalPlan getPlanById(String planId) throws WrongJsonTypeException, IOException, HttpErrorException {
        JsonObject obj = new UtilsPayPalJson().getJsonObject(BASE_URL + "/billing/plans/" + planId, this);
        String desc = "";
        if (obj.get("description") != null)
            desc = obj.get("description").getAsString();
        return new PayPalPlan(
                this,
                planId,
                obj.get("product_id").getAsString(),
                obj.get("name").getAsString(),
                desc,
                utils.getPlanStatus(obj.get("status").getAsString()));
    }

    /**
     * Creates a new product at PayPal with the provided {@link Product}s details <br>
     * and even sets the {@link Product#productId}. <br>
     */
    public PayPalREST createProduct(Product product) throws IOException, HttpErrorException {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", product.name);
        obj.addProperty("description", product.description);
        if (product.isRecurring())
            obj.addProperty("type", "SERVICE");
        else
            obj.addProperty("type", "DIGITAL"); // TODO do we really need to differ between digital/physical goods?
        product.paypalProductId = utilsJson.postJsonAndGetResponse(BASE_URL+"/catalogs/products", obj, this)
                .getAsJsonObject().get("id").getAsString();
        return this;
    }

    /**
     * Updates the product at PayPal with the provided {@link Product}s details.
     * @throws NullPointerException when {@link Product#paypalProductId} is null.
     */
    public PayPalREST updateProduct(Product product) throws IOException, HttpErrorException {
        Objects.requireNonNull(product.paypalProductId);
        JsonArray arr = new JsonArray();
        JsonObject patchName = new JsonObject();
        patchName.addProperty("op", "replace");
        patchName.addProperty("path", "/name");
        patchName.addProperty("value", product.name);
        arr.add(patchName);
        JsonObject patchDesc = new JsonObject();
        patchDesc.addProperty("op", "replace");
        patchDesc.addProperty("path", "/description");
        patchDesc.addProperty("value", product.description);
        arr.add(patchDesc);
        utilsJson.patchJsonAndGetResponse(BASE_URL+"/catalogs/products/"+product.paypalProductId, arr, this);
        return this;
    }

    /**
     * Can only be done in the first 14 days, thus the first transaction is fetched and a refund for that is tried.
     */
    public PayPalREST refundSubscription(UserOrder order) throws IOException, HttpErrorException {
        AL.debug(this.getClass(), "Refunding subscription: " + order.getSubscriptionPaypalId());
        JsonArray transactions = getSubscriptionTransactions(order);
        refundPayment(transactions.get(0).getAsJsonObject().get("id").getAsString()); // transactionId
        cancelSubscription(order);
        return this;
    }

    public PayPalREST cancelSubscription(Product product) throws IOException, HttpErrorException {
        JsonObject obj = new JsonObject();
        obj.addProperty("reason", "No reason provided.");
        utilsJson.postJsonAndGetResponse(BASE_URL + "/billing/subscriptions/" + product.paypalSubscriptionId + "/cancel", obj, this, 204);
        order.setSubscriptionStop(new Timestamp(System.currentTimeMillis()));
        new QueriesUser().updateOrder(order);
        return this;
    }


    public PayPalREST refundPayment(String captureOrTransactionId) throws IOException {
        AL.debug(this.getClass(), "Refunding payment with capture/transaction id: " + captureOrTransactionId);
        new PayPalRefund().refundOrder(captureOrTransactionId);
        return this;
    }

    /**
     * Client id and secret separated by : encoded with Base64.
     */
    public String getCredBase64() {
        return credBase64;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public Mode getMode() {
        return mode;
    }

    /**
     * Returns the transaction-id.
     *
     * @param order
     * @return
     * @throws Exception
     */
    public JsonObject captureSubscription(UserOrder order, String outstandingBalance) throws Exception {
        AL.debug(this.getClass(), "Capturing subscription: " + order.getSubscriptionPaypalId());
        JsonObject obj = new JsonObject();
        obj.addProperty("note", "Capturing payment on subscription.");
        obj.addProperty("capture_type", "OUTSTANDING_BALANCE");
        JsonObject amount = new JsonObject();
        amount.addProperty("currency_code", "EUR");
        amount.addProperty("value", outstandingBalance);
        obj.add("amount", amount);

        JsonObject response = utilsJson
                .postJsonAndGetResponse(BASE_URL + "/billing/subscriptions/" + order.getSubscriptionPaypalId() + "/capture",
                        obj, this, 202)
                .getAsJsonObject();
        return response;
    }

    public JsonElement getSubscriptionDetails(UserOrder order) throws IOException, HttpErrorException {
        AL.debug(this.getClass(), "Getting subscription details: " + order.getSubscriptionPaypalId());
        JsonObject response = utilsJson
                .getJsonElement(BASE_URL + "/billing/subscriptions/" + order.getSubscriptionPaypalId(), this)
                .getAsJsonObject();
        return response;
    }

    public JsonArray getSubscriptionTransactions(UserOrder order) throws IOException, HttpErrorException {
        AL.debug(this.getClass(), "Getting subscription transactions details: " + order.getSubscriptionPaypalId());
        Date endTime = new Date(System.currentTimeMillis());
        Date startTime = new Date(order.getSubscriptionStart().getTime() - (24 * 3600000)); // Minus 24h to make sure there is no UTC local time interfering
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'.000Z'";
        DateFormat df = new SimpleDateFormat(pattern);
        String formattedEndTime = df.format(endTime);
        String formattedStartTime = df.format(startTime);
        JsonElement response = utilsJson
                .getJsonElement(BASE_URL + "/billing/subscriptions/" + order.getSubscriptionPaypalId()
                        + "/transactions?start_time=" + formattedStartTime + "&end_time=" + formattedEndTime, this);
        AL.debug(this.getClass(), "RESPONSE: " + new GsonBuilder().setPrettyPrinting().create().toJson(response));
        return response.getAsJsonObject().get("transactions").getAsJsonArray();
    }

    public Date getLastPaymentDate(String subscriptionId) throws IOException, HttpErrorException, ParseException {
        JsonObject obj = new UtilsPayPalJson().postJsonAndGetResponse(
                        BASE_URL + "/billing/subscriptions/" + subscriptionId, null, this)
                .getAsJsonObject();
        String timestamp = obj.getAsJsonObject("billing_info").getAsJsonObject("last_payment").get("time").getAsString();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sf.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        return sf.parse(timestamp);
    }


    public enum Mode {
        LIVE, SANDBOX
    }
}
