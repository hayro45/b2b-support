package com.hayrettindal.support.auth.application;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {
    @Test
    void blocksEleventhAttemptAndSuccessfulLoginResetsAccount() {
        LoginThrottle throttle = new LoginThrottle(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        for (int i = 0; i < 10; i++) throttle.attempt("test@example.com");
        assertThrows(LoginRateLimitException.class, () -> throttle.attempt("test@example.com"));
        assertDoesNotThrow(() -> throttle.attempt("other@example.com"));
        throttle.success("test@example.com");
        assertDoesNotThrow(() -> throttle.attempt("test@example.com"));
    }
}
