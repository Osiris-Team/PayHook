package com.osiris.payhook;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;

/**
 * Represents a received or sent payment. <br>
 */
public class Payment {
    public final int id;
    /**
     * Unique identifier provided by you, to assign this {@link Payment} to the buying user/entity.
     */
    @NotNull public final String buyerId;
    /**
     * Unique identifier provided by you, to assign this {@link Payment} to the selling user/entity.
     */
    @NotNull public final String sellerId;
    /**
     * The amount of money in the smallest currency. Example: 100 = 1€. <br>
     * Negative amount if the money was sent (for example in a refund), positive when received. <br>
     */
    public long charge;
    /**
     * The <a href="https://de.wikipedia.org/wiki/ISO_4217">ISO_4217</a> currency code.
     */
    @NotNull public String currency;
    /**
     * Redirect the user to this url, to authorize/complete the {@link Payment}.
     */
    @Nullable public String url;
    @NotNull public Type type;
    @NotNull public State state;
    /**
     * Gets set at {@link Payment} creation.
     * @see PayHook#paymentCreatedEvent
     */
    @Nullable public Timestamp timestampCreated;
    /**
     * Gets set at {@link Payment} creation or at authorization.
     * The {@link Payment#url} is only valid for a certain time period, which
     * can be different for each {@link PaymentProcessor}.
     * This timestamp is calculated by adding that maximum time period to the current time. <br>
     * If this is a recurring payment and not the first payment, the timestamp is calculated by adding the {@link Payment.Type} time
     * to the {@link #timestampAuthorized} time.
     * TODO how to know if this is the first payment? By checking if there is a payment with the same subscription id already in the db.
     */
    @Nullable public Timestamp timestampExpires;
    /**
     * Gets set once this {@link Payment} was completed/authorized.
     * @see PayHook#paymentAuthorizedEvent
     */
    @Nullable public Timestamp timestampAuthorized;
    /**
     * Gets set once this {@link Payment} was cancelled.
     * @see PayHook#paymentCancelledEvent
     */
    @Nullable public Timestamp timestampCancelled;


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
    @Nullable public String stripePaymentIntentId;
    @Nullable public String stripeSubscriptionId;

    // PayPal specific stuff:
    @Nullable public String paypalOrderId;
    @Nullable public String paypalSubscriptionId;
    @Nullable public String paypalCaptureId;

    // Not stored inside the database:
    public PaymentProcessor paymentProcessor;


    public Payment(int id, @NotNull String buyerId, @NotNull String sellerId, long charge, @NotNull String currency, @Nullable String url, @Nullable Timestamp timestampCreated, @Nullable Timestamp timestampAuthorized,
                   @NotNull Type type, int productId, @Nullable String productName, int productQuantity, @Nullable String stripePaymentIntentId, @Nullable String stripeSubscriptionId) {
        this.id = id;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.charge = charge;
        this.currency = currency;
        this.url = url;
        this.timestampCreated = timestampCreated;
        this.timestampAuthorized = timestampAuthorized;
        this.type = type;
        this.productId = productId;
        this.productName = productName;
        this.productQuantity = productQuantity;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.paymentProcessor = getPaymentProcessor();
        this.state = getState();
    }

    private State getState(){
        State state = State.CREATED;
        if(timestampAuthorized!=null) state = State.AUTHORIZED;
        if(timestampCancelled!=null) state = State.CANCELLED;
        return state;
    }

    private PaymentProcessor getPaymentProcessor(){
        if (isPayPalSupported()) return PaymentProcessor.BRAINTREE;
        else if(isStripeSupported()) return PaymentProcessor.STRIPE;
        else return null;
    }

    /**
     * Returns true when {@link #timestampAuthorized} is null.
     */
    public boolean isPending(){
        return timestampAuthorized == null;
    }

    /**
     * Returns true if a subscription id is provided.
     */
    public boolean isRecurring(){
        return paypalSubscriptionId != null || stripeSubscriptionId != null;
    }

    /**
     * Returns true when this has all the necessary Braintree specific information available.
     */
    public boolean isPayPalSupported(){
        return paypalSubscriptionId != null; // TODO
    }

    /**
     * Returns true when this has all the necessary Stripe specific information available.
     */
    public boolean isStripeSupported(){
        return stripePaymentIntentId != null && stripeSubscriptionId != null;
    }

    public enum State{
        /**
         * The initial (default) state of a payment directly after creating it.
         * @see PayHook#createPayment(String, String, Product, PaymentProcessor, String, String)
         */
        CREATED(0),
        /**
         * When the user visited the {@link Payment#url}
         * and authorized/completed the payment.
         * @see PayHook#paymentAuthorizedEvent
         */
        AUTHORIZED(1),
        /**
         * When a payment was created/expected, but
         * the user didn't complete/authorize it in time.
         * @see PayHook#paymentCancelledEvent
         */
        CANCELLED(2);

        public final int state;
        State(int state){
            this.state = state;
        }
    }

    public enum Type {
        /**
         * One time payment. Type 0.
         */
        ONE_TIME(0),
        /**
         * Recurring payment every month (exactly 30 days). Type 1.
         */
        RECURRING(1),
        /**
         * Recurring payment every 3 months (exactly 90 days). Type 2.
         */
        RECURRING_3(2),
        /**
         * Recurring payment every 6 months (exactly 180 days). Type 3.
         */
        RECURRING_6(3),
        /**
         * Recurring payment every 12 months (exactly 360 days). Type 4.
         */
        RECURRING_12(4),
        /**
         * Recurring payment with a custom intervall. Type 5.
         */
        RECURRING_CUSTOM(5);

        public final int type;
        Type(int type){
            this.type = type;
        }
        public long toDays(){
            if (type == 1) return 30;
            else if(type == 2) return 90;
            else if(type == 3) return 180;
            else if(type == 4) return 360;
            return 0;
        }
        public long toHours() {
            return toDays() * 24;
        }
        public long toMilliseconds(){
            return toHours() * 3600000;
        }
    }
}
