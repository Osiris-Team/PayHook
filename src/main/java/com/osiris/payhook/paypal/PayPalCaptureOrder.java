/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.payhook.paypal;

import com.osiris.autoplug.webserver.payment.Payments;
import com.paypal.http.HttpResponse;
import com.paypal.orders.Order;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.OrdersCaptureRequest;

import java.io.IOException;

public class PayPalCaptureOrder {

    /**
     * Creating empty body for capture request. We can set the payment source if
     * required.
     *
     * @return OrderRequest request with empty body
     */
    public OrderRequest buildRequestBody() {
        return new OrderRequest();
    }

    /**
     * Method to capture order after creation. Valid approved order Id should be
     * passed an argument to this method.
     *
     * @param payPalOrderId Order ID from createOrder response
     * @return HttpResponse<Order> response received from API
     * @throws IOException Exceptions from API if any
     */
    public HttpResponse<Order> captureOrder(String payPalOrderId) throws Exception {
        OrdersCaptureRequest request = new OrdersCaptureRequest(payPalOrderId);
        request.requestBody(buildRequestBody());
        HttpResponse<Order> response = Payments.PAYPAL.execute(request);
        if (response.statusCode() != 201) {
            throw new Exception("Error-Code: " + response.statusCode() + " Status-Message: " + response.result().status());
        }
        return response;
    }

}
