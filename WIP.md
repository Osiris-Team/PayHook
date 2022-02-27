### Aim

Working with payments in Java is painful. If you want to expand to other
third-party payment processors it's hell, that's why PayHook exists.
```java
Payment payment = payHook.createPayment("user_id", product, PaymentProcessor.PAYPAL, "success_url", "cancel_url");
payHook.onPayment(payment.paymentId, event -> {
    // Executed when the payment was received.
});
```
PayHooks' main goal is simplicity, thus there are only 3 important Java objects (**PayHook** | **Product** | **Payment**)
and as you can see above, creating payments can be done in one line.

### Features
- Support for regular products and products with recurring payments (subscriptions).
- Supported payment processors: PayPal and Stripe.
- Secure, verified payments without duplicates, due to the design being based solely on validated webhook events.
- Catch all payments. If your application is offline for example 
payment processors notice that the webhook event wasn't received 
and try again later, several times.
- Actions on missed payments. Cancel the users subscription if the amount due was not paid.
- Easy product/payment creation across multiple payment-processors, through abstraction.
- Low-level SQL queries to ensure maximum speed.
- Payments saved to the database. This saves you time when creating summaries or tax reports, 
since all payments are at one place and not scattered over each payment processor.
- TODO Live and Sandbox tables in the database, to ensure these are separated strictly.
- Commandline tool to extract relevant data from the database and modify it.

### Installation

Prerequisites:
- SQL database like MySQL, MariaDB, etc...
- 5 minutes of free time to set this up.

Setup:
1. Set database information
2. Set API credentials of payment processors
3. Create/Update products

```java
public class ExampleConstants {
  public static final PayHookV3 P;
  public static final Product product;
  public static final Product productRecurring;

  static {
    // Insert the below somewhere where it gets ran once.
    // For example in a Constants class of yours.
    try {
      P = new PayHookV3(
              "payhook",
              "db_url",
              "db_name",
              "db_password");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    P.initPayPal(true, "client_id", "client_secret");
    P.initStripe(true, "secret_key");
    
    product = P.createProduct();
    productRecurring = P.createProduct();

    P.onMissedPayment(event -> { // Relevant if you have products with recurring payments (like subscriptions)
      try{
        Order o = event.getOrder();
        // TODO what should happen when the user misses a payment?
        // TODO implement logic.
      } catch (Exception e) {
        // TODO handle exception
        e.printStackTrace();
      }
    });
  }
  
  // The code below should be run when the user clicks on a buy button.
  void onBuyBtnClick(){
    Order order1 = P.createStripeOrder(product);
    Order order2 = P.createStripeOrder(productRecurring);

    order1.onPaymentReceived(event -> { // Note that this only gets ran once

    });

    order2.onPaymentReceived(event -> {

    });
  }
}
```
Webhooks:

Depending on your initialised payment processors, you have to
create links that listen for their webhook events too. For example like:
`https://example.com/paypal-webhook` or `https://example.com/stripe-webhook`.
I'm planing on making PayHook-Spring version that handles all that too.
Here is an example on how to do that with Spring:
```java
@RestController
@RequestMapping(value = "paypal-hook", method = RequestMethod.POST)
public class PayHookExample {
    // This listens at https://.../paypal-hook
    // for PayPal webhook events and returns a "OK" text as response.
    @GetMapping(produces = "text/plain")
    public @ResponseBody String receiveAndRespond(HttpServletRequest request) {
        try{
            boolean isValid = P.isWebhookEventValid("INSERT_VALID_WEBHOOK_ID", // Get it from here: https://developer.paypal.com/developer/applications/
                    Arrays.asList("CHECKOUT.ORDER.APPROVED", "PAYMENTS.PAYMENT.CREATED"), // Insert your valid event types/names here. Full list of all event types/names here: https://developer.paypal.com/docs/api-basics/notifications/webhooks/event-names
                    getHeadersAsMap(request),
                    getBodyAsString(request));

            if (isValid) 
                P.executeValidPayPalWebhookEvent(); // Fires P.onValidPayPalWebhookEvent();
            else
                P.executeInvalidPayPalWebhookEvent(); // Fires P.onInvalidPayPalWebhookEvent();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Validation failed: "+e.getMessage());
        }
        return "OK"; // Always return status code 200 with an "OK" text no matter what the result to annoy attackers.
    }
}
```
