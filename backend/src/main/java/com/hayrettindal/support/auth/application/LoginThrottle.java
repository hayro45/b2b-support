package com.hayrettindal.support.auth.application;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Small single-host fixed window: at most ten unsuccessful attempts per account in five minutes. */
@Component
public class LoginThrottle {
    private record Window(long expiresAt, int attempts) {}
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;

    public LoginThrottle() { this(Clock.systemUTC()); }
    LoginThrottle(Clock clock) { this.clock = clock; }

    public synchronized void attempt(String email) {
        long now = clock.millis();
        windows.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        Window window = windows.get(email);
        if (window != null && window.attempts() >= 10) throw new LoginRateLimitException();
        // Bound memory even if the attacker invents account names.
        if (window == null && windows.size() >= 10000) throw new LoginRateLimitException();
        windows.put(email, new Window(window == null ? now + 300000 : window.expiresAt(), window == null ? 1 : window.attempts() + 1));
    }

    public synchronized void success(String email) { windows.remove(email); }
}
