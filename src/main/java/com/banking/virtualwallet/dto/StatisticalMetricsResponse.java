package com.banking.virtualwallet.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response payload carrying full descriptive statistics and empirical probability metrics
 * computed over completed transactions for a specified virtual wallet.
 */
public class StatisticalMetricsResponse {

    private Long walletId;
    private String walletName;
    private int transactionCount;
    private BigDecimal totalSpent;
    private BigDecimal mean;
    private BigDecimal median;
    private BigDecimal variance;
    private BigDecimal standardDeviation;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal range;
    private BigDecimal firstQuartile; // Q1
    private BigDecimal thirdQuartile; // Q3
    private BigDecimal interquartileRange; // IQR
    private List<RangeBucket> frequencyDistribution;
    private Double probabilityGreaterThanThreshold;
    private BigDecimal thresholdQueried;

    public static class RangeBucket {
        private String label;
        private double min;
        private double max;
        private int count;
        private double probability; // count / N

        public RangeBucket() {}

        public RangeBucket(String label, double min, double max, int count, double probability) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.count = count;
            this.probability = probability;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public double getMin() { return min; }
        public void setMin(double min) { this.min = min; }
        public double getMax() { return max; }
        public void setMax(double max) { this.max = max; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public double getProbability() { return probability; }
        public void setProbability(double probability) { this.probability = probability; }
    }

    public StatisticalMetricsResponse() {}

    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }

    public String getWalletName() { return walletName; }
    public void setWalletName(String walletName) { this.walletName = walletName; }

    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }

    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

    public BigDecimal getMean() { return mean; }
    public void setMean(BigDecimal mean) { this.mean = mean; }

    public BigDecimal getMedian() { return median; }
    public void setMedian(BigDecimal median) { this.median = median; }

    public BigDecimal getVariance() { return variance; }
    public void setVariance(BigDecimal variance) { this.variance = variance; }

    public BigDecimal getStandardDeviation() { return standardDeviation; }
    public void setStandardDeviation(BigDecimal standardDeviation) { this.standardDeviation = standardDeviation; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public BigDecimal getRange() { return range; }
    public void setRange(BigDecimal range) { this.range = range; }

    public BigDecimal getFirstQuartile() { return firstQuartile; }
    public void setFirstQuartile(BigDecimal firstQuartile) { this.firstQuartile = firstQuartile; }

    public BigDecimal getThirdQuartile() { return thirdQuartile; }
    public void setThirdQuartile(BigDecimal thirdQuartile) { this.thirdQuartile = thirdQuartile; }

    public BigDecimal getInterquartileRange() { return interquartileRange; }
    public void setInterquartileRange(BigDecimal interquartileRange) { this.interquartileRange = interquartileRange; }

    public List<RangeBucket> getFrequencyDistribution() { return frequencyDistribution; }
    public void setFrequencyDistribution(List<RangeBucket> frequencyDistribution) { this.frequencyDistribution = frequencyDistribution; }

    public Double getProbabilityGreaterThanThreshold() { return probabilityGreaterThanThreshold; }
    public void setProbabilityGreaterThanThreshold(Double probabilityGreaterThanThreshold) { this.probabilityGreaterThanThreshold = probabilityGreaterThanThreshold; }

    public BigDecimal getThresholdQueried() { return thresholdQueried; }
    public void setThresholdQueried(BigDecimal thresholdQueried) { this.thresholdQueried = thresholdQueried; }
}
