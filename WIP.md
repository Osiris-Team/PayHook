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
- Currently, works only via Webhooks.
- Supported payment processors: [PayPal](https://paypal.com)
and [Stripe (supports ApplePay, GooglePay etc.)](https://stripe.com/docs/payments/payment-methods/overview).
- Easy product/payment creation across multiple payment-processors, through abstraction.
- No need to handle JSON or learn the individual REST-APIs of the payment-processors.
- Secure, verified payments without duplicates, due to the design being based solely on validated webhook events.
- Catch all payments. If your application is offline for example 
payment processors notice that the webhook event wasn't received 
and try again later, several times.
- Actions on missed payments. Cancel the users' subscription if the amount due was not paid.
- Low-level SQL queries to ensure maximum speed.
- Payments saved to the database. This saves you time when creating summaries or tax reports, 
since all payments are at one place and not scattered over each payment processor.
- Different database in sandbox mode, to ensure live and sandbox actions are separated strictly.
- Commandline tool to extract relevant data from the database and modify it.

### Todo
- TODO Switch from Paypal to Braintree, due to the mess of PayPals API
- Functionality to send payments. Currently, it's only possible to receive payments.
- Add support for real goods. Currently, the focus is on digital goods and services,
which means that billing addresses are ignored.
- Add support for more payment processors.

### Installation

You can run PayHook as a standalone command line app (this is in todo),
or integrate it into your Java app by adding it as a dependency via [Maven/Gradle/Sbt/Leinigen](https://jitpack.io/#Osiris-Team/PayHook/LATEST).

Prerequisites:
- SQL database like MySQL, MariaDB, etc... (normally pre-installed on a linux server)
- PayPal/Stripe business account and API credentials.
- A Java web-app based on [Spring](https://spring.io/web-applications) for example, to listen for webhook events/notifications.
- 5 minutes of free time to set this up.

Important:
- Do not change product details later via the payment processors dashboards. Only change product details
in the code.
- If you have already existing subscriptions and payments use the [PayHook-Database-Helper]() tool,
  (in todo) to create a `payhook` database with the existing information.
- Copy, paste and customize (even better read and understand) all the code shown below, to make
sure everything works as expected.

```java
public class ExampleConstants {
    public static Product pCoolCookie;
    public static Product pCoolSubscription;

    // Insert the below somewhere where it gets ran once.
    // For example in the static constructor of a Constants class of yours, like shown here.
    static {
        try {
            PayHook.init(
                    "Brand-Name",
                    "db_url",
                    "db_username",
                    "db_password",
                    true); // Sandbox

            PayHook.initPayPal("client_id", "client_secret", "https://my-shop.com/paypal-hook");
            PayHook.initStripe("secret_key", "https://my-shop.com/stripe-hook");

            pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", PaymentType.ONE_TIME, 0);
            pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", PaymentType.RECURRING, 0);

            PayHook.onMissedPayment(event -> {
                // Executed when the user misses the payment for a subscription (recurring).
                try{
                    Product product = event.product;
                    Payment payment = event.payment;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        } 
    }

    void onBuyBtnClick() throws Exception {
        // The code below should be run when the user clicks on a buy button.
        Payment payment = PayHook.createPayment("USER_ID", pCoolCookie, PaymentProcessor.PAYPAL, "https://my-shop.com/payment/success", "https://my-shop.com/payment/cancel");
        PayHook.onPayment(payment.paymentId, event -> {
            // Executed when the payment was received.
        });
    }

    void onAnotherBuyBtnClick() throws Exception {
        Payment payment = PayHook.createPayment("USER_ID", pCoolSubscription, PaymentProcessor.STRIPE, "https://my-shop.com/payment/success", "https://my-shop.com/payment/cancel");
        PayHook.onPayment(payment.paymentId, event -> {
            // Executed when the payment was received.
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
public class PayHookExample {

  // This listens at https://.../paypal-hook
  // for PayPal webhook events and returns a "OK" text as response.
  @RequestMapping(value = "paypal-hook", method = RequestMethod.POST)
  @GetMapping(produces = "text/plain")
  public String receiveAndRespondPayPal(HttpServletRequest request, HttpServletResponse response) {
    try {
        response.setStatusCode(HttpStatus.OK); // Directly set status code
        response.flush();
        PayHook.receiveWebhookEvent(
                PaymentProcessor.PAYPAL,
                getHeadersAsMap(request),
                getBodyAsString(request));
    } catch (Exception e) {
      e.printStackTrace();
      // TODO handle exception
    }
    return "OK"; // Always return status code 200 with an "OK" text no matter what the result to annoy attackers.
  }

  // This listens at https://.../stripe-hook
  // for Stripe webhook events and returns a "OK" text as response.
  @RequestMapping(value = "stripe-hook", method = RequestMethod.POST)
  @GetMapping(produces = "text/plain")
  public String receiveAndRespondStripe(HttpServletRequest request, HttpServletResponse response) {
    try {
      response.setStatusCode(HttpStatus.OK); // Directly set status code
      response.flush();
      PayHook.receiveWebhookEvent(
              PaymentProcessor.STRIPE,
              getHeadersAsMap(request),
              getBodyAsString(request));
    } catch (Exception e) {
      e.printStackTrace();
      // TODO handle exception
    }
    return "OK"; // Always return status code 200 with an "OK" text no matter what the result to annoy attackers.
  }

  
  
  
  
  // Simple helper method to help you extract the headers from HttpServletRequest object.
  private Map<String, String> getHeadersAsMap(HttpServletRequest request) {
    Map<String, String> map = new HashMap<String, String>();
    @SuppressWarnings("rawtypes")
    Enumeration headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String key = (String) headerNames.nextElement();
      String value = request.getHeader(key);
      map.put(key, value);
    }
    return map;
  }

  // Simple helper method to fetch request data as a string from HttpServletRequest object.
  private String getBodyAsString(HttpServletRequest request) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()))){
      String line = "";
      while ((line=reader.readLine())!=null)
        stringBuilder.append(line);
    }
    return stringBuilder.toString();
  }
}
```
