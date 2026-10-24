package com.banking.virtualwallet.service;

import com.banking.virtualwallet.dto.WalletDTOs.AccountResponse;
import com.banking.virtualwallet.dto.WalletDTOs.WalletResponse;
import com.banking.virtualwallet.entity.Account;
import com.banking.virtualwallet.entity.Transaction;
import com.banking.virtualwallet.entity.User;
import com.banking.virtualwallet.entity.VirtualWallet;
import com.banking.virtualwallet.repository.AccountRepository;
import com.banking.virtualwallet.repository.TransactionRepository;
import com.banking.virtualwallet.repository.UserRepository;
import com.banking.virtualwallet.repository.VirtualWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing master bank accounts, balance invariant verification,
 * fund deposits, and inter-account fund transfers with multi-tier overflow fallback.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final VirtualWalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository,
                          VirtualWalletRepository walletRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Resolves the master bank account belonging to a specific user.
     */
    @Transactional(readOnly = true)
    public Account getAccountForUser(String username) {
        return accountRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for user: " + username));
    }

    /**
     * Computes the complete account state including all wallet balances, allocation percentages,
     * and asserts the core ledger invariant: sum(wallets.balance) == account.totalBalance.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountDetails(String username) {
        Account account = getAccountForUser(username);
        List<VirtualWallet> wallets = walletRepository.findByAccountId(account.getId());

        BigDecimal totalWallets = BigDecimal.ZERO;
        for (VirtualWallet w : wallets) {
            totalWallets = totalWallets.add(w.getBalance());
        }

        List<WalletResponse> walletResponses = new ArrayList<>();
        BigDecimal accountBalance = account.getTotalBalance();

        for (VirtualWallet w : wallets) {
            double pct = 0.0;
            if (accountBalance.compareTo(BigDecimal.ZERO) > 0) {
                pct = w.getBalance().divide(accountBalance, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            walletResponses.add(new WalletResponse(
                    w.getId(),
                    w.getName(),
                    w.getCategory(),
                    w.getBalance(),
                    w.isDefaultGeneral(),
                    Math.round(pct * 10.0) / 10.0
            ));
        }

        boolean isBalanced = totalWallets.compareTo(accountBalance) == 0;

        return new AccountResponse(
                account.getAccountNumber(),
                accountBalance,
                totalWallets,
                isBalanced,
                walletResponses
        );
    }

    /**
     * Deposits external funds into the user's account.
     * Credits both account total balance and the default General wallet, then writes a DEPOSIT ledger entry.
     */
    @Transactional
    public AccountResponse depositFunds(String username, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        Account account = getAccountForUser(username);
        VirtualWallet generalWallet = walletRepository.findByAccountIdAndIsDefaultGeneralTrue(account.getId())
                .orElseThrow(() -> new IllegalStateException("General wallet not found"));

        // Increase total account balance
        account.setTotalBalance(account.getTotalBalance().add(amount));
        accountRepository.save(account);

        // Inflow automatically credits General wallet to maintain sum(wallets) = totalBalance
        generalWallet.setBalance(generalWallet.getBalance().add(amount));
        walletRepository.save(generalWallet);

        // Record audit transaction
        Transaction depositTx = new Transaction();
        depositTx.setReferenceNumber("DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        depositTx.setAmount(amount);
        depositTx.setDescription("Fund Deposit to Account");
        depositTx.setTimestamp(LocalDateTime.now());
        depositTx.setPrimaryWallet(generalWallet);
        depositTx.setPrimaryAmountDeducted(BigDecimal.ZERO);
        depositTx.setStatus("COMPLETED");
        depositTx.setType("DEPOSIT");
        depositTx.setDirection("CREDIT");
        depositTx.setAccount(account);
        transactionRepository.save(depositTx);

        return getAccountDetails(username);
    }

    /**
     * Executes an atomic account-to-account transfer.
     * Resolves the recipient by UPI ID or account number, debits the sender's chosen funding wallet
     * and master account, credits the recipient's General wallet and master account, and logs dual audit entries.
     */
    @Transactional
    public Map<String, Object> transferBetweenAccounts(String username, String toAccountNumber, String toUpiId, Long fromWalletId, BigDecimal amount, String description) {
        return transferBetweenAccounts(username, toAccountNumber, toUpiId, fromWalletId, null, amount, description);
    }

    /**
     * Executes inter-account transfer with optional interactive backup wallet selection
     * when primary and General wallets encounter a combined deficit.
     */
    public Map<String, Object> transferBetweenAccounts(String username, String toAccountNumber, String toUpiId, Long fromWalletId, Long backupWalletId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        Account fromAccount = getAccountForUser(username);
        Account toAccount;
        if (toUpiId != null && !toUpiId.isBlank()) {
            String upi = toUpiId.trim();
            User recipient = userRepository.findByUpiId(upi)
                    .or(() -> userRepository.findByUsername(upi.contains("@") ? upi.substring(0, upi.indexOf('@')) : upi))
                    .orElseThrow(() -> new IllegalArgumentException("Recipient UPI not found: " + toUpiId));
            toAccount = accountRepository.findByUser(recipient)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found for UPI: " + toUpiId));
        } else if (toAccountNumber != null && !toAccountNumber.isBlank()) {
            String target = toAccountNumber.trim();
            Optional<Account> byAcc = accountRepository.findByAccountNumber(target);
            if (byAcc.isPresent()) {
                toAccount = byAcc.get();
            } else {
                Optional<User> byPhone = userRepository.findByPhone(target);
                if (byPhone.isPresent()) {
                    toAccount = accountRepository.findByUser(byPhone.get())
                            .orElseThrow(() -> new IllegalArgumentException("Account not found for phone: " + target));
                } else {
                    Optional<User> byUpi = userRepository.findByUpiId(target);
                    if (byUpi.isPresent()) {
                        toAccount = accountRepository.findByUser(byUpi.get())
                                .orElseThrow(() -> new IllegalArgumentException("Account not found for UPI: " + target));
                    } else {
                        User byUser = userRepository.findByUsername(target)
                                .orElseThrow(() -> new IllegalArgumentException("Recipient account or phone not found: " + target));
                        toAccount = accountRepository.findByUser(byUser)
                                .orElseThrow(() -> new IllegalArgumentException("Account not found for user: " + target));
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Recipient account number, UPI ID, or phone number is required");
        }

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        if (fromAccount.getTotalBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Available: " + fromAccount.getTotalBalance());
        }

        // Resolve the funding wallet: explicitly chosen one, else General.
        VirtualWallet fromWallet;
        if (fromWalletId != null) {
            fromWallet = walletRepository.findById(fromWalletId)
                    .orElseThrow(() -> new IllegalArgumentException("Source wallet not found: " + fromWalletId));
            if (fromWallet.getAccount() == null
                    || !fromWallet.getAccount().getId().equals(fromAccount.getId())) {
                throw new IllegalArgumentException("Source wallet does not belong to your account");
            }
        } else {
            fromWallet = walletRepository.findByAccountIdAndIsDefaultGeneralTrue(fromAccount.getId())
                    .orElseThrow(() -> new IllegalStateException("Sender General wallet not found"));
        }
        VirtualWallet toGeneral = walletRepository.findByAccountIdAndIsDefaultGeneralTrue(toAccount.getId())
                .orElseThrow(() -> new IllegalStateException("Recipient General wallet not found"));

        VirtualWallet generalWallet = walletRepository.findByAccountIdAndIsDefaultGeneralTrue(fromAccount.getId())
                .orElseThrow(() -> new IllegalStateException("Sender General wallet not found"));

        BigDecimal primaryDeducted = BigDecimal.ZERO;
        BigDecimal overflowDeducted = BigDecimal.ZERO;
        BigDecimal backupDeducted = BigDecimal.ZERO;
        VirtualWallet overflowWallet = null;
        VirtualWallet backupWallet = null;

        if (fromWallet.getBalance().compareTo(amount) >= 0) {
            primaryDeducted = amount;
            fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
            walletRepository.save(fromWallet);
        } else {
            // MULTI-TIER OVERFLOW:
            primaryDeducted = fromWallet.getBalance();
            fromWallet.setBalance(BigDecimal.ZERO);
            walletRepository.save(fromWallet);

            BigDecimal deficit = amount.subtract(primaryDeducted);
            if (!fromWallet.getId().equals(generalWallet.getId())) {
                overflowWallet = generalWallet;
                if (generalWallet.getBalance().compareTo(deficit) >= 0) {
                    overflowDeducted = deficit;
                    generalWallet.setBalance(generalWallet.getBalance().subtract(deficit));
                    walletRepository.save(generalWallet);
                } else {
                    overflowDeducted = generalWallet.getBalance();
                    generalWallet.setBalance(BigDecimal.ZERO);
                    walletRepository.save(generalWallet);

                    BigDecimal remainingDeficit = deficit.subtract(overflowDeducted);
                    if (backupWalletId != null) {
                        backupWallet = walletRepository.findById(backupWalletId).orElse(null);
                        if (backupWallet != null && backupWallet.getAccount().getId().equals(fromAccount.getId())
                                && !backupWallet.getId().equals(fromWallet.getId())
                                && !backupWallet.getId().equals(generalWallet.getId())
                                && backupWallet.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal take = backupWallet.getBalance().min(remainingDeficit);
                            backupDeducted = take;
                            backupWallet.setBalance(backupWallet.getBalance().subtract(take));
                            walletRepository.save(backupWallet);
                            remainingDeficit = remainingDeficit.subtract(take);
                        }
                    }
                    if (remainingDeficit.compareTo(BigDecimal.ZERO) > 0) {
                        List<VirtualWallet> otherWallets = walletRepository.findByAccountId(fromAccount.getId());
                        for (VirtualWallet ow : otherWallets) {
                            if (!ow.getId().equals(fromWallet.getId()) && !ow.getId().equals(generalWallet.getId())
                                    && (backupWallet == null || !ow.getId().equals(backupWallet.getId()))
                                    && ow.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal take = ow.getBalance().min(remainingDeficit);
                                ow.setBalance(ow.getBalance().subtract(take));
                                walletRepository.save(ow);
                                remainingDeficit = remainingDeficit.subtract(take);
                                if (remainingDeficit.compareTo(BigDecimal.ZERO) == 0) break;
                            }
                        }
                    }
                }
            } else {
                BigDecimal remainingDeficit = deficit;
                if (backupWalletId != null) {
                    backupWallet = walletRepository.findById(backupWalletId).orElse(null);
                    if (backupWallet != null && backupWallet.getAccount().getId().equals(fromAccount.getId())
                            && !backupWallet.getId().equals(generalWallet.getId())
                            && backupWallet.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal take = backupWallet.getBalance().min(remainingDeficit);
                        backupDeducted = take;
                        backupWallet.setBalance(backupWallet.getBalance().subtract(take));
                        walletRepository.save(backupWallet);
                        remainingDeficit = remainingDeficit.subtract(take);
                    }
                }
                if (remainingDeficit.compareTo(BigDecimal.ZERO) > 0) {
                    List<VirtualWallet> otherWallets = walletRepository.findByAccountId(fromAccount.getId());
                    for (VirtualWallet ow : otherWallets) {
                        if (!ow.getId().equals(generalWallet.getId())
                                && (backupWallet == null || !ow.getId().equals(backupWallet.getId()))
                                && ow.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal take = ow.getBalance().min(remainingDeficit);
                            ow.setBalance(ow.getBalance().subtract(take));
                            walletRepository.save(ow);
                            remainingDeficit = remainingDeficit.subtract(take);
                            if (remainingDeficit.compareTo(BigDecimal.ZERO) == 0) break;
                        }
                    }
                }
            }
        }

        // Debit sender master balance
        fromAccount.setTotalBalance(fromAccount.getTotalBalance().subtract(amount));
        accountRepository.save(fromAccount);

        // Credit recipient General wallet and master balance
        toGeneral.setBalance(toGeneral.getBalance().add(amount));
        walletRepository.save(toGeneral);
        toAccount.setTotalBalance(toAccount.getTotalBalance().add(amount));
        accountRepository.save(toAccount);

        String note = (description != null && !description.isBlank()) ? description : "Transfer to " + toAccount.getUser().getFullName();

        Transaction outTx = new Transaction();
        outTx.setReferenceNumber("TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        outTx.setAmount(amount);
        String finalDesc = note;
        if (overflowWallet != null && overflowDeducted.compareTo(BigDecimal.ZERO) > 0) {
            finalDesc += " (" + fromWallet.getName() + " ₹" + primaryDeducted + " + " + overflowWallet.getName() + " ₹" + overflowDeducted;
            if (backupWallet != null && backupDeducted.compareTo(BigDecimal.ZERO) > 0) {
                finalDesc += " + " + backupWallet.getName() + " ₹" + backupDeducted;
            }
            finalDesc += ")";
            outTx.setOverflowWallet(overflowWallet);
            outTx.setOverflowAmountDeducted(overflowDeducted);
        } else if (backupWallet != null && backupDeducted.compareTo(BigDecimal.ZERO) > 0) {
            finalDesc += " (" + fromWallet.getName() + " ₹" + primaryDeducted + " + " + backupWallet.getName() + " ₹" + backupDeducted + ")";
            outTx.setOverflowWallet(backupWallet);
            outTx.setOverflowAmountDeducted(backupDeducted);
        }
        outTx.setDescription(finalDesc);
        outTx.setTimestamp(LocalDateTime.now());
        outTx.setPrimaryWallet(fromWallet);
        outTx.setPrimaryAmountDeducted(primaryDeducted);
        outTx.setStatus("COMPLETED");
        outTx.setType("TRANSFER");
        outTx.setDirection("DEBIT");
        outTx.setAccount(fromAccount);
        transactionRepository.save(outTx);

        Transaction inTx = new Transaction();
        inTx.setReferenceNumber("REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        inTx.setAmount(amount);
        inTx.setDescription(note + " from " + fromAccount.getUser().getFullName());
        inTx.setTimestamp(LocalDateTime.now());
        inTx.setPrimaryWallet(toGeneral);
        inTx.setPrimaryAmountDeducted(BigDecimal.ZERO);
        inTx.setStatus("COMPLETED");
        inTx.setType("TRANSFER");
        inTx.setDirection("CREDIT");
        inTx.setAccount(toAccount);
        transactionRepository.save(inTx);

        String statusMsg;
        if (overflowWallet != null && overflowDeducted.compareTo(BigDecimal.ZERO) > 0) {
            statusMsg = "Transfer of ₹" + amount + " to " + toAccount.getUser().getFullName()
                    + " completed! " + fromWallet.getName() + " covered ₹" + primaryDeducted
                    + "; General covered ₹" + overflowDeducted;
            if (backupWallet != null && backupDeducted.compareTo(BigDecimal.ZERO) > 0) {
                statusMsg += "; " + backupWallet.getName() + " covered ₹" + backupDeducted;
            }
            statusMsg += ".";
        } else if (backupWallet != null && backupDeducted.compareTo(BigDecimal.ZERO) > 0) {
            statusMsg = "Transfer of ₹" + amount + " to " + toAccount.getUser().getFullName()
                    + " completed! " + fromWallet.getName() + " covered ₹" + primaryDeducted
                    + "; " + backupWallet.getName() + " covered remainder ₹" + backupDeducted + ".";
        } else {
            statusMsg = "Transfer of ₹" + amount + " to " + toAccount.getUser().getFullName()
                    + " completed from your " + fromWallet.getName() + " wallet.";
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("message", statusMsg);
        result.put("referenceNumber", outTx.getReferenceNumber());
        result.put("newBalance", fromAccount.getTotalBalance());
        result.put("sourceWallet", fromWallet.getName());
        return result;
    }
}
