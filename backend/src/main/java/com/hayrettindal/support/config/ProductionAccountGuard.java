package com.hayrettindal.support.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("prod")
public class ProductionAccountGuard implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public ProductionAccountGuard(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbc.update("UPDATE app_users SET is_active = false WHERE lower(email) LIKE '%@demo.local'");
        Integer unsafe = jdbc.queryForObject("SELECT count(*) FROM app_users WHERE is_active AND password_hash NOT LIKE '{bcrypt}%'", Integer.class);
        if (unsafe != null && unsafe > 0) throw new IllegalStateException("Production accounts require BCrypt passwords");
    }
}
