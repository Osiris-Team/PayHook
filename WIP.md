### Aim

Working with payments in Java is painful. If you want to expand to other
third-party payment processors it's hell, that's why PayHook exists.
```java
Payment payment = PayHook.createPayment("user_id", product, PaymentProcessor.PAYPAL, "success_url", "cancel_url");
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
- Make webhook requirement optional by doing periodic checks.
- Functionality to send payments. Currently, it's only possible to receive payments from customers.
- Add support for real goods. Currently, the focus is on digital goods and services,
which means that billing addresses are ignored.
- Add support for more payment processors.
- Run as server (payments-server) since currently this software is only meant for Java developers to integrate into their existing projects.

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

```java
import com.osiris.autoplug.core.json.exceptions.HttpErrorException;
import com.osiris.payhook.exceptions.InvalidChangeException;
import com.paypal.base.rest.PayPalRESTException;
import com.stripe.exception.StripeException;

import java.io.IOException;
import java.sql.SQLException;

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
              true);

      PayHook.initBraintree("merchant_id","public_key", "private_key", "https://my-shop.com/braintree-hook");
      PayHook.initStripe("secret_key", "https://my-shop.com/stripe-hook");

      pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", Payment.Intervall.NONE, 0);
      pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", Payment.Intervall.DAYS_30, 0);

      PayHook.paymentAuthorizedEvent.addAction((action, event) -> {
        // Backend business logic in here. Gets executed every time.
        Product product = event.product;
        Payment payment = event.payment;
      }, e -> {
        e.printStackTrace();
      });

      PayHook.paymentCancelledEvent.addAction((action, event) -> {
        // Backend business logic in here. Gets executed every time.
        Product product = event.product;
        Payment payment = event.payment;
      }, e -> {
        e.printStackTrace();
      });

      // The cleaner thread is only needed
      // to remove the added actions from further below,
      // since those may not get executed once.
      // Remove them after 6 hours
      PayHook.paymentAuthorizedEvent.initCleaner(3600000, obj -> { // Check every hour
        return obj != null && System.currentTimeMillis() - ((Long) obj) > 21600000; // 6hours
      }, Exception::printStackTrace);
      PayHook.paymentCancelledEvent.initCleaner(3600000, obj -> { // Check every hour
        return obj != null && System.currentTimeMillis() - ((Long) obj) > 21600000; // 6hours
      }, Exception::printStackTrace);

    }  catch (SQLException | StripeException | IOException | HttpErrorException | PayPalRESTException | InvalidChangeException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This can be anywhere in your application.
   */
  void onBuyBtnClick() throws Exception {
    Payment payment = PayHook.createPayment("USER_ID", pCoolCookie, PaymentProcessor.BRAINTREE, "https://my-shop.com/payment/success", "https://my-shop.com/payment/cancel");
    // Forward your user to payment.url
    PayHook.paymentAuthorizedEvent.addAction((action, event) -> {
      if(event.payment.id == payment.id){
        action.remove(); // To make sure it only gets executed once, for this payment.
        Product product = event.product;
        Payment authorizedPayment = event.payment;
        // Additional UI code here (make sure to have access to the UI thread).
      }
    }, e -> {
      e.printStackTrace();
    }).object = System.currentTimeMillis();

    PayHook.paymentCancelledEvent.addAction((action, event) -> {
      if(event.payment.id == payment.id){
        action.remove(); // To make sure it only gets executed once, for this payment.
        Product product = event.product;
        Payment cancelledPayment = event.payment;
        // Additional UI code here (make sure to have access to the UI thread).
      }
    }, e -> {
      e.printStackTrace();
    }).object = System.currentTimeMillis();
  }

  /**
   * This can be anywhere in your application.
   */
  void onAnotherBuyBtnClick() throws Exception {
    Payment payment = PayHook.createPayment("USER_ID", pCoolSubscription, PaymentProcessor.STRIPE, "https://my-shop.com/payment/success", "https://my-shop.com/payment/cancel");
    // Forward your user to payment.url
    PayHook.paymentAuthorizedEvent.addAction((action, event) -> {
      if(event.payment.id == payment.id){
        action.remove(); // To make sure it only gets executed once, for this payment.
        Product product = event.product;
        Payment authorizedPayment = event.payment;
        // Additional UI code here (make sure to have access to the UI thread).
      }
    }, e -> {
      e.printStackTrace();
    }).object = System.currentTimeMillis();

    PayHook.paymentCancelledEvent.addAction((action, event) -> {
      if(event.payment.id == payment.id){
        action.remove(); // To make sure it only gets executed once, for this payment.
        Product product = event.product;
        Payment cancelledPayment = event.payment;
        // Additional UI code here (make sure to have access to the UI thread).
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
`https://your-store.com/paypal-hook` or `https://your-store.com/stripe-hook`.
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
*Testing:*

You probably want to test this before running it on your production app.
You can do that easily from your current computer, just follow the steps below.

- Clone this repository and open a terminal in the root directory.
- Make sure to have business accounts at the payment processors
and enter the sandbox/test credentials into `test-credentials.txt` (also create an
[ngrok](ngrok.com) account, which is needed to tunnel/forward the webhook events
from a public domain/ip to your current computer).
- Run `TODO` in your commandline to run all tests.
