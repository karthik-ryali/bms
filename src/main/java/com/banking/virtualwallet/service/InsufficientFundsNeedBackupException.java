package com.banking.virtualwallet.service;

import com.banking.virtualwallet.dto.TransactionDTOs.BackupWalletOption;

import java.math.BigDecimal;
import java.util.List;

/**
 * Exception thrown when primary and General wallets cannot cover a payment,
 * prompting the client to choose an eligible backup wallet.
 */
public class InsufficientFundsNeedBackupException extends RuntimeException {

    private final BigDecimal requestedAmount;
    private final BigDecimal availablePrimary;
    private final BigDecimal availableGeneral;
    private final BigDecimal deficit;
    private final List<BackupWalletOption> eligibleWallets;

    public InsufficientFundsNeedBackupException(BigDecimal requestedAmount,
                                                BigDecimal availablePrimary,
                                                BigDecimal availableGeneral,
                                                BigDecimal deficit,
                                                List<BackupWalletOption> eligibleWallets) {
        super("Primary and General wallets have insufficient funds. Please select an alternative wallet to cover the remaining deficit of ₹" + deficit);
        this.requestedAmount = requestedAmount;
        this.availablePrimary = availablePrimary;
        this.availableGeneral = availableGeneral;
        this.deficit = deficit;
        this.eligibleWallets = eligibleWallets;
    }

    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getAvailablePrimary() { return availablePrimary; }
    public BigDecimal getAvailableGeneral() { return availableGeneral; }
    public BigDecimal getDeficit() { return deficit; }
    public List<BackupWalletOption> getEligibleWallets() { return eligibleWallets; }
}
