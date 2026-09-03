package com.hayrettindal.support.common;

import java.time.LocalDateTime;

public record ApiErrorResponse(
    String code,
    String message,
    LocalDateTime timestamp
) {}
