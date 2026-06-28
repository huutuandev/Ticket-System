package com.example.ticket.repository;

import com.example.ticket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("""
    SELECT u FROM User u
    JOIN FETCH u.roles
    WHERE u.username = :username
""")
    Optional<User> findByUsernameWithRoles(String username);
}
