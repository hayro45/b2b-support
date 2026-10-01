package com.hayrettindal.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SupportApiIntegrationTest {

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Autowired
    private com.hayrettindal.support.ticket.application.TicketService tickets;

    @Autowired
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private com.hayrettindal.support.audit.infrastructure.AuditLogRepository audit;

    private static final java.util.UUID ORG = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final java.util.UUID AGENT = java.util.UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final java.util.UUID CUSTOMER = java.util.UUID.fromString("00000000-0000-0000-0000-000000000010");

    private String token(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "demo12345"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    private java.util.UUID firstTicket() {
        return jdbc.queryForObject("SELECT id FROM tickets ORDER BY ticket_no LIMIT 1", java.util.UUID.class);
    }

    @Test
    void queryBoundsAndMalformedEnumsReturn400() throws Exception {
        String auth = token("agent@demo.local");
        for (String query : java.util.List.of("size=101", "page=-1", "sort=passwordHash,desc", "sort=title,nonsense", "status=WRONG")) {
            mockMvc.perform(get("/api/v1/tickets?" + query).header("Authorization", auth)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void staffEndpointsAndMutationsRejectCustomer() throws Exception {
        String auth = token("customer@demo.local");
        mockMvc.perform(get("/api/v1/auth/agents").header("Authorization", auth)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/tickets/" + firstTicket() + "/assignment-history").header("Authorization", auth)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/tickets/" + firstTicket() + "/status").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/tickets/" + firstTicket() + "/comments").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"secret\",\"internalNote\":true}")).andExpect(status().isForbidden());
    }

    @Test
    void agentDirectoryAndAssignmentExcludeCustomers() throws Exception {
        String auth = token("agent@demo.local");
        mockMvc.perform(get("/api/v1/auth/agents").header("Authorization", auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value("agent@demo.local")).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(patch("/api/v1/tickets/" + firstTicket() + "/assign").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("{\"assigneeUserId\":\"" + CUSTOMER + "\"}")).andExpect(status().isBadRequest());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void inactiveUserCannotUsePreviouslyIssuedToken() throws Exception {
        String auth = token("agent@demo.local");
        jdbc.update("UPDATE app_users SET is_active=false WHERE id=?", AGENT);
        entityManager.clear();
        mockMvc.perform(get("/api/v1/tickets").header("Authorization", auth)).andExpect(status().isUnauthorized());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void combinedFiltersUseAllConditions() throws Exception {
        String auth = token("agent@demo.local");
        jdbc.update("UPDATE tickets SET status='OPEN', priority='LOW', title='match search'");
        jdbc.update("UPDATE tickets SET priority='HIGH' WHERE id=?", firstTicket());
        mockMvc.perform(get("/api/v1/tickets?status=OPEN&priority=HIGH&q=match").header("Authorization", auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/tickets?status=CLOSED&priority=HIGH&q=match").header("Authorization", auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void internalNotesAreExcludedBeforeCustomerLimit() {
        java.util.UUID id = firstTicket();
        tickets.addComment(id, ORG, AGENT, com.hayrettindal.support.auth.domain.UserRole.AGENT,
            new com.hayrettindal.support.ticket.api.CreateTicketCommentRequest("public", false));
        tickets.addComment(id, ORG, AGENT, com.hayrettindal.support.auth.domain.UserRole.AGENT,
            new com.hayrettindal.support.ticket.api.CreateTicketCommentRequest("secret", true));
        var comments = tickets.listComments(id, ORG, com.hayrettindal.support.auth.domain.UserRole.CUSTOMER, 1);
        org.junit.jupiter.api.Assertions.assertEquals(1, comments.size());
        org.junit.jupiter.api.Assertions.assertFalse(comments.getFirst().internalNote());
    }

    @Test
    void auditFailureRollsBackTicketCreation() {
        Integer before = jdbc.queryForObject("SELECT count(*) FROM tickets", Integer.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("audit failed")).when(audit).save(org.mockito.ArgumentMatchers.any());
        try {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> tickets.create(
                new com.hayrettindal.support.ticket.api.CreateTicketRequest("rollback ticket", "description", com.hayrettindal.support.ticket.domain.TicketPriority.HIGH), ORG, AGENT));
            org.junit.jupiter.api.Assertions.assertEquals(before, jdbc.queryForObject("SELECT count(*) FROM tickets", Integer.class));
        } finally {
            org.mockito.Mockito.reset(audit);
        }
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void anotherOrganizationCannotReadTicket() {
        org.junit.jupiter.api.Assertions.assertThrows(com.hayrettindal.support.common.NotFoundException.class,
            () -> tickets.getById(firstTicket(), java.util.UUID.randomUUID()));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void secondTenantJwtCannotReadOrMutateFirstTenantTickets() throws Exception {
        java.util.UUID otherOrg = java.util.UUID.randomUUID();
        java.util.UUID otherAgent = java.util.UUID.randomUUID();
        jdbc.update("INSERT INTO organizations(id,name) VALUES (?,?)", otherOrg, "Other Org");
        jdbc.update("INSERT INTO app_users(id,organization_id,full_name,email,role,is_active,password_hash) SELECT ?,?,'Other Agent','other@tenant.test','AGENT',true,password_hash FROM app_users WHERE id=?", otherAgent, otherOrg, AGENT);
        String auth = token("other@tenant.test");
        java.util.UUID id = firstTicket();
        String originalStatus = jdbc.queryForObject("SELECT status FROM tickets WHERE id=?", String.class, id);
        mockMvc.perform(get("/api/v1/tickets").header("Authorization", auth)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        for (String suffix : java.util.List.of("", "/comments", "/assignment-history")) {
            mockMvc.perform(get("/api/v1/tickets/" + id + suffix).header("Authorization", auth)).andExpect(status().isNotFound());
        }
        mockMvc.perform(patch("/api/v1/tickets/" + id + "/status").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}")).andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/tickets/" + id + "/assign").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("{\"assigneeUserId\":\"" + otherAgent + "\"}")).andExpect(status().isNotFound());
        org.junit.jupiter.api.Assertions.assertEquals(originalStatus, jdbc.queryForObject("SELECT status FROM tickets WHERE id=?", String.class, id));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void issuedAgentTokenImmediatelyLosesPrivilegesOnDemotion() throws Exception {
        String auth = token("agent@demo.local");
        jdbc.update("UPDATE app_users SET role='CUSTOMER' WHERE id=?", AGENT);
        entityManager.clear();
        mockMvc.perform(get("/api/v1/auth/agents").header("Authorization", auth)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/tickets/" + firstTicket() + "/status").header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}")).andExpect(status().isForbidden());
    }

    @Test
    void assignmentAuditFailureRollsBackTicketAndHistory() {
        java.util.UUID id = firstTicket();
        var originalAssignee = jdbc.queryForObject("SELECT assignee_user_id FROM tickets WHERE id=?", java.util.UUID.class, id);
        Integer historyCount = jdbc.queryForObject("SELECT count(*) FROM ticket_assignment_history WHERE ticket_id=?", Integer.class, id);
        Long version = jdbc.queryForObject("SELECT version FROM tickets WHERE id=?", Long.class, id);
        org.mockito.Mockito.doThrow(new IllegalStateException("audit failed")).when(audit).save(org.mockito.ArgumentMatchers.any());
        try {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> tickets.assign(id, ORG, AGENT, AGENT));
            org.junit.jupiter.api.Assertions.assertEquals(originalAssignee, jdbc.queryForObject("SELECT assignee_user_id FROM tickets WHERE id=?", java.util.UUID.class, id));
            org.junit.jupiter.api.Assertions.assertEquals(version, jdbc.queryForObject("SELECT version FROM tickets WHERE id=?", Long.class, id));
            org.junit.jupiter.api.Assertions.assertEquals(historyCount, jdbc.queryForObject("SELECT count(*) FROM ticket_assignment_history WHERE ticket_id=?", Integer.class, id));
        } finally { org.mockito.Mockito.reset(audit); }
    }

    @Test
    void repeatedInvalidLoginsReturn429AndRetryAfter() throws Exception {
        String body = "{\"email\":\"rate-limit@example.com\",\"password\":\"invalid-password\"}";
        for (int attempt = 0; attempt < 10; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isTooManyRequests())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Retry-After", "300"));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void productionGuardDisablesSeededAccounts() throws Exception {
        new com.hayrettindal.support.config.ProductionAccountGuard(jdbc).run(null);
        entityManager.clear();
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM app_users WHERE is_active AND email LIKE '%@demo.local'", Integer.class));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"agent@demo.local\",\"password\":\"demo12345\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void metricsAreNotAvailableToCustomers() throws Exception {
        mockMvc.perform(get("/actuator/metrics").header("Authorization", token("customer@demo.local"))).andExpect(status().isForbidden());
    }

    @Test
    void unknownRoutesReturn404AndUnsupportedMethodsReturn405() throws Exception {
        String auth = token("agent@demo.local");
        mockMvc.perform(get("/api/v1/missing-resource").header("Authorization", auth)).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/auth/me").header("Authorization", auth))
            .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Allow", "GET"));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void statusRulesAndAssignmentRecordHistoryAtomically() {
        var created = tickets.create(new com.hayrettindal.support.ticket.api.CreateTicketRequest("workflow test", "description", com.hayrettindal.support.ticket.domain.TicketPriority.HIGH), ORG, AGENT);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> tickets.updateStatus(created.id(), ORG, AGENT, com.hayrettindal.support.ticket.domain.TicketStatus.RESOLVED));
        var changed = tickets.updateStatus(created.id(), ORG, AGENT, com.hayrettindal.support.ticket.domain.TicketStatus.IN_PROGRESS);
        org.junit.jupiter.api.Assertions.assertEquals(com.hayrettindal.support.ticket.domain.TicketStatus.IN_PROGRESS, changed.status());
        org.junit.jupiter.api.Assertions.assertFalse(changed.updatedAt().isBefore(created.updatedAt()));
        var assigned = tickets.assign(created.id(), ORG, AGENT, AGENT);
        org.junit.jupiter.api.Assertions.assertEquals(AGENT, assigned.assigneeUserId());
        org.junit.jupiter.api.Assertions.assertEquals(1, tickets.listAssignmentHistory(created.id(), ORG).size());
        org.junit.jupiter.api.Assertions.assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE entity_id=?", Integer.class, created.id()));
    }

    @Test
    void legacyPasswordsAreUpgradedToBcrypt() {
        String hash = jdbc.queryForObject("SELECT password_hash FROM app_users WHERE id=?", String.class, AGENT);
        org.junit.jupiter.api.Assertions.assertTrue(hash.startsWith("{bcrypt}"));
    }

    @Test
    void simultaneousUpdatesCannotSilentlyOverwriteTicket() {
        var first = entityManagerFactory.createEntityManager();
        var second = entityManagerFactory.createEntityManager();
        java.util.UUID id = firstTicket();
        String original = jdbc.queryForObject("SELECT title FROM tickets WHERE id=?", String.class, id);
        try {
            first.getTransaction().begin();
            second.getTransaction().begin();
            var a = first.find(com.hayrettindal.support.ticket.infrastructure.TicketEntity.class, id);
            var b = second.find(com.hayrettindal.support.ticket.infrastructure.TicketEntity.class, id);
            a.setTitle("first edit");
            first.getTransaction().commit();
            b.setTitle("stale edit");
            org.junit.jupiter.api.Assertions.assertThrows(jakarta.persistence.OptimisticLockException.class, second::flush);
            second.getTransaction().rollback();
            org.junit.jupiter.api.Assertions.assertEquals("first edit", jdbc.queryForObject("SELECT title FROM tickets WHERE id=?", String.class, id));
        } finally {
            if (first.getTransaction().isActive()) first.getTransaction().rollback();
            if (second.getTransaction().isActive()) second.getTransaction().rollback();
            first.close();
            second.close();
            jdbc.update("UPDATE tickets SET title=? WHERE id=?", original, id);
        }
    }

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
        .withDatabaseName("support_test")
        .withUsername("support_test")
        .withPassword("support_test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void loginAndListTicketsWithJwt() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "agent@demo.local",
                    "password", "demo12345"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(loginResponse);
        String token = responseJson.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/tickets")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void ticketListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
