/*
 * Copyright Osiris Team
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * AutoPlug License.  Please consult the file "LICENSE" for details.
 */

package com.osiris.payhook.paypal;

import com.osiris.autoplug.core.logger.AL;
import com.osiris.autoplug.webserver.GD;
import com.osiris.autoplug.webserver.objects.Product;
import com.osiris.autoplug.webserver.payment.Payments;
import com.paypal.http.HttpResponse;
import com.paypal.http.serializer.Json;
import com.paypal.orders.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PayPalOrder {

    /**
     * Method to generate sample create order body with <b>CAPTURE</b> intent
     *
     * @return OrderRequest with created order request
     */
    private OrderRequest buildRequestBody(Product product) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        ApplicationContext applicationContext = new ApplicationContext().brandName("Osiris Codes").landingPage("BILLING")
                .cancelUrl(GD.LINK_WEBSITE + "/store").returnUrl(GD.LINK_WEBSITE + "/user-order-verify").userAction("CONTINUE")
                .shippingPreference("NO_SHIPPING");
        orderRequest.applicationContext(applicationContext);

        List<PurchaseUnitRequest> purchaseUnitRequests = new ArrayList<PurchaseUnitRequest>();
        PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest()
                .referenceId("PUHF")
                .description("AutoPlug - Store").customId("CUST-Osiris-Codes").softDescriptor("Osiris-Codes")
                .amountWithBreakdown(new AmountWithBreakdown().currencyCode("EUR").value(product.getPrice())
                        .amountBreakdown(new AmountBreakdown().itemTotal(new Money().currencyCode("EUR").value(product.getPrice()))
                        )) // Durch kleinunternehmerregelung keine Umsatzsteuer
                .items(new ArrayList<Item>() {
                    {
                        add(new Item().name(product.getName()).description(product.getDescription()).sku("Product-ID: " + product.getType())
                                .unitAmount(new Money().currencyCode("EUR").value(product.getPrice())).quantity("1")
                                .category("DIGITAL_GOODS")); // Durch kleinunternehmerregelung keine Umsatzsteuer
                    }
                });
        purchaseUnitRequests.add(purchaseUnitRequest);
        orderRequest.purchaseUnits(purchaseUnitRequests);
        return orderRequest;
    }

    /**
     * Method to create order
     *
     * @return HttpResponse<Order> response received from API
     * @throws IOException Exceptions from API if any
     */
    public HttpResponse<Order> createOrder(Product product) throws IOException {
        AL.info("Creating order...");
        OrdersCreateRequest request = new OrdersCreateRequest();
        request.header("prefer", "return=representation");
        request.requestBody(buildRequestBody(product));
        HttpResponse<Order> response = Payments.PAYPAL.execute(request);
        if (response.statusCode() == 201) {
            AL.info("Status Code: " + response.statusCode());
            AL.info("Status: " + response.result().status());
            AL.info("Order ID: " + response.result().id());
            AL.info("Intent: " + response.result().checkoutPaymentIntent());
            AL.info("Links: ");
            for (LinkDescription link : response.result().links()) {
                AL.info("\t" + link.rel() + ": " + link.href() + "\tCall Type: " + link.method());
            }
            AL.info("Total Amount: " + response.result().purchaseUnits().get(0).amountWithBreakdown().currencyCode()
                    + " " + response.result().purchaseUnits().get(0).amountWithBreakdown().value());
            AL.info("Full response body:");
            AL.info(new JSONObject(new Json().serialize(response.result())).toString(4));
        }
        return response;
    }
}