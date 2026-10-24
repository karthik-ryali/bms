package com.banking.virtualwallet;

import com.banking.virtualwallet.dto.WalletDTOs.WalletCreateRequest;
import com.banking.virtualwallet.dto.WalletDTOs.WalletResponse;
import com.banking.virtualwallet.dto.WalletDTOs.WalletTransferRequest;
import com.banking.virtualwallet.entity.Account;
import com.banking.virtualwallet.entity.Transaction;
import com.banking.virtualwallet.entity.User;
import com.banking.virtualwallet.entity.VirtualWallet;
import com.banking.virtualwallet.repository.AccountRepository;
import com.banking.virtualwallet.repository.TransactionRepository;
import com.banking.virtualwallet.repository.VirtualWalletRepository;
import com.banking.virtualwallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private VirtualWalletRepository walletRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletService walletService;

    private Account account;
    private VirtualWallet generalWallet;
    private VirtualWallet foodWallet;

    @BeforeEach
    void setUp() {
        User user = new User("karthik", "pass", "Karthik", "k@k.com", "ROLE_USER");
        account = new Account("ACC-100234", new BigDecimal("10000.00"), user);
        account.setId(1L);

        generalWallet = new VirtualWallet("General", "General", new BigDecimal("3500.00"), true, account);
        generalWallet.setId(10L);

        foodWallet = new VirtualWallet("Food", "Dining", new BigDecimal("2000.00"), false, account);
        foodWallet.setId(20L);
    }

    @Test
    void testCreateWalletDeductsFromGeneralWallet() {
        when(accountRepository.findByUserUsername("karthik")).thenReturn(Optional.of(account));
        when(walletRepository.existsByAccountIdAndNameIgnoreCase(1L, "Travel")).thenReturn(false);
        when(walletRepository.findByAccountIdAndIsDefaultGeneralTrue(1L)).thenReturn(Optional.of(generalWallet));
        when(walletRepository.save(any(VirtualWallet.class))).thenAnswer(i -> {
            VirtualWallet w = i.getArgument(0);
            w.setId(30L);
            return w;
        });

        WalletCreateRequest request = new WalletCreateRequest();
        request.setName("Travel");
        request.setCategory("Transit");
        request.setInitialAllocation(new BigDecimal("1000.00"));

        WalletResponse created = walletService.createWallet("karthik", request);

        assertEquals("Travel", created.getName());
        assertEquals(new BigDecimal("1000.00"), created.getBalance());
        assertEquals(new BigDecimal("2500.00"), generalWallet.getBalance()); // 3500 - 1000
    }

    @Test
    void testTransferBetweenWallets() {
        when(accountRepository.findByUserUsername("karthik")).thenReturn(Optional.of(account));
        when(walletRepository.findById(10L)).thenReturn(Optional.of(generalWallet));
        when(walletRepository.findById(20L)).thenReturn(Optional.of(foodWallet));

        WalletTransferRequest request = new WalletTransferRequest();
        request.setFromWalletId(10L);
        request.setToWalletId(20L);
        request.setAmount(new BigDecimal("500.00"));

        walletService.transferBetweenWallets("karthik", request);

        assertEquals(new BigDecimal("3000.00"), generalWallet.getBalance()); // 3500 - 500
        assertEquals(new BigDecimal("2500.00"), foodWallet.getBalance());    // 2000 + 500
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testDeleteWalletRefundsToGeneral() {
        when(accountRepository.findByUserUsername("karthik")).thenReturn(Optional.of(account));
        when(walletRepository.findById(20L)).thenReturn(Optional.of(foodWallet));
        when(walletRepository.findByAccountIdAndIsDefaultGeneralTrue(1L)).thenReturn(Optional.of(generalWallet));

        walletService.deleteWallet("karthik", 20L);

        assertEquals(new BigDecimal("5500.00"), generalWallet.getBalance()); // 3500 + 2000
        verify(walletRepository, times(1)).delete(foodWallet);
    }
}
