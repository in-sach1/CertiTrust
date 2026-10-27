package com.digitalcertificate.config;

import com.digitalcertificate.entity.Role;
import com.digitalcertificate.entity.User;
import com.digitalcertificate.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class InitialAdminLoaderTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void createsHashedAdminWhenDatabaseIsEmptyAndCredentialsAreConfigured() {
        when(users.count()).thenReturn(0L);
        when(passwordEncoder.encode("StrongPass123")).thenReturn("bcrypt-hash");
        InitialAdminLoader loader = new InitialAdminLoader(
                new InitialAdminProperties("Admin@example.com", "StrongPass123"), users, passwordEncoder);

        loader.run(null);

        ArgumentCaptor<User> admin = ArgumentCaptor.forClass(User.class);
        verify(users).save(admin.capture());
        assertEquals("admin@example.com", admin.getValue().getEmail());
        assertEquals("bcrypt-hash", admin.getValue().getPassword());
        assertEquals(Role.ADMIN, admin.getValue().getRole());
        verify(passwordEncoder).encode("StrongPass123");
    }

    @Test
    void skipsWhenNoBootstrapCredentialsAreConfigured() {
        InitialAdminLoader loader = new InitialAdminLoader(
                new InitialAdminProperties("", ""), users, passwordEncoder);

        loader.run(null);

        verifyNoInteractions(users, passwordEncoder);
    }

    @Test
    void refusesIncompleteCredentials() {
        InitialAdminLoader loader = new InitialAdminLoader(
                new InitialAdminProperties("admin@example.com", ""), users, passwordEncoder);

        assertThrows(IllegalStateException.class, () -> loader.run(null));

        verify(users, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void doesNotCreateAnAdminWhenDatabaseAlreadyContainsUsers() {
        when(users.count()).thenReturn(1L);
        InitialAdminLoader loader = new InitialAdminLoader(
                new InitialAdminProperties("admin@example.com", "StrongPass123"), users, passwordEncoder);

        loader.run(null);

        verify(users, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}
