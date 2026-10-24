package com.banking.virtualwallet.service;

import com.banking.virtualwallet.dto.StatisticalMetricsResponse;
import com.banking.virtualwallet.dto.StatisticalMetricsResponse.RangeBucket;
import com.banking.virtualwallet.entity.Transaction;
import com.banking.virtualwallet.entity.VirtualWallet;
import com.banking.virtualwallet.repository.TransactionRepository;
import com.banking.virtualwallet.repository.VirtualWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service powering the Probability & Statistics (P&S) descriptive statistics engine.
 * Computes mean, median, sample variance, quartiles, and frequency probability distributions.
 */
@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final VirtualWalletRepository walletRepository;

    public AnalyticsService(TransactionRepository transactionRepository, VirtualWalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    /**
     * Probability & Statistics (P&S) Analytics Engine.
     * Computes descriptive statistics over completed transactions for a specified primary wallet:
     * - Mean, Median, Range (Min / Max)
     * - Sample Variance: sum((x - mean)^2) / (n - 1) and Standard Deviation
     * - Quartiles (Q1, Q3) and Interquartile Range (IQR = Q3 - Q1)
     * - Frequency Distribution categorized into ₹100-step range buckets
     * - Empirical Probability: P(X > threshold)
     */
    @Transactional(readOnly = true)
    public StatisticalMetricsResponse calculateWalletStatistics(Long walletId, BigDecimal customThreshold) {
        VirtualWallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with id: " + walletId));

        List<Transaction> transactions = transactionRepository.findCompletedByPrimaryWalletId(walletId);

        StatisticalMetricsResponse response = new StatisticalMetricsResponse();
        response.setWalletId(wallet.getId());
        response.setWalletName(wallet.getName());
        response.setTransactionCount(transactions.size());

        if (transactions.isEmpty()) {
            response.setTotalSpent(BigDecimal.ZERO);
            response.setMean(BigDecimal.ZERO);
            response.setMedian(BigDecimal.ZERO);
            response.setVariance(BigDecimal.ZERO);
            response.setStandardDeviation(BigDecimal.ZERO);
            response.setMinAmount(BigDecimal.ZERO);
            response.setMaxAmount(BigDecimal.ZERO);
            response.setRange(BigDecimal.ZERO);
            response.setFirstQuartile(BigDecimal.ZERO);
            response.setThirdQuartile(BigDecimal.ZERO);
            response.setInterquartileRange(BigDecimal.ZERO);
            response.setFrequencyDistribution(buildDefaultBuckets());
            response.setProbabilityGreaterThanThreshold(0.0);
            response.setThresholdQueried(customThreshold != null ? customThreshold : new BigDecimal("500"));
            return response;
        }

        // Extract sorted amounts
        List<Double> amounts = new ArrayList<>();
        BigDecimal totalSpent = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            amounts.add(t.getAmount().doubleValue());
            totalSpent = totalSpent.add(t.getAmount());
        }
        Collections.sort(amounts);
        int n = amounts.size();

        response.setTotalSpent(totalSpent.setScale(2, RoundingMode.HALF_UP));

        // 1. Mean
        double meanVal = totalSpent.doubleValue() / n;
        response.setMean(BigDecimal.valueOf(meanVal).setScale(2, RoundingMode.HALF_UP));

        // 2. Median
        double medianVal = computeMedian(amounts);
        response.setMedian(BigDecimal.valueOf(medianVal).setScale(2, RoundingMode.HALF_UP));

        // 3. Variance and Standard Deviation (Sample Variance)
        double varianceVal = 0.0;
        if (n > 1) {
            double sumSquaredDiff = 0.0;
            for (Double x : amounts) {
                sumSquaredDiff += Math.pow(x - meanVal, 2);
            }
            varianceVal = sumSquaredDiff / (n - 1);
        }
        double stdDevVal = Math.sqrt(varianceVal);
        response.setVariance(BigDecimal.valueOf(varianceVal).setScale(2, RoundingMode.HALF_UP));
        response.setStandardDeviation(BigDecimal.valueOf(stdDevVal).setScale(2, RoundingMode.HALF_UP));

        // 4. Min, Max, Range
        double minVal = amounts.get(0);
        double maxVal = amounts.get(n - 1);
        double rangeVal = maxVal - minVal;
        response.setMinAmount(BigDecimal.valueOf(minVal).setScale(2, RoundingMode.HALF_UP));
        response.setMaxAmount(BigDecimal.valueOf(maxVal).setScale(2, RoundingMode.HALF_UP));
        response.setRange(BigDecimal.valueOf(rangeVal).setScale(2, RoundingMode.HALF_UP));

        // 5. Quartiles (Q1, Q3, IQR)
        double q1Val;
        double q3Val;
        int mid = n / 2;
        if (n == 1) {
            q1Val = amounts.get(0);
            q3Val = amounts.get(0);
        } else if (n % 2 == 0) {
            List<Double> lowerHalf = amounts.subList(0, mid);
            List<Double> upperHalf = amounts.subList(mid, n);
            q1Val = computeMedian(lowerHalf);
            q3Val = computeMedian(upperHalf);
        } else {
            List<Double> lowerHalf = amounts.subList(0, mid);
            List<Double> upperHalf = amounts.subList(mid + 1, n);
            q1Val = computeMedian(lowerHalf);
            q3Val = computeMedian(upperHalf);
        }
        double iqrVal = q3Val - q1Val;
        response.setFirstQuartile(BigDecimal.valueOf(q1Val).setScale(2, RoundingMode.HALF_UP));
        response.setThirdQuartile(BigDecimal.valueOf(q3Val).setScale(2, RoundingMode.HALF_UP));
        response.setInterquartileRange(BigDecimal.valueOf(iqrVal).setScale(2, RoundingMode.HALF_UP));

        // 6. Frequency Distribution Categorized by Ranges (from course presentation)
        List<RangeBucket> buckets = buildFrequencyBuckets(amounts);
        response.setFrequencyDistribution(buckets);

        // 7. Probability Measures e.g. P(X > threshold)
        BigDecimal threshold = customThreshold != null ? customThreshold : new BigDecimal("500");
        response.setThresholdQueried(threshold);
        double thresholdVal = threshold.doubleValue();
        long countAboveThreshold = amounts.stream().filter(val -> val > thresholdVal).count();
        double probAboveThreshold = (double) countAboveThreshold / n;
        response.setProbabilityGreaterThanThreshold(Math.round(probAboveThreshold * 10000.0) / 10000.0);

        return response;
    }

    /**
     * Calculates the median value from a sorted list of transaction amounts.
     */
    private double computeMedian(List<Double> sortedList) {
        int size = sortedList.size();
        if (size == 0) return 0.0;
        if (size % 2 == 1) {
            return sortedList.get(size / 2);
        } else {
            return (sortedList.get(size / 2 - 1) + sortedList.get(size / 2)) / 2.0;
        }
    }

    /**
     * Bins transaction amounts into course-specified frequency ranges (₹0-100, ₹100-200, ..., ₹500+)
     * and computes the relative empirical probability for each bucket.
     */
    private List<RangeBucket> buildFrequencyBuckets(List<Double> amounts) {
        int n = amounts.size();
        int count0_100 = 0;
        int count100_200 = 0;
        int count200_300 = 0;
        int count300_400 = 0;
        int count400_500 = 0;
        int count500Plus = 0;

        for (Double amt : amounts) {
            if (amt < 100) count0_100++;
            else if (amt < 200) count100_200++;
            else if (amt < 300) count200_300++;
            else if (amt < 400) count300_400++;
            else if (amt < 500) count400_500++;
            else count500Plus++;
        }

        List<RangeBucket> buckets = new ArrayList<>();
        buckets.add(new RangeBucket("₹0 - ₹100", 0, 100, count0_100, roundProb((double) count0_100 / n)));
        buckets.add(new RangeBucket("₹100 - ₹200", 100, 200, count100_200, roundProb((double) count100_200 / n)));
        buckets.add(new RangeBucket("₹200 - ₹300", 200, 300, count200_300, roundProb((double) count200_300 / n)));
        buckets.add(new RangeBucket("₹300 - ₹400", 300, 400, count300_400, roundProb((double) count300_400 / n)));
        buckets.add(new RangeBucket("₹400 - ₹500", 400, 500, count400_500, roundProb((double) count400_500 / n)));
        buckets.add(new RangeBucket("₹500+", 500, Double.MAX_VALUE, count500Plus, roundProb((double) count500Plus / n)));
        return buckets;
    }

    /**
     * Constructs default empty frequency buckets for a wallet with no transactions.
     */
    private List<RangeBucket> buildDefaultBuckets() {
        List<RangeBucket> buckets = new ArrayList<>();
        buckets.add(new RangeBucket("₹0 - ₹100", 0, 100, 0, 0.0));
        buckets.add(new RangeBucket("₹100 - ₹200", 100, 200, 0, 0.0));
        buckets.add(new RangeBucket("₹200 - ₹300", 200, 300, 0, 0.0));
        buckets.add(new RangeBucket("₹300 - ₹400", 300, 400, 0, 0.0));
        buckets.add(new RangeBucket("₹400 - ₹500", 400, 500, 0, 0.0));
        buckets.add(new RangeBucket("₹500+", 500, Double.MAX_VALUE, 0, 0.0));
        return buckets;
    }

    private double roundProb(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }
}
