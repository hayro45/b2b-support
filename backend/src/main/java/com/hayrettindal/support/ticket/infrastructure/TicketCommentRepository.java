package com.hayrettindal.support.ticket.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketCommentEntity, UUID> {

    List<TicketCommentEntity> findByTicketIdOrderByCreatedAtDesc(UUID ticketId, Pageable pageable);
}
