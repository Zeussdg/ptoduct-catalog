package com.ikibm.catalog.repository;

import com.ikibm.catalog.entity.Role;
import com.ikibm.catalog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    Page<User> findByRole(Role role, Pageable pageable);
}
