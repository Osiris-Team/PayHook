# PayHook
A Java-API for validating PayPals Webhooks.
# NOTE: STILL IN DEVELOPMENT! MAY NOT BE FULLY FUNCTIONAL YET!
## Installation
[Click here for maven/gradle/sbt/leinigen instructions.](https://jitpack.io/#Osiris-Team/PayHook/LATEST)
Java 8+ required.
Make sure to watch this repository to get notified of future updates.
## Motivation
Basically PayPals latest [Checkout v2 Java-SDK](https://github.com/paypal/Checkout-Java-SDK)
is missing the webhook event validation feature, which was available in the old, deprecated
[PayPal Java-SDK](https://github.com/paypal/PayPal-Java-SDK).
That's why PayHook exists. Its aim, is to provide an easy to use Java-API for validating
webhook events.
PayHook is based on the official, old [PayPal Java-SDK](https://github.com/paypal/PayPal-Java-SDK).
## Usage example
This example uses spring(tomcat) to listen for POST requests. 
Nevertheless, this can be easily ported to your web application.
```java
@RestController
@RequestMapping(value = "paypal-hook", method = RequestMethod.POST)
public class PayHookExample {

    // This listens at https://.../paypal-hook/... for paypal notification messages and returns a text as response.
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
                    Arrays.asList("CHECKOUT.ORDER.APPROVED", "PAYMENTS.PAYMENT.CREATED"), // Insert your valid event type here. Full list of all event types/name here: https://developer.paypal.com/docs/api-basics/notifications/webhooks/event-names
                    header,
                    body);

            // Do event validation
            try{
                payHook.validateWebhookEvent(event);
                // Do stuff on validation success here
            } catch (Exception e) {
                System.out.println("Validation failed: "+e.getMessage());
                // Do stuff on validation fail here
            }

        } catch (Exception e) {
            e.printStackTrace();
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
