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
    private final com.smartexpensetracker.dao.BudgetRepository budgetRepository;

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

    public java.util.List<String> getAlerts(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        YearMonth currentMonth = YearMonth.now();

        java.util.List<com.smartexpensetracker.model.Budget> budgets = budgetRepository
                .findByUserIdAndMonth(user.getId(), currentMonth);
        java.util.List<String> alerts = java.util.ArrayList.class.cast(new java.util.ArrayList<>());

        // Re-initializing to be safe with standard List import
        alerts = new java.util.ArrayList<>();

        for (com.smartexpensetracker.model.Budget budget : budgets) {
            BigDecimal limit = budget.getAmount();
            BigDecimal spent = BigDecimal.ZERO;

            LocalDate start = currentMonth.atDay(1);
            LocalDate end = currentMonth.atEndOfMonth();

            if (budget.getCategory() != null) {
                // Category-specific budget
                List<Expense> expenses = expenseRepository.findByUserIdAndCategoryIdAndDateBetween(
                        user.getId(), budget.getCategory().getId(), start, end);
                spent = expenses.stream()
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else {
                // Overall budget
                List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(user.getId(), start, end);
                spent = expenses.stream()
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            if (spent.compareTo(limit) > 0) {
                String catName = budget.getCategory() != null ? budget.getCategory().getName() : "Total";
                alerts.add("Alert: You have exceeded your " + catName + " budget! ($" + spent + " / $" + limit + ")");
            } else if (spent.compareTo(limit.multiply(BigDecimal.valueOf(0.8))) > 0) {
                String catName = budget.getCategory() != null ? budget.getCategory().getName() : "Total";
                alerts.add("Warning: You have reached 80% of your " + catName + " budget. ($" + spent + " / $" + limit
                        + ")");
            }
        }

        return alerts;
    }
}
