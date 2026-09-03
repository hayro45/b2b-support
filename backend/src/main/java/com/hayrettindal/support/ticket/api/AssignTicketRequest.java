package com.hayrettindal.support.ticket.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignTicketRequest(@NotNull UUID assigneeUserId) {}
