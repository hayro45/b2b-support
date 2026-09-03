package com.hayrettindal.support.ticket.api;

import com.hayrettindal.support.ticket.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
    @NotBlank @Size(max = 180) String title,
    @NotBlank @Size(max = 5000) String description,
    @NotNull TicketPriority priority
) {}
