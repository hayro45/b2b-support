package com.hayrettindal.support.auth.api;

import java.util.UUID;

public record AgentResponse(UUID id, String fullName, String email) {}
