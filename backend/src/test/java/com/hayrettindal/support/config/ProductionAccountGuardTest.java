package com.hayrettindal.support.config;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ProductionAccountGuardTest {
    @Test
    void disablesKnownDemoAccountsAndRejectsUnsafeProductionPasswords() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        assertThrows(IllegalStateException.class, () -> new ProductionAccountGuard(jdbc).run(null));
        verify(jdbc).update("UPDATE app_users SET is_active = false WHERE lower(email) LIKE '%@demo.local'");
    }
}
