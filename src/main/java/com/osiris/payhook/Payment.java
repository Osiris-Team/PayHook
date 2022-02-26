package com.osiris.payhook;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Payment {
    public final int paymentId;
    /**
     * {@link Product#productId}
     */
    public final int productId;
    /**
     * Unique identifier provided by you, to assign this {@link Payment} to the buying user.
     */
    public final String userId;
    /**
     * The user can buy the same {@link Product} twice for example, then
     * the {@link #quantity} would be 2.
     */
    public int quantity;
    /**
     * The price in the smallest currency. Example: 100 = 1€. <br>
     * Note that this price was already multiplied by the amount of the {@link Product}. <br>
     */
    public long price;
    public String currency;
    public boolean isPending;
    public String productName;
    public String payUrl;

    // PayPal:
    public String paypalSubscriptionId; // TODO

    // Stripe specific stuff:
    // TODO

    // Information related to the status of the order:
    public Timestamp creationTimestamp;
    public Timestamp lastPaymentTimestamp;
    public Timestamp refundTimestamp;
    public Timestamp cancelTimestamp;

    // Not stored inside the database:
    public PaymentProcessor paymentProcessor;
    private final List<Consumer<PaymentEvent>> actionsOnReceivedPayment = new ArrayList<>();


    public Payment(int paymentId, int productId, String userId, int quantity, long price, String currency, boolean isPending, String productName,
                   String payUrl, String paypalSubscriptionId,
                   Timestamp creationTimestamp, Timestamp lastPaymentTimestamp, Timestamp refundTimestamp, Timestamp cancelTimestamp) {
        this.paymentId = paymentId;
        this.productId = productId;
        this.userId = userId;
        this.quantity = quantity;
        this.price = price;
        this.currency = currency;
        this.isPending = isPending;
        this.productName = productName;
        this.payUrl = payUrl;
        this.paypalSubscriptionId = paypalSubscriptionId;
        this.creationTimestamp = creationTimestamp;
        this.lastPaymentTimestamp = lastPaymentTimestamp;
        this.refundTimestamp = refundTimestamp;
        this.cancelTimestamp = cancelTimestamp;
        this.paymentProcessor = getPaymentProcessor();
    }

    private PaymentProcessor getPaymentProcessor(){
        if (isPayPalSupported()) return PaymentProcessor.PAYPAL;
        else if(isStripeSupported()) return PaymentProcessor.STRIPE;
        else return null;
    }

    /**
     * Returns true when this product has all the necessary PayPal related information available.
     */
    public boolean isPayPalSupported(){
        return paypalSubscriptionId != null;
    }

    /**
     * Returns true when this product has all the necessary Stripe related information available.
     */
    public boolean isStripeSupported(){
        return stripeProductId != null && stripePriceId != null;
    }

    public String getFormattedPrice() {
        return price + " " + currency;
    }
}
