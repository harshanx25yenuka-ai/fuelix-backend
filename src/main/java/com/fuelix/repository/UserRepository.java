package com.fuelix.repository;

import com.fuelix.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNic(String nic);
    Optional<User> findByEmail(String email);
    boolean existsByNic(String nic);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);  // Add this method
}