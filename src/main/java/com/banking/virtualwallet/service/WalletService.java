package com.banking.virtualwallet.service;

import com.banking.virtualwallet.dto.WalletDTOs.*;
import com.banking.virtualwallet.entity.Account;
import com.banking.virtualwallet.entity.Transaction;
import com.banking.virtualwallet.entity.VirtualWallet;
import com.banking.virtualwallet.repository.AccountRepository;
import com.banking.virtualwallet.repository.TransactionRepository;
import com.banking.virtualwallet.repository.VirtualWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;

/**
 * Service managing logical virtual wallets, initial allocations, internal transfers,
 * multi-wallet reallocations, renames, and deletion with refund to the General wallet.
 */
@Service
public class WalletService {

    private final VirtualWalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(VirtualWalletRepository walletRepository,
                         AccountRepository accountRepository,
                         TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Retrieves all virtual wallets for a user along with their individual allocation percentages,
     * ensuring the default General wallet is initialized.
     */
    @Transactional(readOnly = true)
    public List<WalletResponse> getWalletsForUser(String username) {
        Account account = accountRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for user: " + username));

        // Ensure General Wallet exists
        ensureGeneralWalletExists(account);

        List<VirtualWallet> wallets = walletRepository.findByAccountId(account.getId());
        BigDecimal totalBalance = account.getTotalBalance();

        List<WalletResponse> responses = new ArrayList<>();
        for (VirtualWallet w : wallets) {
            double pct = 0.0;
            if (totalBalance.compareTo(BigDecimal.ZERO) > 0) {
                pct = w.getBalance().divide(totalBalance, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            responses.add(new WalletResponse(
                    w.getId(),
                    w.getName(),
                    w.getCategory(),
                    w.getBalance(),
                    w.isDefaultGeneral(),
                    Math.round(pct * 10.0) / 10.0
            ));
        }
        return responses;
    }

    /**
     * Creates a new virtual wallet. The initial allocation is deducted from the
     * user's General wallet to preserve the invariant: sum(wallets) == totalBalance.
     */
    @Transactional
    public WalletResponse createWallet(String username, WalletCreateRequest request) {
        Account account = accountRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for user: " + username));

        if (walletRepository.existsByAccountIdAndNameIgnoreCase(account.getId(), request.getName())) {
            throw new IllegalArgumentException("Wallet with name '" + request.getName() + "' already exists");
        }

        // Ensure General Wallet exists
        VirtualWallet generalWallet = ensureGeneralWalletExists(account);

        BigDecimal initialAlloc = request.getInitialAllocation() != null ? request.getInitialAllocation() : BigDecimal.ZERO;

        if (initialAlloc.compareTo(BigDecimal.ZERO) > 0) {
            if (generalWallet.getBalance().compareTo(initialAlloc) < 0) {
                throw new IllegalArgumentException("Insufficient balance in General Wallet (Available: ₹" +
                        generalWallet.getBalance() + ") to allocate ₹" + initialAlloc);
            }
            // Deduct allocation from General Wallet to maintain invariant
            generalWallet.setBalance(generalWallet.getBalance().subtract(initialAlloc));
            walletRepository.save(generalWallet);
        }

        VirtualWallet newWallet = new VirtualWallet(
                request.getName(),
                request.getCategory() != null ? request.getCategory() : "General Category",
                initialAlloc,
                false,
                account
        );
        newWallet = walletRepository.save(newWallet);

        double pct = 0.0;
        if (account.getTotalBalance().compareTo(BigDecimal.ZERO) > 0) {
            pct = newWallet.getBalance().divide(account.getTotalBalance(), 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
        }

        return new WalletResponse(
                newWallet.getId(),
                newWallet.getName(),
                newWallet.getCategory(),
                newWallet.getBalance(),
                newWallet.isDefaultGeneral(),
                Math.round(pct * 10.0) / 10.0
        );
    }

    /**
     * Moves allocated balance between two virtual wallets within the same account.
     * Total account balance remains unchanged; writes an INTERNAL transfer ledger row.
     */
    @Transactional
    public void transferBetweenWallets(String username, WalletTransferRequest request) {
        if (request.getFromWalletId().equals(request.getToWalletId())) {
            throw new IllegalArgumentException("Source and destination wallets cannot be the same");
        }

        Account account = accountRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for user: " + username));

        VirtualWallet fromWallet = walletRepository.findById(request.getFromWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Source wallet not found"));
        VirtualWallet toWallet = walletRepository.findById(request.getToWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Destination wallet not found"));

        if (!fromWallet.getAccount().getId().equals(account.getId()) ||
            !toWallet.getAccount().getId().equals(account.getId())) {
            throw new IllegalArgumentException("Wallets do not belong to the user's account");
        }

        BigDecimal amount = request.getAmount();
        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in " + fromWallet.getName() +
                    " wallet. Available: ₹" + fromWallet.getBalance() + ", Requested: ₹" + amount);
        }

        // Transfer funds
        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
        toWallet.setBalance(toWallet.getBalance().add(amount));

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        // Record transfer transaction
        Transaction tx = new Transaction();
        tx.setReferenceNumber("TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setAmount(amount);
        tx.setDescription("Transfer from " + fromWallet.getName() + " to " + toWallet.getName());
        tx.setTimestamp(LocalDateTime.now());
        tx.setPrimaryWallet(fromWallet);
        tx.setPrimaryAmountDeducted(amount);
        tx.setStatus("COMPLETED");
        tx.setType("TRANSFER");
        tx.setDirection("INTERNAL");
        tx.setAccount(account);
        transactionRepository.save(tx);
    }

    /**
     * Reallocates balances across all user wallets simultaneously.
     * Enforces that the sum of new balances strictly equals the master account total.
     */
    @Transactional
    public void reallocateWallets(String username, WalletReallocateRequest request) {
        Account account = accountRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for user: " + username));

        // Ensure General Wallet exists
        ensureGeneralWalletExists(account);

        List<VirtualWallet> userWallets = walletRepository.findByAccountId(account.getId());
        Map<Long, VirtualWallet> walletMap = new HashMap<>();
        for (VirtualWallet w : userWallets) {
            walletMap.put(w.getId(), w);
        }

        BigDecimal newTotal = BigDecimal.ZERO;
        for (WalletReallocationItem item : request.getAllocations()) {
            if (item.getNewBalance().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Wallet balance cannot be negative");
            }
            if (!walletMap.containsKey(item.getWalletId())) {
                throw new IllegalArgumentException("Wallet id " + item.getWalletId() + " does not belong to user");
            }
            newTotal = newTotal.add(item.getNewBalance());
        }

        // Verify invariant: sum(wallets) == account.totalBalance
        if (newTotal.compareTo(account.getTotalBalance()) != 0) {
            throw new IllegalArgumentException("Total reallocation (₹" + newTotal +
                    ") must equal total account balance (₹" + account.getTotalBalance() + ")");
        }

        for (WalletReallocationItem item : request.getAllocations()) {
            VirtualWallet wallet = walletMap.get(item.getWalletId());
            wallet.setBalance(item.getNewBalance());
            walletRepository.save(wallet);
        }
    }

    /**
     * Renames an existing virtual wallet while verifying name uniqueness per account.
     * The wallet balance is unaffected.
     */
    @Transactional
    public WalletResponse renameWallet(String username, Long walletId, WalletRenameRequest request) {
        Account account = accountRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for user: " + username));

        VirtualWallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (!wallet.getAccount().getId().equals(account.getId())) {
            throw new IllegalArgumentException("Wallet does not belong to user");
        }

        String newName = request.getName().trim();

        if (!wallet.getName().equalsIgnoreCase(newName)
                && walletRepository.existsByAccountIdAndNameIgnoreCase(account.getId(), newName)) {
            throw new IllegalArgumentException("Wallet with name '" + newName + "' already exists");
        }

        wallet.setName(newName);
        wallet = walletRepository.save(wallet);

        double pct = 0.0;
        if (account.getTotalBalance().compareTo(BigDecimal.ZERO) > 0) {
            pct = wallet.getBalance()
                    .divide(account.getTotalBalance(), 4, RoundingMode.HALF_UP)
                    .doubleValue() * 100.0;
        }

        return new WalletResponse(
                wallet.getId(),
                wallet.getName(),
                wallet.getCategory(),
                wallet.getBalance(),
                wallet.isDefaultGeneral(),
                Math.round(pct * 10.0) / 10.0
        );
    }

    /**
     * Deletes a virtual wallet and refunds its remaining balance back into the General wallet.
     * The default General wallet is protected and cannot be deleted.
     */
    @Transactional
    public void deleteWallet(String username, Long walletId) {
        Account account = accountRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for user: " + username));

        VirtualWallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (!wallet.getAccount().getId().equals(account.getId())) {
            throw new IllegalArgumentException("Wallet does not belong to user");
        }

        if (wallet.isDefaultGeneral()) {
            throw new IllegalArgumentException("The default General Wallet cannot be deleted");
        }

        VirtualWallet generalWallet = ensureGeneralWalletExists(account);

        // Refund any remaining balance back to General Wallet
        BigDecimal refundAmount = wallet.getBalance();
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            generalWallet.setBalance(generalWallet.getBalance().add(refundAmount));
            walletRepository.save(generalWallet);
        }

        walletRepository.delete(wallet);
    }

    /**
     * Ensures the General Wallet exists for the account.
     * Creates it with the full account balance if it doesn't exist (self-healing).
     */
    private VirtualWallet ensureGeneralWalletExists(Account account) {
        return walletRepository.findByAccountIdAndIsDefaultGeneralTrue(account.getId())
                .orElseGet(() -> {
                    VirtualWallet generalWallet = new VirtualWallet(
                            "General",
                            "General Purpose",
                            account.getTotalBalance(),
                            true,
                            account
                    );
                    return walletRepository.save(generalWallet);
                });
    }
}