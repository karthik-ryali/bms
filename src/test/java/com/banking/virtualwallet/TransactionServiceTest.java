package com.banking.virtualwallet;

import com.banking.virtualwallet.dto.TransactionDTOs.PaymentRequest;
import com.banking.virtualwallet.dto.TransactionDTOs.PaymentResponse;
import com.banking.virtualwallet.entity.Account;
import com.banking.virtualwallet.entity.Transaction;
import com.banking.virtualwallet.entity.User;
import com.banking.virtualwallet.entity.VirtualWallet;
import com.banking.virtualwallet.repository.AccountRepository;
import com.banking.virtualwallet.repository.TransactionRepository;
import com.banking.virtualwallet.repository.VirtualWalletRepository;
import com.banking.virtualwallet.service.InsufficientFundsNeedBackupException;
import com.banking.virtualwallet.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private VirtualWalletRepository walletRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account account;
    private VirtualWallet generalWallet;
    private VirtualWallet foodWallet;
    private VirtualWallet travelWallet;

    @BeforeEach
    void setUp() {
        User user = new User("karthik", "pass", "Karthik", "k@k.com", "ROLE_USER");
        account = new Account("ACC-100234", new BigDecimal("10000.00"), user);
        account.setId(1L);

        generalWallet = new VirtualWallet("General", "General", new BigDecimal("2000.00"), true, account);
        generalWallet.setId(10L);

        foodWallet = new VirtualWallet("Food", "Dining", new BigDecimal("2000.00"), false, account);
        foodWallet.setId(20L);

        travelWallet = new VirtualWallet("Travel", "Transit", new BigDecimal("1500.00"), false, account);
        travelWallet.setId(30L);
    }

    @Test
    void testDirectPaymentTier1() {
        when(accountRepository.findByUserUsername("karthik")).thenReturn(Optional.of(account));
        when(walletRepository.findById(20L)).thenReturn(Optional.of(foodWallet));
        when(walletRepository.findByAccountIdAndIsDefaultGeneralTrue(1L)).thenReturn(Optional.of(generalWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        PaymentRequest request = new PaymentRequest(20L, new BigDecimal("500.00"), "Lunch");
        PaymentResponse response = transactionService.processPayment("karthik", request);

        assertEquals("COMPLETED", response.getStatus());
        assertEquals(new BigDecimal("500.00"), response.getPrimaryAmountDeducted());
        assertEquals(BigDecimal.ZERO, response.getOverflowAmountDeducted());
        assertEquals(new BigDecimal("1500.00"), foodWallet.getBalance());
        assertEquals(new BigDecimal("9500.00"), account.getTotalBalance());
    }

    @Test
    void testGeneralWalletOverflowTier2() {
        // Scenario from slide 208-213:
        // Food Wallet = ₹2,000, Payment = ₹3,000 -> Food covers ₹2,000, General covers ₹1,000
        when(accountRepository.findByUserUsername("karthik")).thenReturn(Optional.of(account));
        when(walletRepository.findById(20L)).thenReturn(Optional.of(foodWallet));
        when(walletRepository.findByAccountIdAndIsDefaultGeneralTrue(1L)).thenReturn(Optional.of(generalWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        PaymentRequest request = new PaymentRequest(20L, new BigDecimal("3000.00"), "Grand Dinner Buffet");
        PaymentResponse response = transactionService.processPayment("karthik", request);

        assertEquals("COMPLETED", response.getStatus());
        assertEquals(new BigDecimal("2000.00"), response.getPrimaryAmountDeducted());
        assertEquals(new BigDecimal("1000.00"), response.getOverflowAmountDeducted());
        assertEquals(BigDecimal.ZERO, foodWallet.getBalance());
        assertEquals(new BigDecimal("1000.00"), generalWallet.getBalance()); // 2000 - 1000
        assertEquals(new BigDecimal("7000.00"), account.getTotalBalance()); // 10000 - 3000
    }

    @Test
    void testBackupWalletPromptTier3WhenGeneralExhausted() {
        // Food = ₹2,000, General = ₹2,000, Payment = ₹5,000 -> Total available in Food+Gen = ₹4,000 < ₹5,000.
        // Should throw InsufficientFundsNeedBackupException
        when(accountRepository.findByUserUsername("karthik")).thenReturn(Optional.of(account));
        when(walletRepository.findById(20L)).thenReturn(Optional.of(foodWallet));
        when(walletRepository.findByAccountIdAndIsDefaultGeneralTrue(1L)).thenReturn(Optional.of(generalWallet));
        when(walletRepository.findByAccountId(1L)).thenReturn(List.of(generalWallet, foodWallet, travelWallet));

        PaymentRequest request = new PaymentRequest(20L, new BigDecimal("5000.00"), "Catering Order");

        assertThrows(InsufficientFundsNeedBackupException.class, () -> {
            transactionService.processPayment("karthik", request);
        });
    }
}
