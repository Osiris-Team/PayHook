# PayHook
A Java-API for validating PayPals Webhooks.
# NOTE: Functionality was not fully tested yet!
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
## Usage example
This example uses spring(tomcat) to listen for POST post requests. 
Nevertheless this can be easily ported to your web application.
```java
Under construction...
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
