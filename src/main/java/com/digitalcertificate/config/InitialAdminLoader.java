package com.digitalcertificate.config;

import com.digitalcertificate.entity.Role;
import com.digitalcertificate.entity.User;
import com.digitalcertificate.repository.UserRepository;
import com.digitalcertificate.util.Patterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/** Creates an administrator only when credentials are explicitly configured and the database is empty. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InitialAdminLoader implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InitialAdminLoader.class);

    private final InitialAdminProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public InitialAdminLoader(InitialAdminProperties properties, UserRepository users, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.email() == null ? "" : properties.email().trim();
        String password = properties.password() == null ? "" : properties.password();
        if (!StringUtils.hasText(email) && !StringUtils.hasText(password)) {
            return;
        }
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("Both INITIAL_ADMIN_EMAIL and INITIAL_ADMIN_PASSWORD must be configured together.");
        }
        if (!email.matches("(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$")) {
            throw new IllegalStateException("INITIAL_ADMIN_EMAIL must be a valid email address.");
        }
        if (!Patterns.isValidPassword(password)) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD must meet the configured password requirements.");
        }
        if (users.count() > 0) {
            log.info("Initial administrator bootstrap skipped because the database already contains users.");
            return;
        }

        User admin = new User();
        admin.setName("Administrator");
        admin.setEmail(email.toLowerCase(Locale.ROOT));
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        users.save(admin);
        log.info("Initial administrator account created for {}", admin.getEmail());
    }
}
