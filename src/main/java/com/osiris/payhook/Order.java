package com.osiris.payhook;

import com.google.gson.JsonObject;
import com.osiris.autoplug.core.json.exceptions.HttpErrorException;
import com.osiris.payhook.paypal.custom.UtilsPayPalJson;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Consumer;

public class Order {
    public int id;
    public String payUrl;
    public PaymentProcessor paymentProcessor;
    private Product[] products;

    // PayPal:
    public String paypalSubscriptionId; // TODO

    // Product related information:
    public long totalPriceInSmallestCurrency;
    public String currency;
    public int customBillingIntervallInDays;
    // Information related to the status of the order:
    public Timestamp creationTimestamp;
    public Timestamp lastPaymentTimestamp;
    public Timestamp refundTimestamp;
    public Timestamp cancelTimestamp;

    private final List<Consumer<Event>> actionsOnReceivedPayment = new ArrayList<>();

    /**
     * Make sure that the {@link #totalPriceInSmallestCurrency} matches the total price of all passed over {@link #products}.
     */
    public Order(int id, String payUrl, long totalPriceInSmallestCurrency,
                 String currency, int customBillingIntervallInDays, Timestamp creationTimestamp, Timestamp lastPaymentTimestamp,
                 Timestamp refundTimestamp, Timestamp cancelTimestamp, Product[] products) {
        this.id = id;
        this.payUrl = payUrl;
        this.totalPriceInSmallestCurrency = totalPriceInSmallestCurrency;
        this.currency = currency;
        this.customBillingIntervallInDays = customBillingIntervallInDays;
        this.creationTimestamp = creationTimestamp;
        this.lastPaymentTimestamp = lastPaymentTimestamp;
        this.refundTimestamp = refundTimestamp;
        this.cancelTimestamp = cancelTimestamp;
        this.products = products;
    }

    public Date getLastPaymentDate() throws IOException, HttpErrorException, ParseException {
        if (paymentProcessor.equals(PaymentProcessor.PAYPAL)){
            JsonObject obj = new UtilsPayPalJson().postJsonAndGetResponse(
                            BASE_URL + "/billing/subscriptions/" + order.getSubscriptionPaypalId(), null, this)
                    .getAsJsonObject();
            String timestamp = obj.getAsJsonObject("billing_info").getAsJsonObject("last_payment").get("time").getAsString();
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sf.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
            return sf.parse(timestamp);
        } else if(paymentProcessor.equals(PaymentProcessor.STRIPE)){
            //TODO
            return null;
        } else
            return null;
    }

    public void onPaymentReceived(Consumer<Event> action) {
        synchronized (actionsOnReceivedPayment){
            actionsOnReceivedPayment.add(action);
        }
    }

    public void executePaymentReceived(){
        synchronized (actionsOnReceivedPayment){
            for (Consumer<Event> action :
                    actionsOnReceivedPayment) {
                action.accept(new Event(this));
            }
            actionsOnReceivedPayment.clear();
            // TODO LINK THIS WITH ACTUAL WEBHOOK EVENTS
        }
    }
}
