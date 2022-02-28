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
     * The amount of money the user has to pay, in the smallest currency. Example: 100 = 1€. <br>
     * Note that this was already multiplied by the {@link #productQuantity}. <br>
     */
    public long amount;
    public String currency;
    public String productName;
    /**
     * The user can buy the same {@link Product} twice for example, then
     * the {@link #productQuantity} would be 2.
     */
    public int productQuantity;
    public String payUrl;

    // PayPal:
    public String paypalSubscriptionId; // TODO

    // Stripe specific stuff:
    // TODO

    // Information related to the status of the order:
    public Timestamp timestampCreated;
    public Timestamp timestampReceived;
    public Timestamp timestampRefunded;
    /**
     * The amount of money that was refunded, in the smallest currency. Example: 100 = 1€. <br>
     */
    public long amountRefunded;
    public Timestamp timestampCancelled;

    // Not stored inside the database:
    public PaymentProcessor paymentProcessor;
    private final List<Consumer<PaymentEvent>> actionsOnReceivedPayment = new ArrayList<>();


    public Payment(int paymentId, int productId, String userId, int productQuantity, long amount, String currency, String productName,
                   String payUrl, String paypalSubscriptionId,
                   Timestamp timestampCreated, Timestamp timestampReceived, Timestamp timestampRefunded, long amountRefunded, Timestamp timestampCancelled) {
        this.paymentId = paymentId;
        this.productId = productId;
        this.userId = userId;
        this.productQuantity = productQuantity;
        this.amount = amount;
        this.currency = currency;
        this.productName = productName;
        this.payUrl = payUrl;
        this.paypalSubscriptionId = paypalSubscriptionId;
        this.timestampCreated = timestampCreated;
        this.timestampReceived = timestampReceived;
        this.timestampRefunded = timestampRefunded;
        this.amountRefunded = amountRefunded;
        this.timestampCancelled = timestampCancelled;
        this.paymentProcessor = getPaymentProcessor();
    }

    private PaymentProcessor getPaymentProcessor(){
        if (isPayPalSupported()) return PaymentProcessor.PAYPAL;
        else if(isStripeSupported()) return PaymentProcessor.STRIPE;
        else return null;
    }

    /**
     * Returns true when {@link #timestampReceived} is null.
     */
    public boolean isPending(){
        return timestampReceived == null;
    }

    /**
     * Returns true when {@link #timestampRefunded} is not null.
     */
    public boolean isRefunded(){
        return timestampRefunded != null;
    }

    /**
     * Returns true when {@link #timestampCancelled} is not null.
     */
    public boolean isCancelled(){
        return timestampCancelled != null;
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
        return amount + " " + currency;
    }
}
