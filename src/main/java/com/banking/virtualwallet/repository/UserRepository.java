package com.banking.virtualwallet.repository;

import com.banking.virtualwallet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access repository for User accounts and authentication credentials.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user entity by unique username (used during authentication and profile lookup).
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks if a username is already registered.
     */
    boolean existsByUsername(String username);

    /**
     * Checks if an email address is already registered.
     */
    boolean existsByEmail(String email);

    /**
     * Finds a user by their unique virtual UPI handle (e.g. username@okbms).
     */
    Optional<User> findByUpiId(String upiId);

    /**
     * Finds a user by their registered mobile phone number.
     */
    Optional<User> findByPhone(String phone);

    /**
     * Checks if a mobile phone number is already linked to an existing user.
     */
    boolean existsByPhone(String phone);
}
