package com.hayrettindal.support.ticket.api;

import com.hayrettindal.support.auth.application.AuthenticatedUser;
import com.hayrettindal.support.ticket.application.TicketService;
import com.hayrettindal.support.ticket.domain.TicketPriority;
import com.hayrettindal.support.ticket.domain.TicketStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(
        @Valid @RequestBody CreateTicketRequest request,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ticketService.create(request, user.organizationId(), user.userId());
    }

    @GetMapping
    public Page<TicketResponse> list(
        @RequestParam(required = false) TicketStatus status,
        @RequestParam(required = false) TicketPriority priority,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        if (page < 0 || page > 10000 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be 0..10000 and size 1..100");
        }
        String[] sortParts = sort.split(",", -1);
        if (sortParts.length != 2 || !java.util.Set.of("createdAt", "updatedAt", "ticketNo", "title", "status", "priority").contains(sortParts[0])
            || !(sortParts[1].equalsIgnoreCase("asc") || sortParts[1].equalsIgnoreCase("desc"))) {
            throw new IllegalArgumentException("Invalid sort field or direction");
        }
        String sortField = sortParts[0];
        Sort.Direction direction = Sort.Direction.fromString(sortParts[1]);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField).and(Sort.by("id")));
        return ticketService.list(user.organizationId(), status, priority, q, pageable);
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ticketService.getById(id, user.organizationId());
    }

    @PatchMapping("/{id}/status")
    public TicketResponse updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateTicketStatusRequest request,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ticketService.updateStatus(id, user.organizationId(), user.userId(), request.status());
    }

    @PatchMapping("/{id}/assign")
    public TicketResponse assign(
        @PathVariable UUID id,
        @Valid @RequestBody AssignTicketRequest request,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ticketService.assign(id, user.organizationId(), user.userId(), request.assigneeUserId());
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketCommentResponse addComment(
        @PathVariable UUID id,
        @Valid @RequestBody CreateTicketCommentRequest request,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ticketService.addComment(id, user.organizationId(), user.userId(), user.role(), request);
    }

    @GetMapping("/{id}/comments")
    public List<TicketCommentResponse> listComments(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ticketService.listComments(id, user.organizationId(), user.role(), size);
    }

    @GetMapping("/{id}/assignment-history")
    public List<TicketAssignmentHistoryResponse> listAssignmentHistory(
        @PathVariable UUID id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ticketService.listAssignmentHistory(id, user.organizationId());
    }
}
