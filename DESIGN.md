# Design
PayHooks' main goal is to achieve a simple way of handling payments in Java.
This file explains the relevant classes and how they work together, in detail.

## PayHook
This class contains static methods for initialising the internal
payment processor specific libraries, as well as creating pending payments
your customers must authorize by visiting a checkout page and completing
the necessary steps (like logging in with their PayPal account or entering
credit card details).

This class also has methods for handling payment processor webhook events.
These are critical at the moment to PayHooks functionality.
Webhook events make it easier to deal with recurring payments (subscriptions)
and also help to avoid hitting payment processor API rate-limits.

## Payment
The payment object represents a payment made by a customer or to a customer 
(a refund for example). It can be in multiple states, like pending 
(not paid/authorized yet), completed/authorized or cancelled. Methods are available to distinguish
between these states and payment type. 

This is a database object which means that static methods are available
to interact with the Payment table with which you can
list/retrieve all payments for example.

## PaymentWarning (todo)
If something didn't go to plan when receiving a webhook event related
to a payment, then a PaymentWarning object is created that contains a paymentId
and message, and added to the database. 
These warnings should give an insight in what exactly went wrong and 
usually shouldn't be threaded lightly.

Note that if a paymentId couldn't be determined (which usually means that
the issue occurred before even being able to assign it to a paymentId)
an exception is thrown instead in `PayHook.receiveWebhookEvent(...)` method.

This is a database object which means that static methods are available
to interact with the PaymentWarning table with which you can
list/retrieve all PaymentWarnings for example.

## Product
It contains details about the product a customer can buy (make payments for)
and its details get synchronized across payment processors when changes
are detected. Thus changes to a products' details shouldn't be done
over the payment processors web panels, but to the product object
itself inside Java code. The `PayHook.putProduct(...)` method then handles
synchronisation.

This is a database object which means that static methods are available
to interact with the Product table with which you can
list/retrieve all Products for example.

## Subscription
Helper class to map multiple payments to a single subscription object.
Provides additional subscription specific methods that make it easier
to deal with payments for subscriptions.

Not a database object, but still has some static methods
for retrieving subscriptions.