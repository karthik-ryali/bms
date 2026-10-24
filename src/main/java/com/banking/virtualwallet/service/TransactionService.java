package com.banking.virtualwallet.service;

import com.banking.virtualwallet.dto.TransactionDTOs.BackupWalletOption;
import com.banking.virtualwallet.dto.TransactionDTOs.PaymentRequest;
import com.banking.virtualwallet.dto.TransactionDTOs.PaymentResponse;
import com.banking.virtualwallet.entity.Account;
import com.banking.virtualwallet.entity.Transaction;
import com.banking.virtualwallet.entity.VirtualWallet;
import com.banking.virtualwallet.repository.AccountRepository;
import com.banking.virtualwallet.repository.TransactionRepository;
import com.banking.virtualwallet.repository.VirtualWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service orchestrating the flagship multi-tier overflow payment engine,
 * interactive backup wallet deficit coverage, and transaction ledger querying.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final VirtualWalletRepository walletRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              VirtualWalletRepository walletRepository,
                              AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Flagship Multi-Tier Overflow Payment Engine.
     * Attempts payment through 3 cascade tiers:
     * - Tier 1: Deducts directly from selected primary wallet.
     * - Tier 2: Drains primary wallet to 0 and auto-covers remaining deficit from General wallet.
     * - Tier 3: If General wallet is also exhausted, covers remaining deficit from user's chosen backup wallet,
     *           or throws InsufficientFundsNeedBackupException listing eligible backup wallets.
     */
    @Transactional
    public PaymentResponse processPayment(String username, PaymentRequest request) {
        Account account = accountRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for user: " + username));

        VirtualWallet primaryWallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Selected wallet not found"));

        if (!primaryWallet.getAccount().getId().equals(account.getId())) {
            throw new IllegalArgumentException("Wallet does not belong to user");
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        // Check if total account balance is even sufficient
        if (account.getTotalBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient total account balance. Total available: ₹" +
                    account.getTotalBalance() + ", Required: ₹" + amount);
        }

        VirtualWallet generalWallet = walletRepository.findByAccountIdAndIsDefaultGeneralTrue(account.getId())
                .orElseThrow(() -> new IllegalStateException("General wallet not found"));

        BigDecimal primaryBalance = primaryWallet.getBalance();
        BigDecimal primaryDeducted = BigDecimal.ZERO;
        BigDecimal overflowDeducted = BigDecimal.ZERO;
        BigDecimal backupDeducted = BigDecimal.ZERO;
        VirtualWallet overflowWallet = null;
        VirtualWallet backupWallet = null;
        String statusMessage;

        // Tier 1: Primary Wallet deduction
        if (primaryBalance.compareTo(amount) >= 0) {
            primaryDeducted = amount;
            primaryWallet.setBalance(primaryBalance.subtract(amount));
            walletRepository.save(primaryWallet);
            statusMessage = "Payment successful. Deducted directly from " + primaryWallet.getName() + " wallet.";
        } else {
            // Target wallet is insufficient
            primaryDeducted = primaryBalance;
            primaryWallet.setBalance(BigDecimal.ZERO);
            walletRepository.save(primaryWallet);

            BigDecimal deficit = amount.subtract(primaryDeducted);

            // Tier 2: Check General Wallet (if primary is not already General)
            if (primaryWallet.isDefaultGeneral()) {
                // Primary is already General and couldn't cover
                // Check if user provided backup wallet
                if (request.getBackupWalletId() != null) {
                    backupWallet = walletRepository.findById(request.getBackupWalletId())
                            .orElseThrow(() -> new IllegalArgumentException("Backup wallet not found"));
                    if (!backupWallet.getAccount().getId().equals(account.getId())) {
                        throw new IllegalArgumentException("Backup wallet does not belong to user");
                    }
                    if (backupWallet.getBalance().compareTo(deficit) < 0) {
                        throw new IllegalArgumentException("Selected backup wallet (" + backupWallet.getName() +
                                ") does not have enough balance (₹" + backupWallet.getBalance() + ") to cover ₹" + deficit);
                    }
                    backupDeducted = deficit;
                    backupWallet.setBalance(backupWallet.getBalance().subtract(deficit));
                    walletRepository.save(backupWallet);
                    statusMessage = "Payment completed using General (₹" + primaryDeducted + ") and " +
                            backupWallet.getName() + " (₹" + backupDeducted + ").";
                } else {
                    List<BackupWalletOption> eligible = getEligibleBackupWallets(account.getId(), primaryWallet.getId(), deficit);
                    throw new InsufficientFundsNeedBackupException(amount, primaryBalance, BigDecimal.ZERO, deficit, eligible);
                }
            } else {
                // Primary is a non-general category wallet (e.g. Food)
                overflowWallet = generalWallet;
                BigDecimal generalBalance = generalWallet.getBalance();

                if (generalBalance.compareTo(deficit) >= 0) {
                    // General wallet covers entire remainder
                    overflowDeducted = deficit;
                    generalWallet.setBalance(generalBalance.subtract(deficit));
                    walletRepository.save(generalWallet);
                    statusMessage = "Payment completed. " + primaryWallet.getName() + " covered ₹" +
                            primaryDeducted + "; remainder of ₹" + overflowDeducted + " was auto-covered by General Wallet.";
                } else {
                    // Tier 3: General wallet is also insufficient
                    overflowDeducted = generalBalance;
                    generalWallet.setBalance(BigDecimal.ZERO);
                    walletRepository.save(generalWallet);

                    BigDecimal remainingDeficit = deficit.subtract(overflowDeducted);

                    if (request.getBackupWalletId() != null) {
                        backupWallet = walletRepository.findById(request.getBackupWalletId())
                                .orElseThrow(() -> new IllegalArgumentException("Backup wallet not found"));
                        if (!backupWallet.getAccount().getId().equals(account.getId()) ||
                            backupWallet.getId().equals(primaryWallet.getId()) ||
                            backupWallet.getId().equals(generalWallet.getId())) {
                            throw new IllegalArgumentException("Invalid backup wallet selected");
                        }
                        if (backupWallet.getBalance().compareTo(remainingDeficit) < 0) {
                            throw new IllegalArgumentException("Selected backup wallet (" + backupWallet.getName() +
                                    ") does not have enough balance (₹" + backupWallet.getBalance() + ") to cover ₹" + remainingDeficit);
                        }
                        backupDeducted = remainingDeficit;
                        backupWallet.setBalance(backupWallet.getBalance().subtract(remainingDeficit));
                        walletRepository.save(backupWallet);

                        statusMessage = "Payment completed. Split across " + primaryWallet.getName() + " (₹" + primaryDeducted +
                                "), General (₹" + overflowDeducted + "), and " + backupWallet.getName() + " (₹" + backupDeducted + ").";
                    } else {
                        // Rollback balances in this transaction by throwing exception
                        List<BackupWalletOption> eligible = getEligibleBackupWallets(account.getId(), primaryWallet.getId(), remainingDeficit);
                        throw new InsufficientFundsNeedBackupException(amount, primaryDeducted, generalBalance, remainingDeficit, eligible);
                    }
                }
            }
        }

        // Deduct from total account balance to maintain invariant
        account.setTotalBalance(account.getTotalBalance().subtract(amount));
        accountRepository.save(account);

        // Record Transaction
        Transaction tx = new Transaction();
        tx.setReferenceNumber("TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        tx.setAmount(amount);
        tx.setDescription(request.getDescription() != null && !request.getDescription().isBlank() ?
                request.getDescription() : "Payment via " + primaryWallet.getName());
        tx.setTimestamp(LocalDateTime.now());
        tx.setPrimaryWallet(primaryWallet);
        tx.setPrimaryAmountDeducted(primaryDeducted);
        tx.setOverflowWallet(overflowWallet);
        tx.setOverflowAmountDeducted(overflowDeducted);
        tx.setBackupWallet(backupWallet);
        tx.setBackupAmountDeducted(backupDeducted);
        tx.setStatus("COMPLETED");
        tx.setType("PAYMENT");
        tx.setDirection("DEBIT");
        tx.setAccount(account);
        tx = transactionRepository.save(tx);

        // Construct response
        PaymentResponse response = new PaymentResponse();
        response.setReferenceNumber(tx.getReferenceNumber());
        response.setStatus(tx.getStatus());
        response.setTotalAmount(tx.getAmount());
        response.setPrimaryWalletName(primaryWallet.getName());
        response.setPrimaryAmountDeducted(primaryDeducted);
        if (overflowWallet != null && overflowDeducted.compareTo(BigDecimal.ZERO) > 0) {
            response.setOverflowWalletName(overflowWallet.getName());
            response.setOverflowAmountDeducted(overflowDeducted);
        }
        if (backupWallet != null && backupDeducted.compareTo(BigDecimal.ZERO) > 0) {
            response.setBackupWalletName(backupWallet.getName());
            response.setBackupAmountDeducted(backupDeducted);
        }
        response.setRemainingPrimaryBalance(primaryWallet.getBalance());
        response.setRemainingGeneralBalance(generalWallet.getBalance());
        response.setTimestamp(tx.getTimestamp());
        response.setMessage(statusMessage);

        return response;
    }

    /**
     * Finds wallets that have sufficient available balance to cover the remaining deficit,
     * excluding the primary and general wallets.
     */
    private List<BackupWalletOption> getEligibleBackupWallets(Long accountId, Long excludedWalletId, BigDecimal deficit) {
        List<VirtualWallet> allWallets = walletRepository.findByAccountId(accountId);
        List<BackupWalletOption> options = new ArrayList<>();
        for (VirtualWallet w : allWallets) {
            if (!w.getId().equals(excludedWalletId) && !w.isDefaultGeneral() && w.getBalance().compareTo(deficit) >= 0) {
                options.add(new BackupWalletOption(w.getId(), w.getName(), w.getBalance()));
            }
        }
        return options;
    }

    /**
     * Retrieves transactions for the authenticated user, optionally filtered to transactions
     * involving a specific wallet.
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsForUser(String username, Long walletId) {
        Account account = accountRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for user: " + username));

        if (walletId != null) {
            return transactionRepository.findByAccountIdAndWalletInvolved(account.getId(), walletId);
        } else {
            return transactionRepository.findByAccountIdOrderByTimestampDesc(account.getId());
        }
    }
}
