package com.digitalcertificate.service;

import com.digitalcertificate.dto.UserForm;
import com.digitalcertificate.entity.Institution;
import com.digitalcertificate.entity.Role;
import com.digitalcertificate.entity.User;
import com.digitalcertificate.exception.BusinessException;
import com.digitalcertificate.exception.InvalidInputException;
import com.digitalcertificate.exception.ResourceNotFoundException;
import com.digitalcertificate.repository.CertificateRepository;
import com.digitalcertificate.repository.InstitutionRepository;
import com.digitalcertificate.repository.RevocationRepository;
import com.digitalcertificate.repository.UserRepository;
import com.digitalcertificate.repository.VerificationLogRepository;
import com.digitalcertificate.util.Patterns;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class UserService {
    private final UserRepository users;
    private final InstitutionRepository institutions;
    private final CertificateRepository certificates;
    private final RevocationRepository revocations;
    private final VerificationLogRepository logs;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserService(UserRepository users, InstitutionRepository institutions, CertificateRepository certificates,
                       RevocationRepository revocations, VerificationLogRepository logs,
                       PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.institutions = institutions;
        this.certificates = certificates;
        this.revocations = revocations;
        this.logs = logs;
        this.encoder = encoder;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ public self-registration

    /**
     * Self-registration. ADMIN accounts cannot be self-created. A new ISSUER also creates its institution and stays
     * disabled until an administrator approves it, so nobody can issue certificates in an institution's name on their own.
     */
    @Transactional
    public User register(String name, String email, String rawPassword, Role role, String institutionName, String ip) {
        if (role == Role.ADMIN) {
            throw new BusinessException("Administrator accounts can only be created by an existing administrator.");
        }
        if (!Patterns.isValidPassword(rawPassword)) {
            throw new InvalidInputException(Patterns.PASSWORD_MESSAGE);
        }
        String mail = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(mail)) {
            throw new BusinessException("An account with this email already exists.");
        }
        User u = new User();
        u.setName(name.trim());
        u.setEmail(mail);
        u.setPassword(encoder.encode(rawPassword));
        u.setRole(role);
        u.setEnabled(true);

        if (role == Role.ISSUER) {
            if (institutionName == null || institutionName.isBlank()) {
                throw new BusinessException("Please enter the name of your institution.");
            }
            if (institutions.existsByNameIgnoreCase(institutionName.trim())) {
                throw new BusinessException("This institution is already registered. Ask your administrator to create your issuer account.");
            }
            Institution inst = new Institution();
            inst.setName(institutionName.trim());
            u.setInstitution(institutions.save(inst));
            u.setEnabled(false); // pending approval
        }
        u = users.save(u);
        audit.log("USER_REGISTERED", u.getEmail(), "Role " + role + (u.isEnabled() ? "" : " (pending approval)"), ip);
        return u;
    }

    // ------------------------------------------------------------------ administration

    @Transactional(readOnly = true)
    public Page<User> search(String q, int page, int size) {
        String like = "%" + (q == null ? "" : q.trim().toLowerCase(Locale.ROOT)) + "%";
        return users.search(like, PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public User createByAdmin(UserForm f, String adminEmail, String ip) {
        String mail = f.getEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(mail)) {
            throw new BusinessException("An account with this email already exists.");
        }
        if (!Patterns.isValidPassword(f.getPassword())) {
            throw new InvalidInputException(Patterns.PASSWORD_MESSAGE);
        }
        User u = new User();
        u.setName(f.getName().trim());
        u.setEmail(mail);
        u.setPassword(encoder.encode(f.getPassword()));
        u.setRole(f.getRole());
        u.setEnabled(f.isEnabled());
        u.setInstitution(institutionFor(f));
        u = users.save(u);
        audit.log("USER_CREATED", adminEmail, u.getEmail() + " (" + u.getRole() + ")", ip);
        return u;
    }

    @Transactional
    public User updateByAdmin(Long id, UserForm f, Long actingAdminId, String adminEmail, String ip) {
        User u = getById(id);
        String mail = f.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!mail.equalsIgnoreCase(u.getEmail()) && users.existsByEmailIgnoreCase(mail)) {
            throw new BusinessException("An account with this email already exists.");
        }
        boolean self = u.getId().equals(actingAdminId);
        if (self && (f.getRole() != Role.ADMIN || !f.isEnabled())) {
            throw new BusinessException("You cannot change your own role or disable your own account.");
        }
        if (u.getRole() == Role.ADMIN && f.getRole() != Role.ADMIN && users.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessException("At least one administrator must remain.");
        }
        if (f.getPassword() != null && !f.getPassword().isBlank()) {
            if (!Patterns.isValidPassword(f.getPassword())) {
                throw new InvalidInputException(Patterns.PASSWORD_MESSAGE);
            }
            u.setPassword(encoder.encode(f.getPassword()));
        }
        u.setName(f.getName().trim());
        u.setEmail(mail);
        u.setRole(f.getRole());
        u.setEnabled(f.isEnabled());
        u.setInstitution(institutionFor(f));
        u = users.save(u);
        audit.log("USER_UPDATED", adminEmail, u.getEmail() + " (" + u.getRole() + ", " + (u.isEnabled() ? "enabled" : "disabled") + ")", ip);
        return u;
    }

    @Transactional
    public boolean toggleEnabled(Long id, Long actingAdminId, String adminEmail, String ip) {
        User u = getById(id);
        if (u.getId().equals(actingAdminId)) {
            throw new BusinessException("You cannot disable your own account.");
        }
        u.setEnabled(!u.isEnabled());
        users.save(u);
        audit.log(u.isEnabled() ? "USER_ENABLED" : "USER_DISABLED", adminEmail, u.getEmail(), ip);
        return u.isEnabled();
    }

    @Transactional
    public void delete(Long id, Long actingAdminId, String adminEmail, String ip) {
        User u = getById(id);
        if (u.getId().equals(actingAdminId)) {
            throw new BusinessException("You cannot delete your own account.");
        }
        if (u.getRole() == Role.ADMIN && users.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessException("At least one administrator must remain.");
        }
        if (certificates.existsByIssuerId(id) || revocations.existsByRevokedById(id)) {
            throw new BusinessException("This user has issued or revoked certificates, so the account cannot be deleted. Disable it instead.");
        }
        logs.detachVerifier(id);
        users.delete(u);
        audit.log("USER_DELETED", adminEmail, u.getEmail(), ip);
    }

    private Institution institutionFor(UserForm f) {
        if (f.getRole() != Role.ISSUER) {
            return null;
        }
        if (f.getInstitutionId() == null) {
            throw new BusinessException("Please choose an institution for the issuer.");
        }
        return institutions.findById(f.getInstitutionId())
                .orElseThrow(() -> new BusinessException("The selected institution does not exist."));
    }

    // ------------------------------------------------------------------ own account

    @Transactional
    public void updateName(Long id, String name, String ip) {
        User u = getById(id);
        u.setName(name.trim());
        users.save(u);
        audit.log("USER_UPDATED", u.getEmail(), "Profile name changed", ip);
    }

    @Transactional
    public void changePassword(Long id, String current, String newPassword, String ip) {
        if (!Patterns.isValidPassword(newPassword)) {
            throw new InvalidInputException(Patterns.PASSWORD_MESSAGE);
        }
        User u = getById(id);
        if (!encoder.matches(current, u.getPassword())) {
            throw new BusinessException("Your current password is incorrect.");
        }
        u.setPassword(encoder.encode(newPassword));
        users.save(u);
        audit.log("USER_PASSWORD_CHANGED", u.getEmail(), "Password changed", ip);
    }
}
