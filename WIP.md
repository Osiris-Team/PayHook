### Progress

- **🟢Tested and working🟢**
  - (paypal-authorize) Handle for checkout payment.
  - (paypal-authorize) Handle for subscription initial payment.
  - (stripe-authorize) Handle for checkout payment.
  - (stripe-authorize) Handle for subscription initial payment.
  - 
- **🔴Tested and not working🔴**
  - (paypal-cancel) Handle for payment.
  - (paypal-cancel) Handle for subscription payment.
  - 
- **⚫Not tested⚫**
  - (stripe-cancel) Handle for payment.
  - (stripe-cancel) Handle for subscription payment.
  - (paypal-authorize) Handle for subscription following payment.
  - (stripe-authorize) Handle for subscription following payment.
  - 
- **⚫Not implemented⚫**
  - (paypal-refund) Handle for checkout payment.
  - (paypal-refund) Handle for subscription payment.
  - (stripe-refund) Handle for checkout payment.
  - (stripe-refund) Handle for subscription payment.
  - 

### Aim

Working with payments in Java is painful. If you want to expand to other
third-party payment processors it's even worse, that's why PayHook exists.
```java
  /**
   * This can be anywhere in your application.
   */
  void onBuyBtnClick() throws Exception {
    Payment payment = PayHook.createPayment("USER_ID", pCoolCookie, PaymentProcessor.PAYPAL);
    // Forward your user to payment.url to complete/authorize the payment here...

    PayHook.onPaymentAuthorized.addAction((action, event) -> {
      if (event.payment.id == payment.id) {
        action.remove(); // To make sure it only gets executed once, for this payment.
        Product product = event.product;
        Payment authorizedPayment = event.payment;
        // Insert ONLY additional UI code here (make sure to have access to the UI thread).
        // Code that does backend, aka important stuff does not belong here!
      }
    }, e -> {
      e.printStackTrace();
    }).object = System.currentTimeMillis();

    //PayHook.onPaymentCancelled.addAction((action, event) -> {...}
  }
```
PayHooks' main goal is simplicity, thus there are only 3 important Java objects (**PayHook** | **Product** | **Payment**)
and as you can see above, creating payments can be done in one line. The PayHook class 
contains most of the important methods.

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
- Make webhook requirement optional by doing periodic checks.
- Functionality to send payments. Currently, it's only possible to receive payments from customers.
- Add support for real goods. Currently, the focus is on digital goods and services,
which means that billing addresses are ignored.
- Add support for more payment processors.
- Run as server (payments-server) since currently this software is only meant for Java developers to integrate into their existing projects.

### Testing

You probably want to test this before running it in a production environment.
You can do that easily from your current computer, just follow the steps below.

- Clone this repository.
- Make sure to have business accounts at your desired payment processors
  and enter the sandbox/test credentials into `test-credentials.yml` (also create an
  [ngrok](ngrok.com) account, which is needed to tunnel/forward the webhook events
  from a public domain/ip to your current computer).
