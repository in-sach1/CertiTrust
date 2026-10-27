package com.digitalcertificate.config;

import com.digitalcertificate.security.JsonAuthEntryPoint;
import com.digitalcertificate.security.LoginSuccessHandler;
import com.digitalcertificate.service.AuditService;
import com.digitalcertificate.util.RequestInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Two filter chains:
 *  1) /api/**  - JSON errors, HTTP Basic or session login, no login redirects.
 *  2) everything else - the Thymeleaf website with form login, CSRF-protected forms and role-based pages.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http, JsonAuthEntryPoint jsonEntryPoint) throws Exception {
        // Requests that carry an Authorization header (HTTP Basic) cannot be forged by another website, so CSRF is skipped for them.
        RequestMatcher hasAuthorizationHeader = request -> request.getHeader("Authorization") != null;

        http.securityMatcher("/api/**")
            .csrf(csrf -> csrf
                    .ignoringRequestMatchers("/api/auth/**")
                    .ignoringRequestMatchers(hasAuthorizationHeader))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/certificates/*", "/api/certificates/*/verify").permitAll()
                    .anyRequest().authenticated())
            .httpBasic(basic -> basic.authenticationEntryPoint(jsonEntryPoint))
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(jsonEntryPoint)
                    .accessDeniedHandler(jsonEntryPoint))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webChain(HttpSecurity http, LoginSuccessHandler successHandler,
                                        AuditService audit, RequestInfo requestInfo) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/about", "/design", "/contact", "/verify", "/login", "/register", "/error", "/error/**",
                            "/certificate/**", "/logo/**", "/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/issuer/**").hasRole("ISSUER")
                    .requestMatchers("/verifier/**").hasRole("VERIFIER")
                    .requestMatchers("/manage/**").hasAnyRole("ISSUER", "ADMIN")
                    .anyRequest().authenticated())
            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .usernameParameter("email")
                    .passwordParameter("password")
                    .successHandler(successHandler)
                    .failureHandler((request, response, exception) -> response.sendRedirect(
                            request.getContextPath() + (exception instanceof DisabledException ? "/login?disabled" : "/login?error")))
                    .permitAll())
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .addLogoutHandler((request, response, authentication) -> {
                        if (authentication != null) {
                            audit.log("USER_LOGOUT", authentication.getName(), "Signed out", requestInfo.ip(request));
                        }
                    })
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll())
            .sessionManagement(s -> s.sessionFixation(fix -> fix.migrateSession()));
        return http.build();
    }
}
