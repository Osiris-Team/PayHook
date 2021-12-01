package com.osiris.payhook;

public class Event {
    private Order order;

    public Event(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
