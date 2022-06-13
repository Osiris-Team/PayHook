/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.payhook.paypal;

public class PayPalPlan {
    private final MyPayPal context;
    private final String planId;
    private final String productId;
    private final String name;
    private final String description;
    private final Status status;

    public PayPalPlan(MyPayPal context, String planId, String productId, String name, String description, Status status) {
        this.context = context;
        this.planId = planId;
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    /**
     * Creates a subscription for this plan and returns it.
     */

    public String getPlanId() {
        return planId;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        CREATED, ACTIVE, INACTIVE
    }
}
