package com.digitalcertificate.config;

import com.digitalcertificate.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:initial-admin-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.bootstrap-admin.email=initial-admin@example.com",
        "app.bootstrap-admin.password=StrongPass123",
        "app.storage.upload-dir=./target/test-storage/uploads",
        "app.storage.certificate-dir=./target/test-storage/certificates",
        "app.storage.qr-dir=./target/test-storage/qr"
})
class InitialAdminBootstrapIntegrationTest {
    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void applicationBootstrapsFirstAdminWithEncodedPassword() {
        var admin = users.findByEmailIgnoreCase("initial-admin@example.com").orElseThrow();

        assertTrue(admin.getPassword().startsWith("$2"));
        assertTrue(passwordEncoder.matches("StrongPass123", admin.getPassword()));
    }
}
