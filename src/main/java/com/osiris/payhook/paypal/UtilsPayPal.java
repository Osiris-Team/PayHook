/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.payhook.paypal;

public class UtilsPayPal {

    public PayPalPlan.Status getPlanStatus(String statusAsString) {
        if (statusAsString.equalsIgnoreCase(PayPalPlan.Status.ACTIVE.name()))
            return PayPalPlan.Status.ACTIVE;
        else if (statusAsString.equalsIgnoreCase(PayPalPlan.Status.INACTIVE.name()))
            return PayPalPlan.Status.INACTIVE;
        else if (statusAsString.equalsIgnoreCase(PayPalPlan.Status.CREATED.name()))
            return PayPalPlan.Status.CREATED;
        else
            return null;
    }

    public PayPalSubscription.Status getSubscriptionStatus(String statusAsString) {
        if (statusAsString.equalsIgnoreCase(PayPalSubscription.Status.APPROVAL_PENDING.name()))
            return PayPalSubscription.Status.APPROVAL_PENDING;
        else if (statusAsString.equalsIgnoreCase(PayPalSubscription.Status.APPROVED.name()))
            return PayPalSubscription.Status.APPROVED;
        else if (statusAsString.equalsIgnoreCase(PayPalSubscription.Status.ACTIVE.name()))
            return PayPalSubscription.Status.ACTIVE;
        else if (statusAsString.equalsIgnoreCase(PayPalSubscription.Status.SUSPENDED.name()))
            return PayPalSubscription.Status.SUSPENDED;
        else if (statusAsString.equalsIgnoreCase(PayPalSubscription.Status.CANCELLED.name()))
            return PayPalSubscription.Status.CANCELLED;
        else if (statusAsString.equalsIgnoreCase(PayPalSubscription.Status.EXPIRED.name()))
            return PayPalSubscription.Status.EXPIRED;
        else
            return null;
    }

}
