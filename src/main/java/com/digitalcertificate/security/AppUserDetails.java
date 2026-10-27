package com.digitalcertificate.security;

import com.digitalcertificate.entity.Role;
import com.digitalcertificate.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

/** What Spring Security keeps in the session for a logged-in user (no entity, no lazy loading). */
public class AppUserDetails implements UserDetails {
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;

    public AppUserDetails(User u) {
        this.id = u.getId();
        this.name = u.getName();
        this.email = u.getEmail();
        this.passwordHash = u.getPassword();
        this.role = u.getRole();
        this.enabled = u.isEnabled();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
