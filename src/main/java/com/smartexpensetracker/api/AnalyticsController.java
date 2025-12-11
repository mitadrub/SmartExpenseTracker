package com.smartexpensetracker.api;

import com.smartexpensetracker.service.AnalyticsService;
import com.smartexpensetracker.service.AnalyticsService.AnalyticsSummary;
import com.smartexpensetracker.service.AnalyticsService.Forecast;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/analytics/summary")
    public ResponseEntity<AnalyticsSummary> getSummary(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserDetails userDetails) {
        YearMonth ym = (month != null) ? YearMonth.parse(month) : YearMonth.now();
        return ResponseEntity.ok(analyticsService.getSummary(userDetails.getUsername(), ym));
    }

    @GetMapping("/analytics/trends")
    public ResponseEntity<Map<LocalDate, BigDecimal>> getTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (from == null)
            from = LocalDate.now().minusMonths(3);
        if (to == null)
            to = LocalDate.now();
        return ResponseEntity.ok(analyticsService.getTrends(userDetails.getUsername(), from, to));
    }

    @GetMapping("/forecast/next-month")
    public ResponseEntity<Forecast> getForecast(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getForecast(userDetails.getUsername()));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<String>> getAlerts(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getAlerts(userDetails.getUsername()));
    }
}
