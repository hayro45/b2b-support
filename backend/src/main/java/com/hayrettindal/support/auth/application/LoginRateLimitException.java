package com.hayrettindal.support.auth.application;

public class LoginRateLimitException extends RuntimeException {
    public LoginRateLimitException() { super("Too many login attempts; try again in five minutes"); }
}
