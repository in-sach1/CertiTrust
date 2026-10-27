package com.digitalcertificate.repository;

import com.digitalcertificate.entity.Role;
import com.digitalcertificate.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(Role role);

    @Query("select u from User u where lower(u.name) like :q or lower(u.email) like :q")
    Page<User> search(@Param("q") String q, Pageable pageable);
}