- Run the [MainTest](https://github.com/Osiris-Team/PayHook/blob/main/src/test/java/com/osiris/payhook/MainTest.java)
  class from your IDE.
- Enter `help` for a list of available commands once initialised.

### Installation

You can run PayHook as a standalone command line app (this is in todo),
or integrate it into your Java app by adding it as a dependency via [Maven/Gradle/Sbt/Leinigen](https://jitpack.io/#Osiris-Team/PayHook/LATEST).

*Prerequisites:*
- SQL database like MySQL, MariaDB, etc... (normally pre-installed on a linux server)
- PayPal/Stripe business account and API credentials.
- A Java web-app based on [Spring](https://spring.io/web-applications) for example, to listen for webhook events/notifications.
- 5 minutes of free time to set this up.

*Important:*
- Do not change product details later via the payment processors dashboards. Only change product details
in the code.
- If you have already existing subscriptions and payments use the [PayHook-Database-Helper]() tool,
  (in todo) to create a `payhook` database with the existing information.
- Copy, paste and customize (even better read and understand) all the code shown below, to make
sure everything works as expected.
- The provided PayPal client id/secret (app) must have permissions for using `Transaction Search`.
Tick that box and save the change at your [developer dashboard](https://developer.paypal.com/developer/applications).
Otherwise, it's not possible to find the subscription-id for a payment.
- Webhook events are checked for validity/authenticity
by doing an API request to the payment-processor.
To avoid hitting API rate-limits make sure
to protect against fake events by limiting the amount of events per ip by adding 
timeouts for example.
Also limit the maximum allowed size of a webhook event to avoid sending large amounts
of data to the API (TODO FIND OUT BYTE-SIZE OF BIGGEST PAYPAL/STRIPE WEBHOOK-EVENT AND MENTION IT HERE).

```java
package com.osiris.payhook;

public class ExampleConstants {
  public static Product pCoolCookie;
  public static Product pCoolSubscription;

  // Insert the below somewhere where it gets ran once.
  // For example in the static constructor of a Constants class of yours, like shown here, or in your main method.
  static {
    try {
      PayHook.init(
              "Brand-Name",
              "db_url",
              "db_username",
              "db_password",
              true,
              "https://my-shop.com/payment/success",
              "https://my-shop.com/payment/cancel");

      PayHook.initBraintree("merchant_id", "public_key", "private_key", "https://my-shop.com/braintree-hook");
      PayHook.initStripe("secret_key", "https://my-shop.com/stripe-hook");

      pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", Payment.Intervall.NONE);
      pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", Payment.Intervall.MONTHLY);

      PayHook.onPaymentAuthorized.addAction(event -> {
        // Additional backend business logic for all payments in here.
        // Gets executed every time a payment is authorized/completed.
        // If something goes wrong in here a RuntimeException is thrown.
        Product product = event.product;
        Payment payment = event.payment;
      });

      PayHook.onPaymentCancelled.addAction(event -> {
        // Additional backend business logic for all payments in here.
        // Gets executed every time a payment was cancelled.
        // If something goes wrong in here a RuntimeException is thrown.
        Product product = event.product;
        Payment payment = event.payment;
      });

      // The cleaner thread is only needed
      // to remove the added actions from further below, since those may not get executed once.
      // Remove them after 6-7 hours.
      PayHook.onPaymentAuthorized.initCleaner(3600000, obj -> { // Check every hour
        return obj != null && System.currentTimeMillis() - ((Long) obj) > 21600000; // 6hours
      }, Exception::printStackTrace);
      PayHook.onPaymentCancelled.initCleaner(3600000, obj -> { // Check every hour
        return obj != null && System.currentTimeMillis() - ((Long) obj) > 21600000; // 6hours
      }, Exception::printStackTrace);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This can be anywhere in your application.
   */
  void onBuyBtnClick() throws Exception {
    Payment payment = PayHook.createPayment("USER_ID", pCoolCookie, PaymentProcessor.PAYPAL);
    // Forward your user to payment.url to complete/authorize the payment here...

    PayHook.onPaymentAuthorized.addAction((action, event) -> {
      if (event.payment.id == payment.id) {
        action.remove(); // To make sure it only gets executed once, for this payment.
        Product product = event.product;
        Payment authorizedPayment = event.payment;
        // Insert ONLY additional UI code here (make sure to have access to the UI thread).
        // Code that does backend, aka important stuff does not belong here!
      }
    }, e -> {
      e.printStackTrace();
    }).object = System.currentTimeMillis();

    PayHook.onPaymentCancelled.addAction((action, event) -> {
      if (event.payment.id == payment.id) {
        action.remove(); // To make sure it only gets executed once, for this payment.
        Product product = event.product;
        Payment cancelledPayment = event.payment;
        // Insert ONLY additional UI code here (make sure to have access to the UI thread).
        // Code that does backend, aka important stuff does not belong here!
      }
    }, e -> {
      e.printStackTrace();
    }).object = System.currentTimeMillis();
  }

  /**
   * This can be anywhere in your application.
   */
  void onAnotherBuyBtnClick() throws Exception {
    Payment payment = PayHook.createPayment("USER_ID", pCoolSubscription, PaymentProcessor.STRIPE);
    // Forward your user to payment.url to complete/authorize the payment here...

    PayHook.onPaymentAuthorized.addAction((action, event) -> {
      if (event.payment.id == payment.id) {
        action.remove(); // To make sure it only gets executed once, for this payment.
        Product product = event.product;
        Payment authorizedPayment = event.payment;
        // Insert ONLY additional UI code here (make sure to have access to the UI thread).
        // Code that does backend, aka important stuff does not belong here!
      }
    }, e -> {
      e.printStackTrace();
    }).object = System.currentTimeMillis();

    PayHook.onPaymentCancelled.addAction((action, event) -> {
      if (event.payment.id == payment.id) {
        action.remove(); // To make sure it only gets executed once, for this payment.
        Product product = event.product;
        Payment cancelledPayment = event.payment;
        // Insert ONLY additional UI code here (make sure to have access to the UI thread).
        // Code that does backend, aka important stuff does not belong here!
      }
    }, e -> {
      e.printStackTrace();
    }).object = System.currentTimeMillis();
  }
}
```

*Webhooks:*

Depending on your initialised payment processors, you have to
create links that listen for their webhook events too. For example like:
`https://my-store.com/paypal-hook` or `https://my-store.com/stripe-hook`.
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


