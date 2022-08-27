package com.osiris.payhook.paypal;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.osiris.autoplug.core.json.exceptions.HttpErrorException;
import com.osiris.autoplug.core.json.exceptions.WrongJsonTypeException;
import com.paypal.base.rest.PayPalRESTException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class MyPayPalTest {

    @Test
    void getWebhooks() throws IOException, HttpErrorException, PayPalRESTException, WrongJsonTypeException {
        //TODO REMOVE
        MyPayPal myPayPal = new MyPayPal("Ack-AHXexQWdeOHtVAeUBkjBhNlrlQnQO-cqL8RI02wgDTL9BPsmooB1tlBGBNQsEYwlgC4GrdCYiVhP",
                "EOfcNbJMiPeCljn5cF_xnLtehdAj8lN4TNjAa19Rh9WbcHKQz88q8tx6gNCHIPlfC5p777fELSB8Ct3G",
                MyPayPal.Mode.SANDBOX);
        //System.out.println(myPayPal.accessToken);
        JsonArray webhooks = myPayPal.getWebhooks();
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(webhooks));
    }
}