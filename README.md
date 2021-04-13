# PayHook [![](https://jitpack.io/v/Osiris-Team/PayHook.svg)](https://jitpack.io/#Osiris-Team/PayHook)
A Java-API for validating PayPals Webhook events/notifications.
## NOTE: STILL IN DEVELOPMENT! MAY NOT BE FULLY FUNCTIONAL YET!
### Sandbox mode was tested and is functional. 
### 'Real' mode was not yet tested.
## Links
 - Support and chat over at [Discord](https://discord.com/invite/GGNmtCC)
## Installation
[Click here for maven/gradle/sbt/leinigen instructions.](https://jitpack.io/#Osiris-Team/PayHook/LATEST)
Java 8+ required.
Make sure to watch this repository to get notified of future updates.
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
    public @ResponseBody String doMain(
            HttpServletRequest request) {

        System.out.println("Received webhook event at .../paypal-hook/...");
        try{
            PayHook payHook = new PayHook();
            payHook.setSandboxMode(true); // Default is false. Remove this in production.
            
            // Get the header and body
            WebhookEventHeader header = payHook.parseAndGetHeader(getHeadersAsMap(request));
            JsonObject         body   = payHook.parseAndGetBody(getBodyAsString(request));

            // Create this event
            WebhookEvent event = new WebhookEvent(
                    "insert your valid webhook id here", // Get it from here: https://developer.paypal.com/developer/applications/
                    Arrays.asList("CHECKOUT.ORDER.APPROVED", "PAYMENTS.PAYMENT.CREATED"), // Insert your valid event types/names here. Full list of all event types/names here: https://developer.paypal.com/docs/api-basics/notifications/webhooks/event-names
                    header,
                    body);

            // Do event validation
            payHook.validateWebhookEvent(event); 
            System.out.println("Validation successful!");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Validation failed: "+e.getMessage());
        }
        return "OK";
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
## Validation workflow
1. Receive a POST http request
2. Parse it into a WebHookEvent object
3. Validate the event
## FAQ
<div>
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
