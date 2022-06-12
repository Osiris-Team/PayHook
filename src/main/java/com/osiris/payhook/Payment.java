package com.osiris.payhook;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

/**
 * Represents a received or sent payment. <br>
 */
public class Payment implements Serializable {
    public final int id;
    /**
     * The entity that sends the money for this payment (usually the customer, buying your product). <br>
     * Unique identifier provided by you, to be able to assign this payment to an entity/user. <br>
     */
    @NotNull
    public final String userId;
    /**
     * If this payment was related to a {@link Product} returns its id, otherwise -1.
     */
    public final int productId;
    /**
     * The amount of money in the smallest currency. Example: 100 = 1€. <br>
     * Negative amount if the money was sent (for example in a refund), positive when received. <br>
     */
    public long charge;
    /**
     * The <a href="https://de.wikipedia.org/wiki/ISO_4217">ISO_4217</a> currency code.
     */
    @NotNull
    public String currency;
    /**
     * Redirect the user to this url, to authorize/complete the {@link Payment}.
     */
    @Nullable
    public String url;
    @NotNull
    public Payment.Intervall intervall;
    @NotNull
    public State state;
    /**
     * Gets set at {@link Payment} creation.
     *
     * @see PayHook#paymentCreatedEvent
     */
    @Nullable
    public Timestamp timestampCreated;
    /**
     * Gets set at {@link Payment} creation or at authorization.
     * The {@link Payment#url} is only valid for a certain time period, which
     * can be different for each {@link PaymentProcessor}.
     * This timestamp is calculated by adding that maximum time period to the current time. <br>
     * If this is a recurring payment and not the first payment, the timestamp is calculated by adding the {@link Intervall} time
     * to the {@link #timestampAuthorized} time.
     * TODO how to know if this is the first payment? By checking if there is a payment with the same subscription id already in the db.
     */
    @Nullable
    public Timestamp timestampExpires;
    /**
     * Gets set once this {@link Payment} was completed/authorized.
     *
     * @see PayHook#paymentAuthorizedEvent
     */
    @Nullable
    public Timestamp timestampAuthorized;
    /**
     * Gets set once this {@link Payment} was cancelled.
     *
     * @see PayHook#paymentCancelledEvent
     */
    @Nullable
    public Timestamp timestampCancelled;
    /**
     * If this payment was related to a {@link Product} returns its name, otherwise null.
     */
    @Nullable
    public String productName;
    /**
     * The user can buy the same {@link Product} twice for example, then
     * the {@link #productQuantity} would be 2.
     */
    public int productQuantity;

    // Stripe specific stuff:
    @Nullable
    public String stripePaymentIntentId; // TODO set on webhook event and in constructor
    @Nullable
    public String stripeSubscriptionId; // TODO set on webhook event and in constructor
    @Nullable
    public String stripeChargeId; // TODO set on webhook event and in constructor

    // PayPal specific stuff:
    @Nullable
    public String paypalOrderId; // TODO set on webhook event and in constructor
    @Nullable
    public String paypalSubscriptionId; // TODO set on webhook event and in constructor
    @Nullable
    public String paypalCaptureId; // TODO set on webhook event and in constructor

    // Not stored inside the database:
    public PaymentProcessor paymentProcessor;



    public Payment(int id, @NotNull String userId, long charge, @NotNull String currency, @Nullable String url, @Nullable Timestamp timestampCreated,
                   @Nullable Timestamp timestampExpires, @Nullable Timestamp timestampAuthorized, @Nullable Timestamp timestampCancelled,
                   @NotNull Payment.Intervall intervall, int productId, @Nullable String productName, int productQuantity, @Nullable String stripePaymentIntentId,
                   @Nullable String stripeSubscriptionId, @Nullable String stripeChargeId) {
        this.id = id;
        this.userId = userId;
        this.charge = charge;
        this.currency = currency;
        this.url = url;
        this.timestampCreated = timestampCreated;
        this.timestampExpires = timestampExpires;
        this.timestampAuthorized = timestampAuthorized;
        this.timestampCancelled = timestampCancelled;
        this.intervall = intervall;
        this.productId = productId;
        this.productName = productName;
        this.productQuantity = productQuantity;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripeChargeId = stripeChargeId;
        this.paymentProcessor = getPaymentProcessor();
        this.state = getState();
    }

    private State getState() {
        State state = State.CREATED;
        if (timestampAuthorized != null) state = State.AUTHORIZED;
        if (timestampCancelled != null) state = State.CANCELLED;
        return state;
    }

