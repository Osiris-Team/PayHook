package com.osiris.payhook;

import org.junit.jupiter.api.Test;

class PayHookTest {

    @Test
    void createOrder() throws Exception {
        TestUtils.fetchCrendentials();
        PayHook p = new PayHook("payhook", TestUtils.dbUrl, TestUtils.dbUser, TestUtils.dbPassword);
        // TODO
    }
}