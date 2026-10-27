package com.digitalcertificate.config;

import com.digitalcertificate.dto.CertificateRequest;
import com.digitalcertificate.entity.Certificate;
import com.digitalcertificate.entity.CertificateStatus;
import com.digitalcertificate.entity.CertificateType;
import com.digitalcertificate.entity.Institution;
import com.digitalcertificate.entity.Role;
import com.digitalcertificate.entity.User;
import com.digitalcertificate.entity.VerificationLog;
import com.digitalcertificate.entity.VerificationStatus;
import com.digitalcertificate.repository.CertificateRepository;
import com.digitalcertificate.repository.InstitutionRepository;
import com.digitalcertificate.repository.UserRepository;
import com.digitalcertificate.repository.VerificationLogRepository;
import com.digitalcertificate.service.CertificateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * DEMO DATA - loaded once on an empty database when app.demo-data=true.
 * Creates demo institutions, users and certificates so every screen can be tried immediately.
 */
@Component
public class DemoDataLoader implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final AppProperties props;
    private final UserRepository users;
    private final InstitutionRepository institutions;
    private final CertificateRepository certificates;
    private final VerificationLogRepository logs;
    private final CertificateService certificateService;
    private final PasswordEncoder encoder;

    public DemoDataLoader(AppProperties props, UserRepository users, InstitutionRepository institutions,
                          CertificateRepository certificates, VerificationLogRepository logs,
                          CertificateService certificateService, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.institutions = institutions;
        this.certificates = certificates;
        this.logs = logs;
        this.certificateService = certificateService;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.demoData() || users.count() > 0) {
            return;
        }
        log.info("Empty database detected - loading DEMO data ...");

        Institution dit = institution("Demo Institute of Technology", "12 Innovation Park, Bengaluru, India", "contact@dit.example.edu", "https://dit.example.edu");
        Institution gsa = institution("Global Skills Academy", "88 Learning Avenue, Singapore", "hello@gsa.example.org", "https://gsa.example.org");
        institution("Northbridge University", "1 College Green, Dublin, Ireland", "registry@northbridge.example.ie", "https://northbridge.example.ie");

        user("Demo Admin", "admin@demo.com", "Admin@123", Role.ADMIN, null);
        User issuer = user("Demo Issuer", "issuer@demo.com", "Issuer@123", Role.ISSUER, dit);
        User issuer2 = user("Global Skills Issuer", "issuer2@demo.com", "Issuer@123", Role.ISSUER, gsa);
        User verifier = user("Demo Verifier", "verifier@demo.com", "Verifier@123", Role.VERIFIER, null);

        LocalDate today = LocalDate.now();
        List<String> ids = new ArrayList<>();
        ids.add(cert(issuer, "Aarav Sharma", "B.Tech in Computer Science & Engineering", CertificateType.DEGREE, today.minusMonths(5), null, "CGPA 8.9"));
        ids.add(cert(issuer, "Priya Nair", "Full-Stack Web Development Bootcamp", CertificateType.COURSE_COMPLETION, today.minusMonths(4).minusDays(3), today.plusYears(3), "A+"));
        ids.add(cert(issuer, "Rohan Mehta", "Cyber Security Fundamentals", CertificateType.COURSE_COMPLETION, today.minusMonths(3), today.plusYears(2), "92%"));
        ids.add(cert(issuer, "Sneha Kapoor", "National Hackathon - Finalist", CertificateType.PARTICIPATION, today.minusMonths(2).minusDays(10), null, null));
        ids.add(cert(issuer, "Vikram Singh", "Data Structures & Algorithms", CertificateType.COURSE_COMPLETION, today.minusYears(2), today.minusYears(1), "B+"));
        String revoked = cert(issuer, "Ananya Iyer", "Cloud Computing Essentials", CertificateType.COURSE_COMPLETION, today.minusMonths(1), today.plusYears(2), "A");
        ids.add(revoked);
        ids.add(cert(issuer2, "Karan Patel", "Diploma in Digital Marketing", CertificateType.DIPLOMA, today.minusMonths(3).plusDays(5), null, "Distinction"));
        ids.add(cert(issuer2, "Meera Joshi", "Excellence in Public Speaking", CertificateType.ACHIEVEMENT, today.minusDays(12), null, "Gold Medal"));

        certificateService.revoke(revoked, "Issued to the wrong recipient (demo data)", issuer.getId(), null);
        seedVerifications(ids, verifier);

        log.info("==================================================================");
        log.info(" DEMO CREDENTIALS (demo data - change or remove before real use)");
        log.info("   Admin    : admin@demo.com     / Admin@123");
        log.info("   Issuer   : issuer@demo.com    / Issuer@123");
        log.info("   Verifier : verifier@demo.com  / Verifier@123");
        log.info("==================================================================");
    }

    private Institution institution(String name, String address, String email, String website) {
        Institution i = new Institution();
        i.setName(name);
        i.setAddress(address);
        i.setEmail(email);
        i.setWebsite(website);
        return institutions.save(i);
    }

    private User user(String name, String email, String password, Role role, Institution institution) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(encoder.encode(password));
        u.setRole(role);
        u.setInstitution(institution);
        return users.save(u);
    }

    private String cert(User issuer, String recipient, String course, CertificateType type,
                        LocalDate issued, LocalDate expiry, String grade) {
        CertificateRequest r = new CertificateRequest();
        r.setRecipientName(recipient);
        r.setRecipientEmail(recipient.toLowerCase().replace(' ', '.') + "@example.com");
        r.setCourse(course);
        r.setCertificateType(type);
        r.setIssueDate(issued);
        r.setExpiryDate(expiry);
        r.setGrade(grade);
        r.setAdditionalInfo("Demo certificate generated by the CertiTrust sample data loader.");
        Certificate c = certificateService.issue(r, issuer.getId(), null);
        c.setCreatedAt(issued.atTime(10, 0));          // spread the demo certificates over time for the charts
        certificates.save(c);
        return c.getCertificateId();
    }

    private void seedVerifications(List<String> ids, User verifier) {
        Random rnd = new Random(42);
        LocalDateTime now = LocalDateTime.now();
        for (String id : ids) {
            Certificate c = certificates.findByCertificateId(id).orElseThrow();
            VerificationStatus status = c.getStatus() == CertificateStatus.REVOKED ? VerificationStatus.REVOKED
                    : c.isExpired() ? VerificationStatus.EXPIRED : VerificationStatus.VERIFIED;
            int n = 2 + rnd.nextInt(5);
            for (int i = 0; i < n; i++) {
                long minutes = Math.max(1, Duration.between(c.getCreatedAt(), now).toMinutes());
                addLog(id, c.getCreatedAt().plusMinutes((long) (rnd.nextDouble() * minutes)), status,
                        rnd.nextInt(3) == 0 ? verifier : null, rnd);
            }
            c.setVerificationCount(n);
            certificates.save(c);
        }
        for (int i = 0; i < 3; i++) {
            addLog("CERT-2026-99999" + i, now.minusDays(rnd.nextInt(30)), VerificationStatus.NOT_FOUND, null, rnd);
        }
    }

    private void addLog(String id, LocalDateTime time, VerificationStatus status, User verifier, Random rnd) {
        VerificationLog l = new VerificationLog();
        l.setCertificateId(id);
        l.setVerificationTime(time);
        l.setVerificationStatus(status);
        l.setVerifier(verifier);
        l.setIpAddress("203.0.113." + (1 + rnd.nextInt(200)));   // documentation-range IPs, not real visitors
        logs.save(l);
    }
}
