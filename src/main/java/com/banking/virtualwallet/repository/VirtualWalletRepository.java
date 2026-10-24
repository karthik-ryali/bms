package com.banking.virtualwallet.repository;

import com.banking.virtualwallet.entity.VirtualWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access repository for Virtual Wallets belonging to a master account.
 */
@Repository
public interface VirtualWalletRepository extends JpaRepository<VirtualWallet, Long> {

    /**
     * Retrieves all logical virtual wallets configured under a specific bank account ID.
     */
    List<VirtualWallet> findByAccountId(Long accountId);

    /**
     * Finds the primary default 'General' wallet for an account, which acts as the
     * buffer wallet for deposits, refunds, and multi-tier overflow coverage.
     */
    Optional<VirtualWallet> findByAccountIdAndIsDefaultGeneralTrue(Long accountId);

    /**
     * Checks if a wallet name already exists for the account (case-insensitive) to prevent duplicates.
     */
    boolean existsByAccountIdAndNameIgnoreCase(Long accountId, String name);
}