    private PaymentProcessor getPaymentProcessor() {
        if (isPayPalSupported()) return PaymentProcessor.PAYPAL;
        else if (isStripeSupported()) return PaymentProcessor.STRIPE;
        else return null;
        // TODO ADD NEW PROCESSORS
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", charge=" + charge +
                ", currency='" + currency + '\'' +
                ", url='" + url + '\'' +
                ", intervall=" + intervall +
                ", state=" + state +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", productQuantity=" + productQuantity +
                ", paymentProcessor=" + paymentProcessor +
                '}';
    }

    public boolean isPayPalSupported() {
        return paypalSubscriptionId != null || paypalOrderId != null || paypalCaptureId != null;
    }

    public boolean isStripeSupported() {
        return stripePaymentIntentId != null || stripeSubscriptionId != null;
    }

    public long getUrlTimeoutMs() {
        if (paymentProcessor == PaymentProcessor.PAYPAL) return PayHook.paypalUrlTimeoutMs;
        else if (paymentProcessor == PaymentProcessor.STRIPE) return PayHook.stripeUrlTimeoutMs;
        else throw new IllegalArgumentException("Unknown/Invalid payment processor: " + paymentProcessor);
        // TODO ADD NEW PROCESSORS
    }

    /**
     * Must be a recurring payment, otherwise just returns -1. <br>
     * Note that this will always return the difference between the last two (latest and future) payments
     * for this subscription and ignore this {@link Payment} object (also returns -1 when there is no future payment).
     *
     * @return the time left (in milliseconds) until the next due payment.
     * Thus, you get a negative value, if the due payment date was already exceeded, which usually means
     * that the subscription was cancelled.
     * @throws NullPointerException when the future {@link Payment#timestampCreated} is null.
     */
    public long getTimeLeftMs() {
        if (!isRecurring()) return -1;
        long msNow = System.currentTimeMillis();
        List<Payment> payments;
        if (isPayPalSupported())
            payments = PayHook.database.getPendingFuturePayments("paypal_subscription_id", paypalSubscriptionId);
        else if (isStripeSupported())
            payments = PayHook.database.getPendingFuturePayments("stripe_subscription_id", stripeSubscriptionId);
        else throw new IllegalArgumentException("Unknown/Invalid payment processor: " + paymentProcessor);
        // TODO ADD NEW PROCESSORS
        if (payments.isEmpty()) return -1;
        return Objects.requireNonNull(payments.get(0).timestampCreated).getTime() - msNow;
    }


    public boolean isPending() {
        return timestampAuthorized == null && timestampCancelled == null;
    }

    public boolean isRecurring() {
        return intervall != Intervall.NONE;
    }

    public boolean isCancelled() {
        return timestampCancelled != null;
    }

    public boolean isAuthorized(){
        return timestampAuthorized != null;
    }

    public boolean isRefund(){
        return charge < 0;
    }

    public boolean isFree(){
        return charge == 0;
    }

    public enum State {
        /**
         * The initial (default) state of a payment directly after creating it.
         *
         * @see PayHook#createPayment(String, String, Product, PaymentProcessor, String, String)
         */
        CREATED(0),
        /**
         * When the user visited the {@link Payment#url}
         * and authorized/completed the payment.
         *
         * @see PayHook#paymentAuthorizedEvent
         */
        AUTHORIZED(1),
        /**
         * When a payment was created/expected, but
         * the user didn't complete/authorize it in time.
         *
         * @see PayHook#paymentCancelledEvent
         */
        CANCELLED(2);

        public final int state;

        State(int state) {
            this.state = state;
        }
    }

    public enum Intervall {
        /**
         * One time payment. Type 0.
         */
        NONE(0),
        /**
         * Recurring payment every month (exactly 30 days). Type 1.
         */
        DAYS_30(1),
        /**
         * Recurring payment every 3 months (exactly 90 days). Type 2.
         */
        DAYS_90(2),
        /**
         * Recurring payment every 6 months (exactly 180 days). Type 3.
         */
        DAYS_180(3),
        /**
         * Recurring payment every 12 months (exactly 360 days). Type 4.
         */
        DAYS_360(4),
        /**
         * Recurring payment with a custom intervall. Type 5.
         */
        DAYS_CUSTOM(5);

        public final int type;

        Intervall(int type) {
            this.type = type;
        }

        public long toDays() {
            if (type == 1) return 30;
            else if (type == 2) return 90;
            else if (type == 3) return 180;
            else if (type == 4) return 360;
            return 0;
        }

        public long toHours() {
            return toDays() * 24;
        }

        public long toMilliseconds() {
            return toHours() * 3600000;
        }
    }
}
