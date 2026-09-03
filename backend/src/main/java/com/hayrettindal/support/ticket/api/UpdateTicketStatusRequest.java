package com.hayrettindal.support.ticket.api;

import com.hayrettindal.support.ticket.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(@NotNull TicketStatus status) {}
