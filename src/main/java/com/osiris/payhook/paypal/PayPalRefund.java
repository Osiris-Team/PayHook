/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.payhook.paypal;

import com.osiris.autoplug.core.logger.AL;
import com.osiris.autoplug.webserver.payment.Payments;
import com.paypal.http.HttpResponse;
import com.paypal.http.serializer.Json;
import com.paypal.payments.CapturesRefundRequest;
import com.paypal.payments.LinkDescription;
import com.paypal.payments.Refund;
import com.paypal.payments.RefundRequest;
import org.json.JSONObject;

import java.io.IOException;

/*
 *
 *1. Import the PayPal SDK client that was created in `Set up Server-Side SDK`.
 *This step extends the SDK client. It's not mandatory to extend the client, you also can inject it.
 */
public class PayPalRefund {

    //2. Set up your server to receive a call from the client
    // Method to refund the capture. Pass a valid capture ID.
    //
    // @param captureId Capture ID from authorizeOrder response
    // @param debug     true = print response data
    // @return HttpResponse<Capture> response received from API
    // @throws IOException Exceptions from API if any
    public HttpResponse<Refund> refundOrder(String captureId) throws IOException {
        CapturesRefundRequest request = new CapturesRefundRequest(captureId);
        request.prefer("return=representation");
        request.requestBody(buildRequestBody());
        HttpResponse<Refund> response = Payments.PAYPAL.execute(request);

        AL.info("Status Code: " + response.statusCode());
        AL.info("Status: " + response.result().status());
        AL.info("Refund Id: " + response.result().id());
        AL.info("Links: ");
        for (LinkDescription link : response.result().links()) {
            AL.info("\t" + link.rel() + ": " + link.href() + "\tCall Type: " + link.method());
        }
        AL.info("Full response body:");
        AL.info(new JSONObject(new Json()
                .serialize(response.result())).toString(4));
        return response;
    }

    // Creating a body for partial refund request.
    // For full refund, pass the empty body.
    //
    // @return OrderRequest request with empty body

    public RefundRequest buildRequestBody() {
        RefundRequest refundRequest = new RefundRequest();
        /* // Make empty
        Money money = new Money();
        money.currencyCode("USD");
        money.value("20.00");
        refundRequest.amount(money);
         */
        return refundRequest;
    }

}
