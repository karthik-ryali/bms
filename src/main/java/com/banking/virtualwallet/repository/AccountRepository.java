package com.banking.virtualwallet.repository;

import com.banking.virtualwallet.entity.Account;
import com.banking.virtualwallet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access repository for Bank Master Accounts.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Finds the master account owned by a specific User entity.
     */
    Optional<Account> findByUser(User user);

    /**
     * Finds the master account belonging to a user by their username.
     */
    Optional<Account> findByUserUsername(String username);

    /**
     * Resolves an account by its unique bank account number (e.g. ACC-100234).
     */
    Optional<Account> findByAccountNumber(String accountNumber);
}
