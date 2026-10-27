package com.digitalcertificate.security;

import com.digitalcertificate.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return users.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
