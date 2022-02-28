package com.osiris.payhook;

public enum PaymentType {
    /**
     * One time payment. Type 0.
     */
    ONE_TIME(0),
    /**
     * Recurring payment every month. Type 1.
     */
    RECURRING(1),
    /**
     * Recurring payment every 3 months. Type 2.
     */
    RECURRING_3(2),
    /**
     * Recurring payment every 6 months. Type 3.
     */
    RECURRING_6(3),
    /**
     * Recurring payment every 12 months. Type 4.
     */
    RECURRING_12(4),
    /**
     * Recurring payment with a custom intervall. Type 5.
     */
    RECURRING_CUSTOM(5);

    public final int type;
    PaymentType(int type){
        this.type = type;
    }
}
