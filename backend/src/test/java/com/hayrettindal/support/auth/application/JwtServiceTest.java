package com.hayrettindal.support.auth.application;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JwtServiceTest {
    private static final String DEV_KEY = "ZGV2LXNlY3JldC1jaGFuZ2UtdGhpcy1pbi1wcm9kdWN0aW9uLTEyMw==";

    @Test
    void productionRejectsKnownDevKeyAndEmptyOrShortKeys() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        assertThrows(IllegalArgumentException.class, () -> new JwtService(DEV_KEY, 3600, env));
        assertThrows(IllegalArgumentException.class, () -> new JwtService("", 3600, env));
        assertThrows(RuntimeException.class, () -> new JwtService("c2hvcnQ=", 3600, env));
    }

    @Test
    void expiryIsBoundedAndDevCanUseDevKey() {
        MockEnvironment env = new MockEnvironment();
        assertThrows(IllegalArgumentException.class, () -> new JwtService(DEV_KEY, 0, env));
        assertDoesNotThrow(() -> new JwtService(DEV_KEY, 3600, env));
    }
}
