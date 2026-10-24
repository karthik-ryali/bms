package com.banking.virtualwallet.controller;

import com.banking.virtualwallet.dto.StatisticalMetricsResponse;
import com.banking.virtualwallet.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Probability & Statistics Analytics Engine", description = "Calculates descriptive statistics (Mean, Median, Variance, SD, Quartiles) and empirical probability distributions per wallet")
public class AnalyticsRestController {

    private final AnalyticsService analyticsService;

    public AnalyticsRestController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Computes full descriptive statistics and probability metrics for transactions under a specified wallet.
     */
    @GetMapping("/{walletId}")
    @Operation(summary = "Calculate wallet statistics and probability metrics",
               description = "Computes Mean, Median, Variance, Standard Deviation, Range, Quartiles (Q1, Q3, IQR), Frequency Distribution ranges, and P(X > threshold) for the specified wallet.")
    public ResponseEntity<StatisticalMetricsResponse> getWalletAnalytics(
            @PathVariable("walletId") Long walletId,
            @RequestParam(value = "threshold", required = false) BigDecimal threshold) {
        return ResponseEntity.ok(analyticsService.calculateWalletStatistics(walletId, threshold));
    }

    /**
     * Convenience alias endpoint matching course review specifications for wallet statistics.
     */
    @GetMapping("/wallet/{walletId}")
    @Operation(summary = "Alias for wallet statistics (Matches review presentation)",
               description = "Direct alias for /api/analytics/{walletId} matching Review 1 Presentation Slide 321.")
    public ResponseEntity<StatisticalMetricsResponse> getWalletAnalyticsAlias(
            @PathVariable("walletId") Long walletId,
            @RequestParam(value = "threshold", required = false) BigDecimal threshold) {
        return ResponseEntity.ok(analyticsService.calculateWalletStatistics(walletId, threshold));
    }
}
