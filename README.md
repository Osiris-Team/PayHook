# PayHook [![](https://jitpack.io/v/Osiris-Team/PayHook.svg)](https://jitpack.io/#Osiris-Team/PayHook)
The simplest payment processing Java library in the world. Unifies PayPal and Stripe into one API. 
Supports both regular payments and subscriptions,
with NO need of handling json, requests, storage into a database yourself.
[Click here for Maven/Gradle/Sbt/Leinigen instructions.](https://jitpack.io/#Osiris-Team/PayHook/LATEST)
Java 8+ required.
Make sure to watch this repository to get notified of future updates.
Support and chat over at [Discord](https://discord.com/invite/GGNmtCC).

## Funding
I am actively maintaining this repository, publishing new releases and working 
on its codebase for free, so if this project benefits you and/or your company consider 
donating an amount you seem fit. Thank you!

<a href="https://www.paypal.com/donate?hosted_button_id=JNXQCWF2TF9W4"><img src="https://github.com/andreostrovsky/donate-with-paypal/raw/master/blue.svg" height="40"></a>

## Aim

Working with payments in Java is painful. If you want to expand to other
third-party payment processors it's hell, that's why PayHook exists.
```java
  void onBuyBtnClick() throws Exception {
    Payment payment = PayHook.expectPayment("USER_ID", myProduct, PaymentProcessor.PAYPAL,
          authorizedPayment -> {
            // Executed when payment was authorized by buyer
          });
    // Forward your user to complete/authorize the payment here...
    openUrl(payment.url);
  }
```
PayHooks' main goal is simplicity, thus there are only 3 important classes (**PayHook** | **Product** | **Payment**).
The PayHook class contains most of the important static methods.

## Features
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
- Validate PayPals Webhook events/notifications.
- Low-level SQL queries to ensure maximum speed.
- Payments saved to the database. This saves you time when creating summaries or tax reports,
  since all payments are at one place and not scattered over each payment processor.
- Different database in sandbox mode, to ensure live and sandbox actions are separated strictly.
- Commandline tool to extract relevant data from the database and modify it.

## Todo
- Make webhook requirement optional by doing periodic checks.
- Functionality to send payments. Currently, it's only possible to receive payments from customers.
- Add support for real goods. Currently, the focus is on digital goods and services,
  which means that billing addresses are ignored.
- Add support for more payment processors.
- Run as server (payments-server) since currently this software is only meant for Java developers to integrate into their existing projects.

## Testing

You probably want to test this before running it in a production environment.
You can do that easily from your current computer, just follow the steps below.

- Clone this repository.
- Run the [MainTest](https://github.com/Osiris-Team/PayHook/blob/main/src/test/java/com/osiris/payhook/MainTest.java)
  class from your IDE (full example of a working application that integrates PayHook).
- Make sure to have business accounts at your desired payment processors
  and enter the sandbox/test credentials into `test-credentials.yml` (also create an
  [ngrok](ngrok.com) account, which is needed to tunnel/forward the webhook events
  from a public domain/ip to your current computer).
- Enter `help` for a list of available commands once initialised.

I am running PayHook on my own site at https://autoplug.one/store.
Even though my site is closed-source you can see how I implemented PayHook below,
so you can get a feel for how its done in a real-world application.
<details>
  <summary>Show/Hide code</summary>

```java
/**
 * GlobalData
 */
public class GD {

    public static Product SERVER_AD_24h;
    public static Product SERVER_AD_72h;
    public static Product SERVER_AD_120h;

    public static Product PREMIUM_1M;
    public static Product PREMIUM_3M;
    public static Product PREMIUM_6M;
    public static Product PREMIUM_12M;

    public static Product SELF_HOST;
    public static Product SELF_HOST_1M;
    public static Product SELF_HOST_3M;
    public static Product SELF_HOST_6M;
    public static Product SELF_HOST_12M;

    static {

        try {
            PayHook.init(
                    "Osiris-Codes",
                    Config.master.databaseUrl,
                    Config.master.databaseName,
                    Config.master.databaseUser,
                    Config.master.databasePassword,
                    Application.IS_TEST_MODE, // Sandbox mode?
                    Config.master.linkWebsite +"/order-created",
                    Config.master.linkWebsite +"/order-aborted");

            String paypalHookUrl = Config.master.linkWebsite +"/paypal-hook";
            String stripeHookUrl = Config.master.linkWebsite +"/stripe-hook";

            //
            // Init processors
            //
            AL.info("paypalHookUrl: "+paypalHookUrl+" stripeHookUrl: "+stripeHookUrl);
            PayHook.initPayPal(Application.IS_TEST_MODE ? Config.master.paypalSandboxClientId : Config.master.paypalClientId,
                    Application.IS_TEST_MODE ? Config.master.paypalSandboxClientSecret :  Config.master.paypalClientSecret,
                    paypalHookUrl);
            PayHook.initStripe(Application.IS_TEST_MODE ? Config.master.stripeSandboxSecret : Config.master.stripeSecretKey,
                    stripeHookUrl);

            //
            // Events for server advertisement
            //
            BetterConsumer<Payment> onAuthorized = payment -> {
                List<Featured_User_Servers> servers = Featured_User_Servers.wherePayment_id().is(payment.id).get();
                if(servers.isEmpty()) throw new Exception("No featured server found for payment: "+ payment.toPrintString());
                Featured_User_Servers server = servers.get(0);
                server.isAuthorized = 1;
                Featured_User_Servers.update(server);
            };
            BetterConsumer<Payment> onRefunded = payment -> {
                Featured_User_Servers.wherePayment_id().is(payment.id).remove();
            };
            BetterConsumer<Payment> onCancelled = payment -> {
                if(!payment.isAuthorized()) return; // Do nothing if the payment was not authorized
                payment.timestampCancelled = System.currentTimeMillis();
                Payment.update(payment); // To make sure this doesn't get executed again through expired event.
                Featured_User_Servers.wherePayment_id().is(payment.id).remove();
            };
            BetterConsumer<Payment> onExpired = onCancelled;
            SERVER_AD_24h = PayHook.putProduct(0, 999, "EUR", "Server feature 24h", "One time payment for adding your server for 24 hours to the 'featured servers' list.",
                    Payment.Interval.NONE).onPaymentAuthorized(onAuthorized, AL::warn).onPaymentRefunded(onRefunded, AL::warn).onPaymentCancelled(onCancelled, AL::warn).onPaymentExpired(onExpired, AL::warn);
            SERVER_AD_72h = PayHook.putProduct(1, 2997, "EUR", "Server feature 72h", "One time payment for adding your server for 72 hours to the 'featured servers' list.",
                    Payment.Interval.NONE).onPaymentAuthorized(onAuthorized, AL::warn).onPaymentRefunded(onRefunded, AL::warn).onPaymentCancelled(onCancelled, AL::warn).onPaymentExpired(onExpired, AL::warn);
            SERVER_AD_120h = PayHook.putProduct(2, 4995, "EUR", "Server feature 120h", "One time payment for adding your server for 120 hours to the 'featured servers' list.",
                    Payment.Interval.NONE).onPaymentAuthorized(onAuthorized, AL::warn).onPaymentRefunded(onRefunded, AL::warn).onPaymentCancelled(onCancelled, AL::warn).onPaymentExpired(onExpired, AL::warn);

            //
            // Events for premium subscription
            //
            onAuthorized = payment -> {
                Commands.upgradeUserToPremium(payment.userId);
            };
            onRefunded = payment -> {
                Commands.downgradePremiumUser(payment.userId);
            };
            onCancelled = payment -> {
                if(!payment.isAuthorized()) return; // Do nothing if the payment was not authorized
                // Only disable if there is no time left
                Subscription sub = new Subscription(payment);
                if(!sub.isTimeLeft()){
                    Commands.downgradePremiumUser(payment.userId);
                    payment.timestampCancelled = System.currentTimeMillis();
                    Payment.update(payment); // To make sure this doesn't get executed again through expired event.
                }
            };
            onExpired = onCancelled;
            // Currently, discounts are not added:
            PREMIUM_1M = PayHook.putProduct(3, 199, "EUR", "Premium-Membership", "Recurring payment for Premium-Membership at "+Config.master.linkRawWebsite +".",
                    Payment.Interval.MONTHLY).onPaymentAuthorized(onAuthorized, AL::warn).onPaymentRefunded(onRefunded, AL::warn).onPaymentCancelled(onCancelled, AL::warn).onPaymentExpired(onExpired, AL::warn);
            PREMIUM_3M = PayHook.putProduct(4, 597, "EUR", "Premium-Membership", "Recurring payment for Premium-Membership at "+Config.master.linkRawWebsite +".",
                    Payment.Interval.TRI_MONTHLY).onPaymentAuthorized(onAuthorized, AL::warn).onPaymentRefunded(onRefunded, AL::warn).onPaymentCancelled(onCancelled, AL::warn).onPaymentExpired(onExpired, AL::warn);
            PREMIUM_6M = PayHook.putProduct(5, 1194, "EUR", "Premium-Membership", "Recurring payment for Premium-Membership at "+Config.master.linkRawWebsite +".",
                    Payment.Interval.HALF_YEARLY).onPaymentAuthorized(onAuthorized, AL::warn).onPaymentRefunded(onRefunded, AL::warn).onPaymentCancelled(onCancelled, AL::warn).onPaymentExpired(onExpired, AL::warn);
            PREMIUM_12M = PayHook.putProduct(6, 2388, "EUR", "Premium-Membership", "Recurring payment for Premium-Membership at "+Config.master.linkRawWebsite +".",
                    Payment.Interval.YEARLY).onPaymentAuthorized(onAuthorized, AL::warn).onPaymentRefunded(onRefunded, AL::warn).onPaymentCancelled(onCancelled, AL::warn).onPaymentExpired(onExpired, AL::warn);


            //
            // Events for autoplug web license
            //
            onAuthorized = payment -> {
                Commands.addAutoPlugWebLicense(payment.userId, payment.id);
            };
            onRefunded = payment -> {
                Commands.removeAutoPlugWebLicense(payment.id);
            };
            onCancelled = payment -> {
                if(!payment.isAuthorized()) return; // Do nothing if the payment was not authorized
                // Only disable if there is no time left
                Subscription sub = new Subscription(payment);
                if(!sub.isTimeLeft()){
                    Commands.removeAutoPlugWebLicense(payment.id);
                    payment.timestampCancelled = System.currentTimeMillis();
                    Payment.update(payment); // To make sure this doesn't get executed again through expired event.
                }
            };
            onExpired = onCancelled;
            // Equals price for 10 years, monthly payment of 14,99€ -> 1798,80 €
            SELF_HOST = PayHook.putProduct(7, 179880, "EUR", "AutoPlug-Web self-host", "One time payment for AutoPlug-Web self-host.",
                    Payment.Interval.NONE).onPaymentAuthorized(onAuthorized, AL::warn).onPaymentRefunded(onRefunded, AL::warn).onPaymentCancelled(onCancelled, AL::warn).onPaymentExpired(onExpired, AL::warn);
            // Currently, discounts are not added:
            SELF_HOST_1M = PayHook.putProduct(8, 1499, "EUR", "AutoPlug-Web self-host", "Recurring payment for AutoPlug-Web self-host at "+Config.master.linkRawWebsite +".",
                    Payment.Interval.MONTHLY).onPaymentAuthorized(onAuthorized, AL::warn).onPaymentRefunded(onRefunded, AL::warn).onPaymentCancelled(onCancelled, AL::warn).onPaymentExpired(onExpired, AL::warn);
            SELF_HOST_3M = PayHook.putProduct(9, 4497, "EUR", "AutoPlug-Web self-host", "Recurring payment for AutoPlug-Web self-host at "+Config.master.linkRawWebsite +".",
                    Payment.Interval.TRI_MONTHLY).onPaymentAuthorized(onAuthorized, AL::warn).onPaymentRefunded(onRefunded, AL::warn).onPaymentCancelled(onCancelled, AL::warn).onPaymentExpired(onExpired, AL::warn);
            SELF_HOST_6M = PayHook.putProduct(10, 8994, "EUR", "AutoPlug-Web self-host", "Recurring payment for AutoPlug-Web self-host at "+Config.master.linkRawWebsite +".",
                    Payment.Interval.HALF_YEARLY).onPaymentAuthorized(onAuthorized, AL::warn).onPaymentRefunded(onRefunded, AL::warn).onPaymentCancelled(onCancelled, AL::warn).onPaymentExpired(onExpired, AL::warn);
            SELF_HOST_12M = PayHook.putProduct(11, 17988, "EUR", "AutoPlug-Web self-host", "Recurring payment for AutoPlug-Web self-host at "+Config.master.linkRawWebsite +".",
                    Payment.Interval.YEARLY).onPaymentAuthorized(onAuthorized, AL::warn).onPaymentRefunded(onRefunded, AL::warn).onPaymentCancelled(onCancelled, AL::warn).onPaymentExpired(onExpired, AL::warn);

            PayHook.onPaymentExpired.addAction(payment -> {
                // Payment was not authorized within the max time
                // Remove it from the database
                if(!payment.isAuthorized()){
                    String s = "Removed expired payment because it was not authorized/paid in time. Product \""+payment.productName+"\" for "
                            +new Converter().toMoneyString("EUR", payment.charge)+", created at: "+payment.timestampCreated;
                    Notify.addNotification(payment.userId, "Removed expired payment.", s);
                    Payment.remove(payment);
                    AL.info(s);
                }

            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
```
</details>

## Installation

You can run PayHook as a standalone command line app (this is in TODO),
or integrate it into your Java app by adding it as a dependency via [Maven/Gradle/Sbt/Leinigen](https://jitpack.io/#Osiris-Team/PayHook/LATEST).

### Prerequisites
- SQL database like MySQL, MariaDB, etc... (normally pre-installed on a linux server)
- PayPal/Stripe business account and API credentials.
- A Java web-app based on [Spring](https://spring.io/web-applications) for example, to listen for webhook events/notifications.
- 5 minutes of free time to set this up.

### Important
- Do not change product details later via the payment processors dashboards. Only change product details
  in the code.
- If you have already existing subscriptions and payments use the [PayHook-Database-Helper]() tool,
  (in todo) to create a `payhook` database with the existing information.
- Copy, paste and customize (even better read and understand) all the code shown below, to make
  sure everything works as expected.
- Webhook events are checked for validity/authenticity
  by doing an API request to the payment-processor.
  To avoid hitting API rate-limits make sure
  to protect against fake events by limiting the amount of events per ip by adding
  timeouts for example.
  Also limit the maximum allowed size of a webhook event to avoid sending large amounts
  of data to the API (TODO FIND OUT BYTE-SIZE OF BIGGEST PAYPAL/STRIPE WEBHOOK-EVENT AND MENTION IT HERE).

```java
package com.osiris.payhook;

import com.osiris.jsqlgen.payhook.Payment;
import com.osiris.jsqlgen.payhook.Product;

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
              "db_name",
              "db_username",
              "db_password",
              true,
              "https://my-shop.com/payment/success",
              "https://my-shop.com/payment/cancel");

      PayHook.initBraintree("merchant_id", "public_key", "private_key", "https://my-shop.com/braintree-hook");
      PayHook.initStripe("secret_key", "https://my-shop.com/stripe-hook");

      pCoolCookie = PayHook.putProduct(0, 500, "EUR", "Cool-Cookie", "A really yummy cookie.", Payment.Interval.NONE);
      pCoolSubscription = PayHook.putProduct(1, 999, "EUR", "Cool-Subscription", "A really creative description.", Payment.Interval.MONTHLY);

      PayHook.onPaymentAuthorized.addAction(payment -> {
        // Additional backend business logic for all payments in here.
        // Gets executed every time a payment is authorized/completed.
        // If something goes wrong in here a RuntimeException is thrown.
      });

      PayHook.onPaymentCancelled.addAction(payment -> {
        // Additional backend business logic for all payments in here.
        // Gets executed every time a payment was cancelled.
        // If something goes wrong in here a RuntimeException is thrown.
      });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This can be anywhere in your application.
   */
  void onAnotherBuyBtnClick() throws Exception {
    Payment payment = PayHook.expectPayment("USER_ID", pCoolSubscription, PaymentProcessor.STRIPE,
            authorizedPayment -> {
              // Insert ONLY additional UI code here (make sure to have access to the UI thread).
              // Code that does backend, aka important stuff does not belong here!
            }, cancelledPayment -> {
              // Insert ONLY additional UI code here (make sure to have access to the UI thread).
              // Code that does backend, aka important stuff does not belong here!
            });
    // Forward your user to payment.url to complete/authorize the payment here...
  }
}
```

### Webhooks

You must create links that listen for webhook events for each of your
payment processors. These could look like this:
`https://my-store.com/paypal-hook` or `https://my-store.com/stripe-hook`.
I'm planing on making PayHook-Spring version that handles all that too.
Here is an example on how to do that with SpringBoot:

```java
@RestController
public class SpringBootExample {

  // This listens at https://.../paypal-hook
  // for PayPal webhook events
  @RequestMapping(value = "paypal-hook", method = RequestMethod.POST)
  public void receiveAndRespondPayPal(HttpServletRequest request, HttpServletResponse response) {
    try {
      response.setStatus(HttpServletResponse.SC_OK); // Directly set status code
      PayHook.receiveWebhookEvent(PaymentProcessor.PAYPAL, getHeadersAsMap(request),
              IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8)); // Apache Utils
    } catch (Exception e) {
      AL.warn(e);
    }
  }

  // This listens at https://.../stripe-hook
  // for Stripe webhook events
  @RequestMapping(value = "stripe-hook", method = RequestMethod.POST)
  public void receiveAndRespondStripe(HttpServletRequest request, HttpServletResponse response) {
    try {
      response.setStatus(HttpServletResponse.SC_OK); // Directly set status code
      PayHook.receiveWebhookEvent(PaymentProcessor.STRIPE, getHeadersAsMap(request),
              IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8)); // Apache Utils
    } catch (Exception e) {
      AL.warn(e);
    }
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
}

```

## Progress

### Send requests
#### 🟢 Tested and working
- **Authorize:**
    - (paypal) checkout payment.
    - (paypal) subscription.
    - (stripe) checkout payment.
    - (stripe) subscription.
- **Cancel:**
    - (paypal) payment.
    - (paypal) subscription payment.
    - (stripe) payment.
    - (stripe) subscription payment.
- **Refund:**
    - (paypal) payment.
    - (paypal) subscription payment.
    - (stripe) payment.
    - (stripe) subscription payment.

#### 🔴 Untested, unimplemented or not working

### Receive requests (webhook)
#### 🟢 Tested and working
- **Authorize:**
    - (paypal) checkout payment.
    - (paypal) subscription initial payment.
    - (stripe) checkout payment.
    - (stripe) subscription initial payment.
    - (paypal) subscription following payment.
    - (stripe) subscription following payment.
- **Cancel:**
    - (stripe) payment.
    - (stripe) subscription payment.
    - (paypal) payment.
    - (paypal) subscription payment.
- **Refund:**
    - (stripe) payment.
    - (stripe) subscription payment.
    - (paypal) payment.
    - (paypal) subscription payment.

#### 🔴 Untested, unimplemented or not working


## FAQ
<div>

<details>
<summary>How to validate a PayPal webhook notification/event?</summary>

Note that validation is already done automatically by PayHook.
This example shows how to do it manually.
Its only a few lines:
```java
MyPayPal paypal = new MyPayPal(clientId, clientSecret, MyPayPal.Mode.SANDBOX);
PaypalWebhookEvent event = new PaypalWebhookEvent(paypalWebhookId, paypalWebhookEventTypes, header, body);
if(!paypal.isWebhookEventValid(event)){
    System.err.println("Received invalid PayPal webhook event.");
    return;
}
```

Here is a complete SpringBoot example:
```java
@RestController
@RequestMapping(value = "paypal-hook", method = RequestMethod.POST)
public class PayHookExample {

    MyPayPal paypal = new MyPayPal(clientId, clientSecret, MyPayPal.Mode.SANDBOX);

    // This listens at https://.../paypal-hook
    // for paypal notification messages and returns a "OK" text as response.
    @GetMapping(produces = "text/plain")
    public @ResponseBody String receiveAndRespond(HttpServletRequest request) {

        System.out.println("Received webhook event. Validating...");
        try{
            boolean isValid = paypal.isWebhookEventValid("INSERT_VALID_WEBHOOK_ID", // Get it from here: https://developer.paypal.com/developer/applications/
                    Arrays.asList("CHECKOUT.ORDER.APPROVED", "PAYMENTS.PAYMENT.CREATED"), // Insert your valid event types/names here. Full list of all event types/names here: https://developer.paypal.com/docs/api-basics/notifications/webhooks/event-names
                    getHeadersAsMap(request),
                    getBodyAsString(request));

            if (isValid) 
                System.out.println("Webhook-Event is valid!");
            else
                System.err.println("Webhook-Event is not valid!");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Validation failed: "+e.getMessage());
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
</details>

<details>
<summary>How do I create a partial refund?</summary>

Simply change the charge to the desired amount like so:
```java
Payment p = ...; // Get payment to refund, original charge = 100.
p.charge = 10; // We are refunding only 10 cents (10%)
// instead of 100 (the full amount) in this example.
PayHook.refundPayment(p);
// The charge above is only changed in memory
// and does not get reflected in the database.
// The database charge gets subtracted,
// so in this case its 90 after the refund.
```
</details>
</div>
