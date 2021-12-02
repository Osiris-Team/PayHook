Working with payments in Java is real pain. If you want to expand to other
third-party payment processors its hell.

That's why I thought to expand PayHook to handle all that.
The basic idea is that we would process all payments only thorough webhook events.
Which is to be honest, the safest and simplest way.

Prerequisites:
- SQL database like MySQL, MariaDB, etc... (this is where PayHook will store the orders, products and payments)
- 5 minutes of free time to set this up.

Features:
- Secure, verified payments without duplicates, due to the design being based solely on validated webhook events.
- Catches all payments. If your application is offline for example 
payment processors notice that the webhook event wasn't received 
and try again several times.
- Notifies you on missed payments by the user. For example when the user misses a payment for his subscription.
- Simplified product and order creation (also across multiple payment-processors).
- Handles saving of products and orders in your SQL database
- TODO Saves each payment to the database. This saves you time when creating summaries or tax reports, 
since all payments are at one place and not scattered over each payment processor.
- TODO Live and Sandbox tables in the database, to ensure these are separated strictly.
- Lowest level queries to the database to ensure maximum speed.

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