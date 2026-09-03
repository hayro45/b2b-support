package com.hayrettindal.support.auth.application;

import com.hayrettindal.support.auth.domain.UserRole;
import java.util.UUID;

public record AuthenticatedUser(
    UUID userId,
    UUID organizationId,
    String email,
    UserRole role
) {}
