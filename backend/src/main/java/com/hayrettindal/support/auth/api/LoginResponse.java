package com.hayrettindal.support.auth.api;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds
) {}
