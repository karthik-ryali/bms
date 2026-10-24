package com.banking.virtualwallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Objects for payments, deposits, transfers, and overflow responses.
 */
public class TransactionDTOs {

    /** Request payload for processing a multi-tier wallet payment. */
    public static class PaymentRequest {
        @NotNull(message = "Target wallet ID is required")
        private Long walletId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        private BigDecimal amount;

        private String description;

        // Optional secondary/backup wallet ID if General wallet is also inadequate
        private Long backupWalletId;

        public PaymentRequest() {}

        public PaymentRequest(Long walletId, BigDecimal amount, String description) {
            this.walletId = walletId;
            this.amount = amount;
            this.description = description;
        }

        public Long getWalletId() { return walletId; }
        public void setWalletId(Long walletId) { this.walletId = walletId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getBackupWalletId() { return backupWalletId; }
        public void setBackupWalletId(Long backupWalletId) { this.backupWalletId = backupWalletId; }
    }

    /** Request payload for depositing external funds into the master account. */
    public static class DepositRequest {
        @NotNull(message = "Deposit amount is required")
        @DecimalMin(value = "1.00", message = "Minimum deposit is 1.00")
        private BigDecimal amount;

        public DepositRequest() {}
        public DepositRequest(BigDecimal amount) { this.amount = amount; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    /** Response payload returning payment execution status and multi-tier deduction split. */
    public static class PaymentResponse {
        private String referenceNumber;
        private String status;
        private BigDecimal totalAmount;
        private String primaryWalletName;
        private BigDecimal primaryAmountDeducted = BigDecimal.ZERO;
        private String overflowWalletName;
        private BigDecimal overflowAmountDeducted = BigDecimal.ZERO;
        private String backupWalletName;
        private BigDecimal backupAmountDeducted = BigDecimal.ZERO;
        private BigDecimal remainingPrimaryBalance;
        private BigDecimal remainingGeneralBalance;
        private LocalDateTime timestamp;
        private String message;

        public PaymentResponse() {}

        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public String getPrimaryWalletName() { return primaryWalletName; }
        public void setPrimaryWalletName(String primaryWalletName) { this.primaryWalletName = primaryWalletName; }
        public BigDecimal getPrimaryAmountDeducted() { return primaryAmountDeducted; }
        public void setPrimaryAmountDeducted(BigDecimal primaryAmountDeducted) { this.primaryAmountDeducted = primaryAmountDeducted; }
        public String getOverflowWalletName() { return overflowWalletName; }
        public void setOverflowWalletName(String overflowWalletName) { this.overflowWalletName = overflowWalletName; }
        public BigDecimal getOverflowAmountDeducted() { return overflowAmountDeducted; }
        public void setOverflowAmountDeducted(BigDecimal overflowAmountDeducted) { this.overflowAmountDeducted = overflowAmountDeducted; }
        public String getBackupWalletName() { return backupWalletName; }
        public void setBackupWalletName(String backupWalletName) { this.backupWalletName = backupWalletName; }
        public BigDecimal getBackupAmountDeducted() { return backupAmountDeducted; }
        public void setBackupAmountDeducted(BigDecimal backupAmountDeducted) { this.backupAmountDeducted = backupAmountDeducted; }
        public BigDecimal getRemainingPrimaryBalance() { return remainingPrimaryBalance; }
        public void setRemainingPrimaryBalance(BigDecimal remainingPrimaryBalance) { this.remainingPrimaryBalance = remainingPrimaryBalance; }
        public BigDecimal getRemainingGeneralBalance() { return remainingGeneralBalance; }
        public void setRemainingGeneralBalance(BigDecimal remainingGeneralBalance) { this.remainingGeneralBalance = remainingGeneralBalance; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /** Request payload for inter-account transfers via account number or UPI ID. */
    public static class AccountTransferRequest {
        private String toAccountNumber;

        private String toUpiId;

        /** Optional source wallet. Defaults to the sender's General wallet when omitted. */
        private Long fromWalletId;

        /** Optional backup/secondary wallet when primary & General wallets cannot cover full amount */
        private Long backupWalletId;

        @NotNull
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        private BigDecimal amount;

        private String description;

        public AccountTransferRequest() {}
        public AccountTransferRequest(String toAccountNumber, BigDecimal amount, String description) {
            this.toAccountNumber = toAccountNumber;
            this.amount = amount;
            this.description = description;
        }
        public String getToAccountNumber() { return toAccountNumber; }
        public void setToAccountNumber(String toAccountNumber) { this.toAccountNumber = toAccountNumber; }
        public String getToUpiId() { return toUpiId; }
        public void setToUpiId(String toUpiId) { this.toUpiId = toUpiId; }
        public Long getFromWalletId() { return fromWalletId; }
        public void setFromWalletId(Long fromWalletId) { this.fromWalletId = fromWalletId; }
        public Long getBackupWalletId() { return backupWalletId; }
        public void setBackupWalletId(Long backupWalletId) { this.backupWalletId = backupWalletId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /** Option descriptor for a wallet eligible to act as secondary backup. */
    public static class BackupWalletOption {
        private Long walletId;
        private String name;
        private BigDecimal balance;

        public BackupWalletOption(Long walletId, String name, BigDecimal balance) {
            this.walletId = walletId;
            this.name = name;
            this.balance = balance;
        }

        public Long getWalletId() { return walletId; }
        public String getName() { return name; }
        public BigDecimal getBalance() { return balance; }
    }

    /** Error response payload prompting the user to select an eligible backup wallet. */
    public static class InsufficientFundsResponse {
        private String error = "INSUFFICIENT_FUNDS_NEED_BACKUP";
        private BigDecimal requestedAmount;
        private BigDecimal availablePrimary;
        private BigDecimal availableGeneral;
        private BigDecimal deficit;
        private List<BackupWalletOption> eligibleWallets;
        private String message;

        public InsufficientFundsResponse(BigDecimal requestedAmount, BigDecimal availablePrimary, BigDecimal availableGeneral, BigDecimal deficit, List<BackupWalletOption> eligibleWallets, String message) {
            this.requestedAmount = requestedAmount;
            this.availablePrimary = availablePrimary;
            this.availableGeneral = availableGeneral;
            this.deficit = deficit;
            this.eligibleWallets = eligibleWallets;
            this.message = message;
        }

        public String getError() { return error; }
        public BigDecimal getRequestedAmount() { return requestedAmount; }
        public BigDecimal getAvailablePrimary() { return availablePrimary; }
        public BigDecimal getAvailableGeneral() { return availableGeneral; }
        public BigDecimal getDeficit() { return deficit; }
        public List<BackupWalletOption> getEligibleWallets() { return eligibleWallets; }
        public String getMessage() { return message; }
    }
}
