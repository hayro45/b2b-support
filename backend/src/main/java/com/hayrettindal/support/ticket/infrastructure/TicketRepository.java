package com.hayrettindal.support.ticket.infrastructure;

import com.hayrettindal.support.ticket.domain.TicketPriority;
import com.hayrettindal.support.ticket.domain.TicketStatus;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<TicketEntity, UUID> {

    Page<TicketEntity> findByOrganizationIdAndStatus(UUID organizationId, TicketStatus status, Pageable pageable);

    Page<TicketEntity> findByOrganizationIdAndPriority(UUID organizationId, TicketPriority priority, Pageable pageable);

    Page<TicketEntity> findByOrganizationIdAndTitleContainingIgnoreCase(
        UUID organizationId,
        String query,
        Pageable pageable
    );

    Page<TicketEntity> findByOrganizationId(UUID organizationId, Pageable pageable);

    Optional<TicketEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
