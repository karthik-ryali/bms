package com.banking.virtualwallet;

import com.banking.virtualwallet.dto.StatisticalMetricsResponse;
import com.banking.virtualwallet.entity.Account;
import com.banking.virtualwallet.entity.Transaction;
import com.banking.virtualwallet.entity.User;
import com.banking.virtualwallet.entity.VirtualWallet;
import com.banking.virtualwallet.repository.TransactionRepository;
import com.banking.virtualwallet.repository.VirtualWalletRepository;
import com.banking.virtualwallet.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private VirtualWalletRepository walletRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private VirtualWallet foodWallet;

    @BeforeEach
    void setUp() {
        User user = new User("karthik", "pass", "Karthik", "k@k.com", "ROLE_USER");
        Account account = new Account("ACC-1", new BigDecimal("10000.00"), user);
        foodWallet = new VirtualWallet("Food", "Dining", new BigDecimal("2000.00"), false, account);
        foodWallet.setId(10L);
    }

    @Test
    void testStatisticalCalculationsOnSampleData() {
        // Sample amounts matching slide 84: 200, 300, 400, 100 -> Mean = 250
        double[] amounts = { 100.0, 200.0, 300.0, 400.0 };
        List<Transaction> txList = new ArrayList<>();
        for (double a : amounts) {
            Transaction t = new Transaction();
            t.setAmount(BigDecimal.valueOf(a));
            txList.add(t);
        }

        when(walletRepository.findById(10L)).thenReturn(Optional.of(foodWallet));
        when(transactionRepository.findCompletedByPrimaryWalletId(10L)).thenReturn(txList);

        StatisticalMetricsResponse stats = analyticsService.calculateWalletStatistics(10L, new BigDecimal("250"));

        assertEquals(4, stats.getTransactionCount());
        assertEquals(new BigDecimal("250.00"), stats.getMean());
        assertEquals(new BigDecimal("250.00"), stats.getMedian());
        assertEquals(new BigDecimal("300.00"), stats.getRange()); // 400 - 100 = 300
        assertEquals(0.5, stats.getProbabilityGreaterThanThreshold()); // 300 and 400 are > 250 -> 2/4 = 0.5
    }
}
