package com.apptitle.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Opt-in, one-time presentation reset. This bean is absent during normal app
 * startup and is removed from the repository after the maintenance job runs.
 */
@Component
@ConditionalOnProperty(name = "app.maintenance.presentation-reset", havingValue = "true")
public class PresentationDataResetRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PresentationDataResetRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurableApplicationContext applicationContext;

    public PresentationDataResetRunner(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            ConfigurableApplicationContext applicationContext
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long usersBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);

        transactionTemplate.executeWithoutResult(status ->
                jdbcTemplate.execute("TRUNCATE TABLE users CASCADE"));

        Long usersAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        LOGGER.warn("Presentation reset completed: users before={}, users after={}", usersBefore, usersAfter);

        int exitCode = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(exitCode);
    }
}
