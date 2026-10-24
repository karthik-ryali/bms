package com.banking.virtualwallet.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing a ledger transaction recording debits, credits, and multi-tier wallet deductions.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String referenceNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "primary_wallet_id", nullable = false)
    private VirtualWallet primaryWallet;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal primaryAmountDeducted = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "overflow_wallet_id")
    private VirtualWallet overflowWallet;

    @Column(precision = 15, scale = 2)
    private BigDecimal overflowAmountDeducted = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "backup_wallet_id")
    private VirtualWallet backupWallet;

    @Column(precision = 15, scale = 2)
    private BigDecimal backupAmountDeducted = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "COMPLETED";

    @Column(nullable = false, length = 20)
    private String type = "PAYMENT"; // PAYMENT, TRANSFER, DEPOSIT, RECEIVED

    /**
     * Cash-flow direction of the entry relative to the owning account:
     * CREDIT (money in), DEBIT (money out), INTERNAL (wallet-to-wallet move).
     * Kept explicit so the UI never has to guess inflow/outflow from `type`.
     */
    @Column(length = 10)
    private String direction = "DEBIT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @JsonIgnore
    private Account account;

    public Transaction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /**
     * Unique transaction reference identifier (e.g. TXN-12345678).
     */
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    /**
     * Absolute transaction amount. Always returned as a positive value.
     */
    public BigDecimal getAmount() { return amount != null ? amount.abs() : BigDecimal.ZERO; }
    public void setAmount(BigDecimal amount) { this.amount = amount != null ? amount.abs() : BigDecimal.ZERO; }

    /**
     * Human-readable description or remark for the transaction.
     */
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public VirtualWallet getPrimaryWallet() { return primaryWallet; }
    public void setPrimaryWallet(VirtualWallet primaryWallet) { this.primaryWallet = primaryWallet; }

    public BigDecimal getPrimaryAmountDeducted() { return primaryAmountDeducted; }
    public void setPrimaryAmountDeducted(BigDecimal primaryAmountDeducted) { this.primaryAmountDeducted = primaryAmountDeducted; }

    public VirtualWallet getOverflowWallet() { return overflowWallet; }
    public void setOverflowWallet(VirtualWallet overflowWallet) { this.overflowWallet = overflowWallet; }

    public BigDecimal getOverflowAmountDeducted() { return overflowAmountDeducted; }
    public void setOverflowAmountDeducted(BigDecimal overflowAmountDeducted) { this.overflowAmountDeducted = overflowAmountDeducted; }

    public VirtualWallet getBackupWallet() { return backupWallet; }
    public void setBackupWallet(VirtualWallet backupWallet) { this.backupWallet = backupWallet; }

    public BigDecimal getBackupAmountDeducted() { return backupAmountDeducted; }
    public void setBackupAmountDeducted(BigDecimal backupAmountDeducted) { this.backupAmountDeducted = backupAmountDeducted; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
}
