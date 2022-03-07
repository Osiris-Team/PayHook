package com.osiris.payhook;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a received or sent payment. <br>
 */
public class Payment {
    public final int id;
    /**
     * Unique identifier provided by you, to assign this {@link Payment} to the buying user.
     */
    @NotNull public final String userId;
    /**
     * The amount of money in the smallest currency. Example: 100 = 1€. <br>
     * Negative amount if the money was sent (for example in a refund), positive when received. <br>
     */
    public long amount;
    /**
     * The <a href="https://de.wikipedia.org/wiki/ISO_4217">ISO_4217</a> currency code.
     */
    @NotNull public String currency;
    /**
     * Redirect the user to this url, to approve/complete the {@link Payment}.
     */
    @Nullable public String url;
    @Nullable public Timestamp timestampCreated;
    /**
     * Gets set once the {@link Payment} was done and received.
     */
    @Nullable public Timestamp timestampPaid;

    /**
     * If this payment was related to a {@link Product} returns its id, otherwise -1.
     */
    public final int productId;
    /**
     * If this payment was related to a {@link Product} returns its name, otherwise null.
     */
    @Nullable public String productName;
    /**
     * The user can buy the same {@link Product} twice for example, then
     * the {@link #productQuantity} would be 2.
     */
    public int productQuantity;

    // Stripe specific stuff:
    @Nullable public String stripePaymentIntentId; // TODO add to constructor
    @Nullable public String stripeSubscriptionId; // TODO add to constructor

    // Braintree specific stuff:
    @Nullable public String braintreeOrderId;
    @Nullable public String braintreeSubscriptionId; // TODO
    @Nullable public String braintreeCaptureId; // TODO

    // Not stored inside the database:
    public PaymentProcessor paymentProcessor;
    private final List<Consumer<PaymentEvent>> actionsOnReceivedPayment = new ArrayList<>();


    public Payment(int id, @NotNull String userId, long amount, @NotNull String currency, @Nullable String url, @Nullable Timestamp timestampCreated, @Nullable Timestamp timestampPaid,
                   int productId, @Nullable String productName, int productQuantity, @Nullable String stripePaymentIntentId, @Nullable String stripeSubscriptionId) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.url = url;
        this.timestampCreated = timestampCreated;
        this.timestampPaid = timestampPaid;
        this.productId = productId;
        this.productName = productName;
        this.productQuantity = productQuantity;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.paymentProcessor = getPaymentProcessor();
    }

    private PaymentProcessor getPaymentProcessor(){
        if (isBraintreeSupported()) return PaymentProcessor.BRAINTREE;
        else if(isStripeSupported()) return PaymentProcessor.STRIPE;
        else return null;
    }

    /**
     * Returns true when {@link #timestampPaid} is null.
     */
    public boolean isPending(){
        return timestampPaid == null;
    }

    /**
     * Returns true if a subscription id is provided.
     */
    public boolean isRecurring(){
        return braintreeSubscriptionId != null || stripeSubscriptionId != null;
    }

    /**
     * Returns true when this has all the necessary Braintree specific information available.
     */
    public boolean isBraintreeSupported(){
        return braintreeSubscriptionId != null; // TODO
    }

    /**
     * Returns true when this has all the necessary Stripe specific information available.
     */
    public boolean isStripeSupported(){
        return stripePaymentIntentId != null && stripeSubscriptionId != null;
    }

}
