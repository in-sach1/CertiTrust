package com.digitalcertificate.controller;

import com.digitalcertificate.entity.Certificate;
import com.digitalcertificate.entity.CertificateStatus;
import com.digitalcertificate.entity.CertificateType;
import com.digitalcertificate.entity.Institution;
import com.digitalcertificate.entity.Role;
import com.digitalcertificate.entity.User;
import com.digitalcertificate.repository.CertificateRepository;
import com.digitalcertificate.repository.InstitutionRepository;
import com.digitalcertificate.repository.RevocationRepository;
import com.digitalcertificate.repository.UserRepository;
import com.digitalcertificate.util.CertificateHasher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:certitrust-security-tests;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=update",
        "app.bootstrap-admin.email=qa-admin@example.test",
        "app.bootstrap-admin.password=StrongPass123",
        "app.demo-data=false",
        "server.servlet.session.cookie.secure=true",
        "logging.level.org.apache.tomcat.util.net.Acceptor=OFF"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ApplicationSecurityIntegrationTest {
    private static final String ADMIN_EMAIL = "qa-admin@example.test";
    private static final String ADMIN_PASSWORD = "StrongPass123";

    @TempDir
    static Path tempDirectory;

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.upload-dir", () -> tempDirectory.resolve("uploads").toString());
        registry.add("app.storage.certificate-dir", () -> tempDirectory.resolve("certificates").toString());
        registry.add("app.storage.qr-dir", () -> tempDirectory.resolve("qr").toString());
    }

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository users;
    @Autowired private InstitutionRepository institutions;
    @Autowired private CertificateRepository certificates;
    @Autowired private RevocationRepository revocations;
    @Autowired private TestRestTemplate restTemplate;
    @LocalServerPort private int port;

    private User issuer;
    private User otherIssuer;

    @BeforeEach
    void createIsolatedTestAccounts() {
        Institution institution = new Institution();
        institution.setName("QA Institution " + UUID.randomUUID());
        institution = institutions.save(institution);
        issuer = createUser("issuer-" + UUID.randomUUID() + "@example.test", "IssuerPass123",
                Role.ISSUER, institution, true);
        otherIssuer = createUser("issuer-" + UUID.randomUUID() + "@example.test", "IssuerPass123",
                Role.ISSUER, institution, true);
    }

    @Test
    void apiAuthenticationAndRoleAuthorizationAreEnforced() throws Exception {
        mvc.perform(get("/api/certificates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        mvc.perform(get("/api/certificates").with(httpBasic(issuer.getEmail(), "IssuerPass123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        MockHttpSession issuerSession = login(issuer.getEmail(), "IssuerPass123");
        mvc.perform(get("/admin/users").session(issuerSession))
                .andExpect(status().isForbidden());

        MockHttpSession adminSession = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        mvc.perform(get("/admin/users").session(adminSession))
                .andExpect(status().isOk());
    }

    @Test
    void apiLoginRotatesAnExistingSessionId() throws Exception {
        MockHttpSession previousSession = new MockHttpSession();
        String oldSessionId = previousSession.getId();

        MvcResult result = mvc.perform(post("/api/auth/login")
                        .session(previousSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(issuer.getEmail(), "IssuerPass123")))
                .andExpect(status().isOk())
                .andReturn();

        assertNotEquals(oldSessionId, result.getRequest().getSession(false).getId());
    }

    @Test
    void realSessionCookieUsesConfiguredSecurityAttributes() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginJson(ADMIN_EMAIL, ADMIN_PASSWORD), headers),
                String.class);

        assertEquals(200, response.getStatusCode().value());
        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertTrue(cookie != null && cookie.contains("HttpOnly"));
        assertTrue(cookie != null && cookie.contains("Secure"));
        assertTrue(cookie != null && cookie.contains("SameSite=Lax"));
    }

    @Test
    void sessionAuthenticatedUnsafeApiRequestsRequireCsrf() throws Exception {
        MockHttpSession session = login(issuer.getEmail(), "IssuerPass123");
        String payload = certificateJson("Session CSRF test");

        mvc.perform(post("/api/certificates")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/certificates")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    void issuancePublicVerificationAndRevocationWorkEndToEnd() throws Exception {
        String certificateId = issue(issuer, "Issuance flow");

        mvc.perform(get("/api/certificates/{certificateId}", certificateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.certificateId").value(certificateId))
                .andExpect(jsonPath("$.data.recipientEmail").doesNotExist());
        mvc.perform(get("/api/certificates/{certificateId}/verify", certificateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));

        mvc.perform(post("/api/certificates/{certificateId}/revoke", certificateId)
                        .with(httpBasic(issuer.getEmail(), "IssuerPass123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"QA revocation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mvc.perform(get("/api/certificates/{certificateId}/verify", certificateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"))
                .andExpect(jsonPath("$.data.revocationReason").value("QA revocation"));
        assertEquals(1, revocations.count());
    }

    @Test
    void issuersCannotRevokeCertificatesOwnedByAnotherIssuer() throws Exception {
        String certificateId = issue(issuer, "Ownership test");

        mvc.perform(post("/api/certificates/{certificateId}/revoke", certificateId)
                        .with(httpBasic(otherIssuer.getEmail(), "IssuerPass123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Not my certificate\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mvc.perform(get("/api/certificates/{certificateId}/verify", certificateId))
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
    }

    @Test
    void invalidInputAndRegistrationRoleEscalationAreRejected() throws Exception {
        mvc.perform(post("/api/certificates")
                        .with(httpBasic(issuer.getEmail(), "IssuerPass123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientName\":\"\",\"issueDate\":\"not-a-date\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Attacker\",\"email\":\"admin-attempt@example.test\","
                                + "\"password\":\"StrongPass123\",\"confirmPassword\":\"StrongPass123\","
                                + "\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        String overByteLimit = "é".repeat(36) + "1A";
        String json = objectMapper.writeValueAsString(
                new RegisterPayload("Unicode User", "unicode-" + UUID.randomUUID() + "@example.test",
                        overByteLimit, overByteLimit, "VERIFIER", null));
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unsupportedApiContentTypeReturns415InsteadOfAnInternalServerError() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(loginJson(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void missingCertificateReturnsNotFoundWithApiEnvelope() throws Exception {
        mvc.perform(get("/api/certificates/CERT-2026-999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void adminCanCreateUsersAndUploadOnlyValidImagesWithCsrfProtection() throws Exception {
        MockHttpSession adminSession = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mvc.perform(post("/admin/users")
                        .session(adminSession)
                        .param("name", "Created By Admin")
                        .param("email", "new-user-" + UUID.randomUUID() + "@example.test")
                        .param("password", "CreatedPass123")
                        .param("role", "VERIFIER"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/admin/users")
                        .session(adminSession)
                        .with(csrf())
                        .param("name", "Created By Admin")
                        .param("email", "new-user-" + UUID.randomUUID() + "@example.test")
                        .param("password", "CreatedPass123")
                        .param("role", "VERIFIER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        MockMultipartFile validLogo = new MockMultipartFile(
                "logo", "logo.png", "image/png", png(32, 24));
        MvcResult upload = mvc.perform(multipart("/admin/institutions")
                        .file(validLogo)
                        .session(adminSession)
                        .with(csrf())
                        .param("name", "Upload QA " + UUID.randomUUID())
                        .param("website", "https://example.test"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        Institution uploaded = institutions.findAll().stream()
                .filter(i -> i.getLogoPath() != null && i.getLogoPath().endsWith(".png"))
                .findFirst().orElseThrow();
        assertTrue(Files.isRegularFile(tempDirectory.resolve("uploads").resolve(uploaded.getLogoPath())));
        mvc.perform(get("/logo/{id}", uploaded.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));

        MockMultipartFile invalidLogo = new MockMultipartFile(
                "logo", "fake.png", "image/png", "not a PNG".getBytes());
        int institutionCount = institutions.findAll().size();
        mvc.perform(multipart("/admin/institutions")
                        .file(invalidLogo)
                        .session(adminSession)
                        .with(csrf())
                        .param("name", "Rejected Upload " + UUID.randomUUID()))
                .andExpect(status().is3xxRedirection());
        assertEquals(institutionCount, institutions.findAll().size());
    }

    @Test
    void persistedLegacyCertificateFingerprintStillVerifiesWithSchemaUpdatesEnabled() throws Exception {
        LocalDate issueDate = LocalDate.of(2026, 5, 20);
        Certificate legacy = new Certificate();
        legacy.setCertificateId("CERT-2026-900001");
        legacy.setRecipientName("Aarav Sharma");
        legacy.setRecipientEmail("aarav@example.test");
        legacy.setCourse("Java");
        legacy.setCertificateType(CertificateType.COURSE_COMPLETION);
        legacy.setInstitution(issuer.getInstitution());
        legacy.setInstitutionName("Demo Institute of Technology");
        legacy.setIssuer(issuer);
        legacy.setIssueDate(issueDate);
        legacy.setGrade("A");
        legacy.setStatus(CertificateStatus.ACTIVE);
        legacy.setCertificateHash(CertificateHasher.compute(legacy.getCertificateId(), "Aarav Sharma", "Java",
                "Demo Institute of Technology", issueDate, "COURSE_COMPLETION", null, "A"));
        certificates.saveAndFlush(legacy);

        mvc.perform(get("/api/certificates/{certificateId}/verify", legacy.getCertificateId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
    }

    private User createUser(String email, String password, Role role, Institution institution, boolean enabled) {
        User user = new User();
        user.setName("QA " + role);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setInstitution(institution);
        user.setEnabled(enabled);
        return users.save(user);
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String issue(User user, String course) throws Exception {
        MvcResult result = mvc.perform(post("/api/certificates")
                        .with(httpBasic(user.getEmail(), "IssuerPass123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(certificateJson(course)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("certificateId").asText();
    }

    private static String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String certificateJson(String course) {
        return "{\"recipientName\":\"QA Recipient\",\"recipientEmail\":\"recipient@example.test\","
                + "\"course\":\"" + course + "\",\"certificateType\":\"COURSE_COMPLETION\","
                + "\"issueDate\":\"" + LocalDate.now().minusDays(1) + "\",\"grade\":\"A\"}";
    }

    private static byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private record RegisterPayload(String name, String email, String password, String confirmPassword,
                                   String role, String institutionName) { }
}
