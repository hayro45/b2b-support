package com.hayrettindal.support.auth.api;

import com.hayrettindal.support.auth.domain.UserRole;
import java.util.UUID;

public record MeResponse(
    UUID userId,
    UUID organizationId,
    String email,
    String fullName,
    UserRole role
) {}
