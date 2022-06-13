/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.payhook.paypal;

public class PayPalSubscription {
    private final PayPalPlan payPalPlan;
    private final String id;
    private final Status status;
    private final String approveUrl; // Example: https://www.paypal.com/webapps/billing/subscriptions?ba_token=BA-2M539689T3856352J
    private final String editUrl; // Example: https://api-m.paypal.com/v1/billing/subscriptions/I-BW452GLLEP1G
    private final String selfUrl; // Example: https://api-m.paypal.com/v1/billing/subscriptions/I-BW452GLLEP1G

    public PayPalSubscription(PayPalPlan payPalPlan, String id, Status status, String approveUrl, String editUrl, String selfUrl) {
        this.payPalPlan = payPalPlan;
        this.id = id;
        this.status = status;
        this.approveUrl = approveUrl;
        this.editUrl = editUrl;
        this.selfUrl = selfUrl;
    }

    public PayPalPlan getPlan() {
        return payPalPlan;
    }

    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public String getApproveUrl() {
        return approveUrl;
    }

    public String getEditUrl() {
        return editUrl;
    }

    public String getSelfUrl() {
        return selfUrl;
    }

    public enum Status {
        APPROVAL_PENDING, // The subscription is created but not yet approved by the buyer.
        APPROVED, // The buyer has approved the subscription.
        ACTIVE, // The subscription is active.
        SUSPENDED, // The subscription is suspended.
        CANCELLED, // The subscription is cancelled.
        EXPIRED, // The subscription is expired.
    }
}
