package com.osiris.payhook;

import com.osiris.payhook.exceptions.ParseBodyException;
import org.junit.jupiter.api.Test;

class PayHookTest {

    @Test
    void parseAndGetBody() throws ParseBodyException {
        String sampleBodyString = "{\n" +
                "  \"webhooks\": [\n" +
                "    {\n" +
                "      \"id\": \"40Y916089Y8324740\",\n" +
                "      \"url\": \"https://example.com/example_webhook\",\n" +
                "      \"event_types\": [\n" +
                "        {\n" +
                "          \"name\": \"PAYMENT.AUTHORIZATION.CREATED\",\n" +
                "          \"description\": \"A payment authorization was created.\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"PAYMENT.AUTHORIZATION.VOIDED\",\n" +
                "          \"description\": \"A payment authorization was voided.\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"links\": [\n" +
                "        {\n" +
                "          \"href\": \"https://api-m.paypal.com/v1/notifications/webhooks/40Y916089Y8324740\",\n" +
                "          \"rel\": \"self\",\n" +
                "          \"method\": \"GET\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"href\": \"https://api-m.paypal.com/v1/notifications/webhooks/40Y916089Y8324740\",\n" +
                "          \"rel\": \"update\",\n" +
                "          \"method\": \"PATCH\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"href\": \"https://api-m.paypal.com/v1/notifications/webhooks/40Y916089Y8324740\",\n" +
                "          \"rel\": \"delete\",\n" +
                "          \"method\": \"DELETE\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"0EH40505U7160970P\",\n" +
                "      \"url\": \"https://example.com/another_example_webhook\",\n" +
                "      \"event_types\": [\n" +
                "        {\n" +
                "          \"name\": \"PAYMENT.AUTHORIZATION.CREATED\",\n" +
                "          \"description\": \"A payment authorization was created.\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"name\": \"PAYMENT.AUTHORIZATION.VOIDED\",\n" +
                "          \"description\": \"A payment authorization was voided.\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"links\": [\n" +
                "        {\n" +
                "          \"href\": \"https://api-m.paypal.com/v1/notifications/webhooks/0EH40505U7160970P\",\n" +
                "          \"rel\": \"self\",\n" +
                "          \"method\": \"GET\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"href\": \"https://api-m.paypal.com/v1/notifications/webhooks/0EH40505U7160970P\",\n" +
                "          \"rel\": \"update\",\n" +
                "          \"method\": \"PATCH\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"href\": \"https://api-m.paypal.com/v1/notifications/webhooks/0EH40505U7160970P\",\n" +
                "          \"rel\": \"delete\",\n" +
                "          \"method\": \"DELETE\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        new PayHook(PayHookValidationType.OFFLINE).parseAndGetBody(sampleBodyString);
    }
}