package com.smartexpensetracker.service;

import com.smartexpensetracker.dao.ExpenseRepository;
import com.smartexpensetracker.dao.UserRepository;
import com.smartexpensetracker.model.Expense;
import com.smartexpensetracker.model.User;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @Data
    @Builder
    public static class AnalyticsSummary {
        private BigDecimal total;
        private Map<String, BigDecimal> byCategory;
        private BigDecimal monthOverMonthChange; // Percentage
    }

    @Data
    @Builder
    public static class Forecast {
        private BigDecimal predictedTotal;
        private Double confidence;
    }

    public AnalyticsSummary getSummary(String username, YearMonth month) {
        User user = userRepository.findByUsername(username).orElseThrow();
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(user.getId(), start, end);

        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byCategory = new HashMap<>();
        expenses.forEach(e -> {
            String catName = e.getCategory() != null ? e.getCategory().getName() : "Uncategorized";
            byCategory.merge(catName, e.getAmount(), BigDecimal::add);
        });

        // Previous month for MoM change
        LocalDate prevStart = start.minusMonths(1);
        LocalDate prevEnd = prevStart.plusMonths(1).minusDays(1);
        List<Expense> prevExpenses = expenseRepository.findByUserIdAndDateBetween(user.getId(), prevStart, prevEnd);
        BigDecimal prevTotal = prevExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal change = BigDecimal.ZERO;
        if (prevTotal.compareTo(BigDecimal.ZERO) > 0) {
            change = total.subtract(prevTotal).divide(prevTotal, 2, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return AnalyticsSummary.builder()
                .total(total)
                .byCategory(byCategory)
                .monthOverMonthChange(change)
                .build();
    }

    public Forecast getForecast(String username) {
        // Simple prediction: Average of last 3 months
        User user = userRepository.findByUsername(username).orElseThrow();
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusMonths(3).withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(1).minusDays(1);

        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(user.getId(), start, end);
        if (expenses.isEmpty()) {
            return Forecast.builder().predictedTotal(BigDecimal.ZERO).confidence(0.0).build();
        }

        BigDecimal total = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = total.divide(BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP);

        return Forecast.builder()
                .predictedTotal(avg)
                .confidence(0.7) // Static confidence for simple average
                .build();
    }

    public Map<LocalDate, BigDecimal> getTrends(String username, LocalDate from, LocalDate to) {
        User user = userRepository.findByUsername(username).orElseThrow();
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(user.getId(), from, to);

        Map<LocalDate, BigDecimal> trends = new HashMap<>();
        expenses.forEach(e -> trends.merge(e.getDate(), e.getAmount(), BigDecimal::add));
        return trends;
    }
}
