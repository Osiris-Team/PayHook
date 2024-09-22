## Upgrade Notes
If you are on a version mentioned below you will need to follow the respective steps to upgrade
to the latest version. You might need to follow more steps the older your current version is.


### Your version <= 4.9.15
- Added new field Payment.chargeRefunded and new refund logic. Previously, to create a refund, you would need to
  subtract the amount to refund from Payment.charge. Now, you need to add the amount to Payment.chargeRefunded instead.
  Meaning Payment.charge will never change and always display the original price, thus when calculating the sum of all payments for
  example, keep in mind that Payment.charRefunded might be != 0.
- PayPal (MAYBE) seems to give a lot of time if the user has no funds to complete payments.
  Thus we may get a larger payment sometime later (if the expired events by PayHook are ignored and the subscription not cancelled).