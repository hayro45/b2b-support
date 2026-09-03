package com.hayrettindal.support.ticket.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketAssignmentHistoryRepository extends JpaRepository<TicketAssignmentHistoryEntity, UUID> {

    List<TicketAssignmentHistoryEntity> findByTicketIdOrderByChangedAtDesc(UUID ticketId);
}
