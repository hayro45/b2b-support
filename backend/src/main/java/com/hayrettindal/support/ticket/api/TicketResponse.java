package com.hayrettindal.support.ticket.api;

import com.hayrettindal.support.ticket.domain.TicketPriority;
import com.hayrettindal.support.ticket.domain.TicketStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
    UUID id,
    UUID organizationId,
    String ticketNo,
    String title,
    String description,
    TicketPriority priority,
    TicketStatus status,
    UUID requesterUserId,
    UUID assigneeUserId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
