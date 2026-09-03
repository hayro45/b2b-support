package com.hayrettindal.support.ticket.api;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketAssignmentHistoryResponse(
    UUID id,
    UUID ticketId,
    UUID fromUserId,
    UUID toUserId,
    UUID changedByUserId,
    LocalDateTime changedAt
) {}
