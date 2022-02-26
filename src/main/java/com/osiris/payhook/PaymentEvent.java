package com.osiris.payhook;

public class PaymentEvent {
    public Product product;
    public Payment payment;

    public PaymentEvent(Product product, Payment payment) {
        this.product = product;
        this.payment = payment;
    }
}
