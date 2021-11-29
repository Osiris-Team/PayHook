# PayHook [![](https://jitpack.io/v/Osiris-Team/PayHook.svg)](https://jitpack.io/#Osiris-Team/PayHook)
A Java-API for validating PayPals Webhook events/notifications.
## Links
 - Support and chat over at [Discord](https://discord.com/invite/GGNmtCC)
 - Support the development by [donating](https://www.paypal.com/donate?hosted_button_id=JNXQCWF2TF9W4)
## Installation
[Click here for maven/gradle/sbt/leinigen instructions.](https://jitpack.io/#Osiris-Team/PayHook/LATEST)
Java 8+ required.
Make sure to watch this repository to get notified of future updates.
## Major changes coming soon
Currently, this API is only used for validating PayPals webhook events.
But what if it could do more than that?

Working with payments in Java is real pain. If you want to expand to other
third-party payment processors its hell.

That's why I thought to expand PayHook to handle all that.
The basic idea is that we would process all payments only thorough webhook events.
Which is to be honest, the safest and simplest way.

Features:
- Secure, verified payments without duplicates, due to the design being based solely on validated webhook events.
- Simplified product and order creation (also accross multiple payment-processors).
- Handles saving of products and orders in your database

I am still working on the design. Here is what I've got:

Setup:
1. Set database information
2. Set API credentials of payment processors
3. Create/Update products

```java
public class Constants{
    // Init PayHook once. For example in a Constants class of yours.
    // It'll connect to your database and search for the payhook database.
    // If not found it'll create it and insert the "orders" table
    // with the same fields as the "Order" class further below.
    public static PayHook P;
    static{
        P = new PayHook(
                "database_url", 
                "database_username", 
                "database_password");
        
        P.setPayPalCredentials("client_id", "client_secret", true);
        P.setStripeCredentials("secret_key", true);
        
        P.putProduct(1, ...);
        P.putProduct(2, ...);
        P.putProduct(3, ...);
    }
}
```
Note that you must create links that listen for webhook events yourself like
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
```java
class Examples{

    /**
     * User clicks on the "Buy" button for example.
     * Then run following code:
     */
    public void onBuyBtnClick(){
        Order order = null;
        boolean selectedPayPal = true;
        if (selectedPayPal) order = P.createPayPalOrder();
        else if (selectedStripe) order = P.createStripeOrder();
        else{
            // etc.
        }
        order.onOrderPaid(webhookEvent -> {
            // Executed once the order was paid.
        });
    }

    /**
     * How do you check for payments on a subscription for example?
     * Since we rely on webhook events we do not need to spam the
     * payment processors APIs to check the payments.
     * We check our own DB and compare each orders, Order#lastPaymentTimestamp with
     * the Orders billing intervall.
     */
    public void exampleRecurring(){
        // Start a thread which checks recurring orders. 
        P.initRecurringOrdersChecker(12); // Checks the orders every 12 hours
        P.onMissingPayment(details -> {
            // Executed when 
        });
    }
}

```
```java
class Order{
    private int id;
    
    // Product related information:
    private String price;
    private String name;
    private String description;
    private int billingType; // Returns a value from 0 to 5
    public boolean isRecurring(){ // For example a subscription
        return billingType != 0;
    }
    public boolean isBillingInterval1Month(){ 
        return billingType == 1;
    }
    public boolean isBillingInterval3Months(){
        return billingType == 2;
    }
    public boolean isBillingInterval6Months(){
        return billingType == 3;
    }
    public boolean isBillingInterval12Months(){
        return billingType == 4;
    }
    public boolean isCustomBillingInterval(){
        return billingType == 5;
    }
    private int customBillingIntervallInDays;
    private Timestamp lastPaymentTimestamp;
    
    // Information related to the status of the order:
    private boolean isPaid;
    private boolean isRefunded;
    private boolean isCancelled;
}
```

## Motivation
Basically PayPals latest [Checkout v2 Java-SDK](https://github.com/paypal/Checkout-Java-SDK)
is missing the webhook event validation feature, which was available in the old, deprecated
[PayPal Java-SDK](https://github.com/paypal/PayPal-Java-SDK).
That's why PayHook exists. Its aim, is to provide a fast and easy to use Java-API for validating
webhook events, without the need of any extra dependencies.
PayHooks validation methods are based on the official, old [PayPal Java-SDKs](https://github.com/paypal/PayPal-Java-SDK) methods and were enhanced for greater functionality/performance.
## Usage example
This example uses spring(tomcat) to listen for POST requests. 
Nevertheless, this can be easily ported to your web application.
```java
@RestController
@RequestMapping(value = "paypal-hook", method = RequestMethod.POST)
public class PayHookExample {

    // This listens at https://.../paypal-hook
    // for paypal notification messages and returns a "OK" text as response.
    @GetMapping(produces = "text/plain")
    public @ResponseBody String receiveAndRespond(HttpServletRequest request) {

        System.out.println("Received webhook event. Validating...");
        try{
            PayHook payHook = new PayHook("INSERT_CLIENT_ID", "INSERT_CLIENT_SECRET");
            payHook.setSandboxMode(true); // Default is false. Remove this in production.
            
            boolean isValid = payHook.isWebhookEventValid("INSERT_VALID_WEBHOOK_ID", // Get it from here: https://developer.paypal.com/developer/applications/
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
## FAQ
<div>
 <details>
  <summary>Difference between online and offline validation?</summary>
The online validation occurs over PayPals REST-API and the offline validation occurs on the currently running machine, using the same methods as the deprecated PayPal Checkout SDK.
</details>
<details>
  <summary>What is a POST http request?</summary>
Every request has a header and a body.
By design, the POST request method requests that a web server accepts the data enclosed in the body of the request message, most likely for storing it.
</details>
<details>
  <summary>What does the header contain?</summary>
In our case it contains: content-length, paypal-transmission-sig,
paypal-cert-url, paypal-auth-algo, correlation-id,
paypal-transmission-id, client_pid,
accept, cal_poolstack, paypal-transmission-time, paypal-auth-version,
host, content-type and finally the user-agent.
</details>
<details>
  <summary>What does the body contain?</summary>
The body is a json string with a bunch of event specific data.
For more details see the paypal docs: <a href="https://developer.paypal.com/docs/api-basics/notifications/webhooks/notification-messages/">webhooks/notification-messages</a>
</details>
</div>

## Tags
<div>
<details>
  <summary>Open/Close tags</summary>
Tags are used to make this repository easier to find for others. <br>
paypal validate webhook java
paypal java webhook example
paypal webhook
webhook paypal
paypal java webhooks
paypal java webhook example
paypal webhook
java paypal integration
java paypal api
paypal validate webhook java
paypal java webhooks
paypal java webhook example
paypal webhook
java paypal integration
java paypal api
paypal validate webhook java
paypal webhook events
paypal webhooks tutorial
paypal webhook listener example
paypal webhook simulator
paypal webhook id
paypal webhook api
paypal webhook listener
paypal webhook localhost
paypal webhook authentication
paypal add webhook
paypal api webhook event
paypal webhook ip address
paypal billing agreement webhook
what is a paypal webhooks
webhook paypal
paypal button webhook
paypal smart button webhook
paypal webhook c#
paypal webhook custom field
paypal webhook certificate
paypal create webhook
paypal checkout webhook
paypal configure webhooks
paypal.com webhooks
paypal webhook payment.sale.completed
paypal webhook delay
paypal webhook documentation
paypal webhook discord
paypal donation webhook
paypal dispute webhook
webhook data paypal
paypal webhook example
paypal webhook event types
paypal webhook example php
paypal webhook endpoint
paypal webhook einrichten
paypal webhook example c#
paypal webhook events pending
paypal webhook for recurring payment
paypal webhook format
paypal webhook get
paypal api get webhook
paypal handle webhooks
paypal webhook ip
paypal webhook ipn
paypal webhook implementation
paypal webhook vs ipn
paypal webhooks interface
paypal payouts webhook id
paypal webhook java
paypal validate webhook java
paypal webhook node js
paypal java webhook example
paypal webhook listener php example
paypal webhook listener example c#
paypal webhook listener example nodejs
paypal webhook listener php
paypal webhooks laravel
paypal manage webhooks
paypal webhook not working
paypal webhook nodejs
paypal webhook .net
paypal webhook notifications not working
paypal webhook simulator not working
paypal sandbox webhook not working
paypal webhook event names
paypal ipn or webhook
paypal webhook php
paypal webhook python
paypal webhook pending
paypal webhook payload
paypal webhook port
paypal payment webhook
paypal plus webhook
paypal webhook url
paypal webhook retry
paypal webhook response
paypal webhook reference id
paypal webhook refund
paypal webhook recurring payment
paypal send webhook
paypal subscription renew webhook
paypal webhooks
paypal webhooks simulator
paypal webhooks example
paypal webhooks php
paypal webhooks localhost
paypal webhooks vs ipn
paypal webhook tutorial
paypal webhook test
paypal not a valid webhook url
paypal webhook verification
paypal webhook validation
paypal webhook validate
paypal webhook verify signature
paypal webhook validation php
woocommerce paypal webhook
magento 2 paypal webhook
shopware 6 paypal webhook
</details>
</div>
