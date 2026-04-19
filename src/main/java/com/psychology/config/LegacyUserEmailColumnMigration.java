package com.psychology.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time compatibility migration for legacy schemas where users login email
 * was stored in the "phone" column.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyUserEmailColumnMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("users")) return;

            boolean hasPhone = columnExists("users", "phone");
            boolean hasEmail = columnExists("users", "email");

            if (hasPhone && !hasEmail) {
                jdbcTemplate.execute("ALTER TABLE users RENAME COLUMN phone TO email");
                log.info("DB migration applied: users.phone renamed to users.email");
                return;
            }

            if (hasPhone) {
                jdbcTemplate.execute("""
                    UPDATE users
                    SET email = phone
                    WHERE (email IS NULL OR email = '') AND phone IS NOT NULL
                    """);
                jdbcTemplate.execute("ALTER TABLE users DROP COLUMN phone");
                log.info("DB migration applied: users.phone data merged into users.email and legacy column removed");
            }
        } catch (Exception e) {
            log.warn("Legacy users.phone -> users.email migration skipped: {}", e.getMessage());
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = ?
            """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
            """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }
}
