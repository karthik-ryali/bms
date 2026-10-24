package com.banking.virtualwallet.repository;

import com.banking.virtualwallet.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access repository for Transaction ledger entries.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Retrieves all transactions associated with a given master account,
     * ordered chronologically with the newest entries first.
     */
    List<Transaction> findByAccountIdOrderByTimestampDesc(Long accountId);

    /**
     * Retrieves transactions for an account where a specific wallet participated
     * either as the primary debit wallet, the overflow buffer, or the backup wallet.
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND " +
           "(t.primaryWallet.id = :walletId OR (t.overflowWallet IS NOT NULL AND t.overflowWallet.id = :walletId) OR (t.backupWallet IS NOT NULL AND t.backupWallet.id = :walletId)) " +
           "ORDER BY t.timestamp DESC")
    List<Transaction> findByAccountIdAndWalletInvolved(@Param("accountId") Long accountId, @Param("walletId") Long walletId);

    /**
     * Retrieves completed transactions charged to a specified primary wallet,
     * used by the Probability & Statistics (P&S) analytics engine.
     */
    @Query("SELECT t FROM Transaction t WHERE t.primaryWallet.id = :walletId AND t.status = 'COMPLETED' ORDER BY t.timestamp DESC")
    List<Transaction> findCompletedByPrimaryWalletId(@Param("walletId") Long walletId);
}
