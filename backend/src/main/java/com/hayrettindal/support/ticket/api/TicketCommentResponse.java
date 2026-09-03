package com.hayrettindal.support.ticket.api;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketCommentResponse(
    UUID id,
    UUID ticketId,
    UUID authorUserId,
    String body,
    boolean internalNote,
    LocalDateTime createdAt
) {}
