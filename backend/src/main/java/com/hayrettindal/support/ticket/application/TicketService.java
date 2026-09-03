package com.hayrettindal.support.ticket.application;

import com.hayrettindal.support.audit.infrastructure.AuditLogEntity;
import com.hayrettindal.support.audit.infrastructure.AuditLogRepository;
import com.hayrettindal.support.auth.domain.UserRole;
import com.hayrettindal.support.auth.infrastructure.AppUserEntity;
import com.hayrettindal.support.auth.infrastructure.AppUserRepository;
import com.hayrettindal.support.common.NotFoundException;
import com.hayrettindal.support.ticket.api.CreateTicketCommentRequest;
import com.hayrettindal.support.ticket.api.CreateTicketRequest;
import com.hayrettindal.support.ticket.api.TicketAssignmentHistoryResponse;
import com.hayrettindal.support.ticket.api.TicketCommentResponse;
import com.hayrettindal.support.ticket.api.TicketResponse;
import com.hayrettindal.support.ticket.domain.TicketPriority;
import com.hayrettindal.support.ticket.domain.TicketStatus;
import com.hayrettindal.support.ticket.infrastructure.TicketAssignmentHistoryEntity;
import com.hayrettindal.support.ticket.infrastructure.TicketAssignmentHistoryRepository;
import com.hayrettindal.support.ticket.infrastructure.TicketCommentEntity;
import com.hayrettindal.support.ticket.infrastructure.TicketCommentRepository;
import com.hayrettindal.support.ticket.infrastructure.TicketEntity;
import com.hayrettindal.support.ticket.infrastructure.TicketRepository;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS = new EnumMap<>(TicketStatus.class);

    static {
        TRANSITIONS.put(TicketStatus.OPEN, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED));
        TRANSITIONS.put(TicketStatus.IN_PROGRESS, Set.of(TicketStatus.WAITING_CUSTOMER, TicketStatus.RESOLVED, TicketStatus.CLOSED));
        TRANSITIONS.put(TicketStatus.WAITING_CUSTOMER, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, TicketStatus.CLOSED));
        TRANSITIONS.put(TicketStatus.RESOLVED, Set.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED));
        TRANSITIONS.put(TicketStatus.CLOSED, Set.of(TicketStatus.OPEN));
    }

    private final TicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketAssignmentHistoryRepository assignmentHistoryRepository;
    private final AuditLogRepository auditLogRepository;

    public TicketService(
        TicketRepository ticketRepository,
        AppUserRepository appUserRepository,
        TicketCommentRepository ticketCommentRepository,
        TicketAssignmentHistoryRepository assignmentHistoryRepository,
        AuditLogRepository auditLogRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.appUserRepository = appUserRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.assignmentHistoryRepository = assignmentHistoryRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public TicketResponse create(CreateTicketRequest request, UUID organizationId, UUID requesterUserId) {
        TicketEntity entity = new TicketEntity();
        entity.setOrganizationId(organizationId);
        entity.setTicketNo(generateTicketNo());
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setPriority(request.priority());
        entity.setStatus(TicketStatus.OPEN);
        entity.setRequesterUserId(requesterUserId);

        TicketEntity saved = ticketRepository.save(entity);
        writeAudit(organizationId, requesterUserId, "TICKET_CREATED", "TICKET", saved.getId(), "{\"status\":\"OPEN\"}");
        return map(saved);
    }

    public Page<TicketResponse> list(UUID organizationId, TicketStatus status, TicketPriority priority, String q, Pageable pageable) {
        Page<TicketEntity> page;
        if (status != null) {
            page = ticketRepository.findByOrganizationIdAndStatus(organizationId, status, pageable);
        } else if (priority != null) {
            page = ticketRepository.findByOrganizationIdAndPriority(organizationId, priority, pageable);
        } else if (q != null && !q.isBlank()) {
            page = ticketRepository.findByOrganizationIdAndTitleContainingIgnoreCase(organizationId, q.trim(), pageable);
        } else {
            page = ticketRepository.findByOrganizationId(organizationId, pageable);
        }

        return page.map(this::map);
    }

    public TicketResponse getById(UUID id, UUID organizationId) {
        TicketEntity entity = ticketRepository.findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new NotFoundException("Ticket not found: " + id));
        return map(entity);
    }

    public TicketResponse updateStatus(UUID id, UUID organizationId, UUID changedByUserId, TicketStatus targetStatus) {
        TicketEntity entity = ticketRepository.findByIdAndOrganizationId(id, organizationId)
            .orElseThrow(() -> new NotFoundException("Ticket not found: " + id));

        TicketStatus current = entity.getStatus();
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(targetStatus)) {
            throw new IllegalArgumentException("Invalid status transition: " + current + " -> " + targetStatus);
        }

        entity.setStatus(targetStatus);
        TicketEntity saved = ticketRepository.save(entity);
        writeAudit(
            organizationId,
            changedByUserId,
            "TICKET_STATUS_UPDATED",
            "TICKET",
            saved.getId(),
            "{\"from\":\"" + current + "\",\"to\":\"" + targetStatus + "\"}"
        );
        return map(saved);
    }

    public TicketResponse assign(UUID ticketId, UUID organizationId, UUID changedByUserId, UUID assigneeUserId) {
        TicketEntity ticket = ticketRepository.findByIdAndOrganizationId(ticketId, organizationId)
            .orElseThrow(() -> new NotFoundException("Ticket not found: " + ticketId));

        AppUserEntity assignee = appUserRepository.findById(assigneeUserId)
            .orElseThrow(() -> new NotFoundException("Assignee user not found"));

        if (!assignee.getOrganizationId().equals(organizationId)) {
            throw new IllegalArgumentException("Assignee is not in the same organization");
        }

        UUID previousAssigneeId = ticket.getAssigneeUserId();
        ticket.setAssigneeUserId(assigneeUserId);
        TicketEntity saved = ticketRepository.save(ticket);
        writeAssignmentHistory(saved.getId(), previousAssigneeId, assigneeUserId, changedByUserId);
        writeAudit(
            organizationId,
            changedByUserId,
            "TICKET_ASSIGNED",
            "TICKET",
            saved.getId(),
            "{\"fromAssignee\":\"" + previousAssigneeId + "\",\"toAssignee\":\"" + assigneeUserId + "\"}"
        );
        return map(saved);
    }

    public TicketCommentResponse addComment(
        UUID ticketId,
        UUID organizationId,
        UUID authorUserId,
        UserRole role,
        CreateTicketCommentRequest request
    ) {
        ticketRepository.findByIdAndOrganizationId(ticketId, organizationId)
            .orElseThrow(() -> new NotFoundException("Ticket not found: " + ticketId));

        if (request.internalNote() && role == UserRole.CUSTOMER) {
            throw new AccessDeniedException("Customers cannot create internal notes");
        }

        TicketCommentEntity comment = new TicketCommentEntity();
        comment.setTicketId(ticketId);
        comment.setAuthorUserId(authorUserId);
        comment.setBody(request.body());
        comment.setInternalNote(request.internalNote());

        TicketCommentEntity saved = ticketCommentRepository.save(comment);
        writeAudit(
            organizationId,
            authorUserId,
            "TICKET_COMMENT_ADDED",
            "TICKET",
            ticketId,
            "{\"internalNote\":" + request.internalNote() + "}"
        );
        return mapComment(saved);
    }

    public List<TicketCommentResponse> listComments(UUID ticketId, UUID organizationId, UserRole role, int size) {
        ticketRepository.findByIdAndOrganizationId(ticketId, organizationId)
            .orElseThrow(() -> new NotFoundException("Ticket not found: " + ticketId));

        List<TicketCommentEntity> comments = ticketCommentRepository.findByTicketIdOrderByCreatedAtDesc(
            ticketId,
            PageRequest.of(0, Math.max(1, Math.min(100, size)))
        );

        if (role == UserRole.CUSTOMER) {
            return comments.stream()
                .filter(comment -> !comment.isInternalNote())
                .map(this::mapComment)
                .toList();
        }

        return comments.stream().map(this::mapComment).toList();
    }

    public List<TicketAssignmentHistoryResponse> listAssignmentHistory(UUID ticketId, UUID organizationId) {
        ticketRepository.findByIdAndOrganizationId(ticketId, organizationId)
            .orElseThrow(() -> new NotFoundException("Ticket not found: " + ticketId));

        return assignmentHistoryRepository.findByTicketIdOrderByChangedAtDesc(ticketId)
            .stream()
            .map(this::mapAssignmentHistory)
            .toList();
    }

    private TicketResponse map(TicketEntity entity) {
        return new TicketResponse(
            entity.getId(),
            entity.getOrganizationId(),
            entity.getTicketNo(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getPriority(),
            entity.getStatus(),
            entity.getRequesterUserId(),
            entity.getAssigneeUserId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private TicketCommentResponse mapComment(TicketCommentEntity comment) {
        return new TicketCommentResponse(
            comment.getId(),
            comment.getTicketId(),
            comment.getAuthorUserId(),
            comment.getBody(),
            comment.isInternalNote(),
            comment.getCreatedAt()
        );
    }

    private TicketAssignmentHistoryResponse mapAssignmentHistory(TicketAssignmentHistoryEntity history) {
        return new TicketAssignmentHistoryResponse(
            history.getId(),
            history.getTicketId(),
            history.getFromUserId(),
            history.getToUserId(),
            history.getChangedByUserId(),
            history.getChangedAt()
        );
    }

    private void writeAssignmentHistory(UUID ticketId, UUID fromUserId, UUID toUserId, UUID changedByUserId) {
        TicketAssignmentHistoryEntity history = new TicketAssignmentHistoryEntity();
        history.setTicketId(ticketId);
        history.setFromUserId(fromUserId);
        history.setToUserId(toUserId);
        history.setChangedByUserId(changedByUserId);
        assignmentHistoryRepository.save(history);
    }

    private void writeAudit(
        UUID organizationId,
        UUID actorUserId,
        String actionType,
        String entityType,
        UUID entityId,
        String metadataJson
    ) {
        AuditLogEntity log = new AuditLogEntity();
        log.setOrganizationId(organizationId);
        log.setActorUserId(actorUserId);
        log.setActionType(actionType);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setMetadataJson(metadataJson);
        auditLogRepository.save(log);
    }

    private String generateTicketNo() {
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        LocalDate date = LocalDate.now();
        return "TCK-" + date.getYear() + "-" + random;
    }
}
