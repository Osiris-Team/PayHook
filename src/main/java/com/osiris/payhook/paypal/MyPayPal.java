/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.payhook.paypal;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.osiris.autoplug.core.json.exceptions.HttpErrorException;
import com.osiris.autoplug.core.json.exceptions.WrongJsonTypeException;
import com.osiris.payhook.Product;
import com.osiris.payhook.paypal.codec.binary.Base64;
import com.paypal.api.payments.Currency;
import com.paypal.base.rest.PayPalRESTException;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.Order;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.OrdersCaptureRequest;
import com.paypal.payments.*;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

/**
 * PayPals' Java SDKs don't cover the complete REST API. <br>
 * This class aims to close those gaps. <br>
 */
public class MyPayPal {
    public static String BASE_V1_URL;
    public static String LIVE_V1_SANDBOX_BASE_URL = "https://api-m.sandbox.paypal.com/v1";
    public static String LIVE_V1_LIVE_BASE_URL = "https://api-m.paypal.com/v1";
    private final String clientId;
    private final String clientSecret;
    private final Mode mode;
    private String credBase64 = "";
    private UtilsPayPal utils = new UtilsPayPal();
    private UtilsPayPalJson utilsJson = new UtilsPayPalJson();

    public MyPayPal(String clientId, String clientSecret, Mode mode) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        credBase64 = Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes());
        this.mode = mode;
        if (mode == Mode.LIVE)
            BASE_V1_URL = LIVE_V1_LIVE_BASE_URL;
        else
            BASE_V1_URL = LIVE_V1_SANDBOX_BASE_URL;
    }

    public PayPalPlan getPlanById(String planId) throws WrongJsonTypeException, IOException, HttpErrorException {
        JsonObject obj = new UtilsPayPalJson().getJsonObject(BASE_V1_URL + "/billing/plans/" + planId, this);
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
     * Creates a new product at PayPal with the provided {@link Product}s details.
     */
    public JsonObject createProduct(Product product) throws IOException, HttpErrorException {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", product.name);
        obj.addProperty("description", product.description);
        if (product.isRecurring())
            obj.addProperty("type", "SERVICE");
        else
            obj.addProperty("type", "DIGITAL"); // TODO do we really need to differ between digital/physical goods?
        return utilsJson.postJsonAndGetResponse(BASE_V1_URL +"/catalogs/products", obj, this).getAsJsonObject();
    }

    /**
     * Updates the product at PayPal with the provided {@link Product}s details.
     * @throws NullPointerException when {@link Product#paypalProductId} is null.
     */
    public MyPayPal updateProduct(Product product) throws IOException, HttpErrorException {
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
        utilsJson.patchJsonAndGetResponse(BASE_V1_URL +"/catalogs/products/"+product.paypalProductId, arr, this);
        return this;
    }

    /**
     * Returns a string array like this: <br>
     * [subscriptionId, approveUrl]
     */
    public String[] createSubscription(String brandName, String planId, String returnUrl, String cancelUrl) throws WrongJsonTypeException, IOException, HttpErrorException, PayPalRESTException {
        JsonObject obj = new JsonObject();
        obj.addProperty("plan_id", planId);

        JsonObject applicationContext = new JsonObject();
        obj.add("application_context", applicationContext);
        applicationContext.addProperty("brand_name", brandName);
        applicationContext.addProperty("locale", "en-US");
        applicationContext.addProperty("shipping_preference", "NO_SHIPPING");
        applicationContext.addProperty("user_action", "SUBSCRIBE_NOW");

        JsonObject paymentMethod = new JsonObject();
        applicationContext.add("payment_method", paymentMethod);
        paymentMethod.addProperty("payer_selected", "PAYPAL");
        paymentMethod.addProperty("payee_preferred", "IMMEDIATE_PAYMENT_REQUIRED");

        applicationContext.addProperty("return_url", returnUrl);
        applicationContext.addProperty("cancel_url", cancelUrl);


        JsonObject resultObj = utilsJson.postJsonAndGetResponse(BASE_V1_URL + "/billing/subscriptions", obj, this, 201)
                .getAsJsonObject();

        String approveUrl = null;
        String editUrl = null;
        String selfUrl = null;
        for (JsonElement element :
                resultObj.get("links").getAsJsonArray()) {
            if (element.getAsJsonObject().get("rel").getAsString().equals("approve"))
                approveUrl = element.getAsJsonObject().get("href").getAsString();
            if (element.getAsJsonObject().get("rel").getAsString().equals("edit"))
                editUrl = element.getAsJsonObject().get("href").getAsString();
            if (element.getAsJsonObject().get("rel").getAsString().equals("self"))
                selfUrl = element.getAsJsonObject().get("href").getAsString();
            else
                throw new PayPalRESTException("Couldn't determine url type: " + element.getAsJsonObject().get("rel").getAsString());
        }
        return new String[]{resultObj.get("id").getAsString(), approveUrl};
    }

    /**
     * Can only be done in the first 14 days, thus the first transaction is fetched and a refund for that is tried.
     */
    public MyPayPal refundSubscription(PayPalHttpClient client, String subscriptionId, long subscriptionStart,
                                       Money amount, String note) throws IOException, HttpErrorException {
        JsonArray transactions = getSubscriptionTransactions(subscriptionId, subscriptionStart);
        refundPayment(client, transactions.get(0).getAsJsonObject().get("id").getAsString(), // transactionId
                amount, note);
        return this;
    }

    public MyPayPal cancelSubscription(String paypalSubscriptionId) throws IOException, HttpErrorException {
        JsonObject obj = new JsonObject();
        obj.addProperty("reason", "No reason provided.");
        utilsJson.postJsonAndGetResponse(BASE_V1_URL + "/billing/subscriptions/" + paypalSubscriptionId + "/cancel",
                obj, this, 204);
        return this;
    }

    public HttpResponse<Refund> refundPayment(PayPalHttpClient client, String captureOrTransactionId, Money amount, String note) throws IOException {
        Objects.requireNonNull(client);
        Objects.requireNonNull(captureOrTransactionId);
        Objects.requireNonNull(amount);
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.amount(amount);
        refundRequest.noteToPayer(note);
        CapturesRefundRequest request = new CapturesRefundRequest(captureOrTransactionId);
        request.prefer("return=representation");
        request.requestBody(refundRequest);
        return client.execute(request);
    }

    /**
     * Since the subscription id is not directly returned in
     * webhook event (for example when a payment is made on a subscription), this method can be pretty useful.
     * @return subscription id.
     * @throws Exception if provided transaction id is not from a subscription.
     */
    public String findSubscriptionId(String transactionId) throws Exception {
        String subscriptionId = null;
        JsonArray arr = getTransactionsLast30Days(transactionId);
        for (JsonElement el : arr) {
            JsonObject transactionInfo = el.getAsJsonObject().getAsJsonObject("transaction_info");
            if(transactionInfo.get("paypal_reference_id_type") != null && transactionInfo.get("paypal_reference_id_type").getAsString().equals("SUB")){
                subscriptionId = transactionInfo.get("paypal_reference_id").getAsString();
                break;
            }
        }
        if(subscriptionId==null) throw new Exception("Failed to find subscriptionId in transactions of last 2 days! Json: "+ new Gson().toJson(arr));
        return subscriptionId;
    }

    public JsonArray getTransactionsLast30Days(String transactionId) throws WrongJsonTypeException, IOException, HttpErrorException {
        Date endTime = new Date(System.currentTimeMillis() + (42 * 3600000)); // Plus 42h to make sure there is no UTC local time interfering
        Date startTime = new Date(System.currentTimeMillis() - (30L * 24 * 3600000)); // 30 days as milliseconds
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'-0000'";
        DateFormat df = new SimpleDateFormat(pattern);
        String formattedEndTime = df.format(endTime);
        String formattedStartTime = df.format(startTime);
        return utilsJson.getJsonObject(BASE_V1_URL + "/reporting/transactions" +
                        "?start_date=" + formattedStartTime+
                        "&end_date=" + formattedEndTime+
                        "&transaction_id=" + transactionId+
                        "&fields=all" +
                        "&page_size=100" +
                        "&page=1", this)
                .getAsJsonObject().getAsJsonArray("transaction_details");
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

    public HttpResponse<Order> captureOrder(PayPalHttpClient client, String orderId) throws Exception {
        OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
        OrderRequest orderRequest = new OrderRequest();
        request.requestBody(orderRequest);
        HttpResponse<Order> response = client.execute(request);
        if (response.statusCode() != 201) {
            throw new Exception("Error-Code: " + response.statusCode() + " Status-Message: " + response.result().status());
        }
        return response;
    }

    public HttpResponse<Capture> capturePayment(PayPalHttpClient client, String paymentId) throws Exception {
        AuthorizationsCaptureRequest request = new AuthorizationsCaptureRequest(paymentId);
        CaptureRequest details = new CaptureRequest();
        request.requestBody(details);
        HttpResponse<Capture> response = client.execute(request);
        if (response.statusCode() != 201) {
            throw new Exception("Error-Code: " + response.statusCode() + " Status-Message: " + response.result().status());
        }
        return response;
    }

    /**
     * @return the transaction-id, aka capture-id.
     * @throws Exception if something went wrong with the API request, or if the returned
     * http status code is not 200/201, or if the currency code and paid balance don't
     * match the expected amount.
     */
    public String captureOrder(PayPalHttpClient paypalV2, String orderId, Currency outstandingBalance) throws Exception {
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(outstandingBalance);

        Order order = null;
        OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
        HttpResponse<com.paypal.orders.Order> response = paypalV2.execute(request);
        order = response.result();

        String currencyCode = order.purchaseUnits().get(0).payments().captures().get(0).amount().currencyCode();
        String paidBalance = order.purchaseUnits().get(0).payments().captures().get(0).amount().value();
        if(!currencyCode.equals(outstandingBalance.getCurrency()))
            throw new Exception("Expected '"+outstandingBalance.getCurrency()+"' currency code, but got '"+currencyCode+"' in the capture!");
        if(!paidBalance.equals(outstandingBalance.getValue()))
            throw new Exception("Expected '"+outstandingBalance.getValue()+"' paid balance, but got '"+paidBalance+"' in the capture!");

        return order.purchaseUnits().get(0).payments().captures().get(0).id();
    }

    public JsonElement getSubscriptionDetails(String subscriptionId) throws IOException, HttpErrorException {
        JsonObject response = utilsJson
                .getJsonElement(BASE_V1_URL + "/billing/subscriptions/" + subscriptionId, this)
                .getAsJsonObject();
        return response;
    }

    public JsonArray getSubscriptionTransactions(String subscriptionId, long subscriptionStart) throws IOException, HttpErrorException {
        Date endTime = new Date(System.currentTimeMillis());
        Date startTime = new Date(subscriptionStart - (24 * 3600000)); // Minus 24h to make sure there is no UTC local time interfering
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'.000Z'";
        DateFormat df = new SimpleDateFormat(pattern);
        String formattedEndTime = df.format(endTime);
        String formattedStartTime = df.format(startTime);
        JsonElement response = utilsJson
                .getJsonElement(BASE_V1_URL + "/billing/subscriptions/" + subscriptionId
                        + "/transactions?start_time=" + formattedStartTime + "&end_time=" + formattedEndTime, this);
        return response.getAsJsonObject().get("transactions").getAsJsonArray();
    }

    public Date getLastPaymentDate(String subscriptionId) throws IOException, HttpErrorException, ParseException {
        JsonObject obj = new UtilsPayPalJson().postJsonAndGetResponse(
                        BASE_V1_URL + "/billing/subscriptions/" + subscriptionId, null, this)
                .getAsJsonObject();
        String timestamp = obj.getAsJsonObject("billing_info").getAsJsonObject("last_payment").get("time").getAsString();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sf.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        return sf.parse(timestamp);
    }

    public JsonArray getWebhooks() throws IOException, HttpErrorException {
        return utilsJson.postJsonAndGetResponse(
                        BASE_V1_URL + "/notifications/webhooks", null, this)
                .getAsJsonObject().getAsJsonArray("webhooks");
    }

    public MyPayPal createWebhook(String webhookUrl, List<String> eventTypes) throws IOException, HttpErrorException{
        return createWebhook(webhookUrl, eventTypes.toArray(new String[0]));
    }
    public MyPayPal createWebhook(String webhookUrl, String... eventTypes) throws IOException, HttpErrorException {
        JsonObject obj = new JsonObject();
        obj.addProperty("url", webhookUrl);
        JsonArray arr = new JsonArray();
        for (String eventType :
                eventTypes) {
            JsonObject o = new JsonObject();
            o.addProperty("name", eventType);
            arr.add(o);
        }
        obj.add("event_types", arr);
        utilsJson.postJsonAndGetResponse(
                        BASE_V1_URL + "/notifications/webhooks", obj, this);
        return this;
    }

    public enum Mode {
        LIVE, SANDBOX
    }
}
