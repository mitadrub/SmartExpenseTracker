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
        User user = getUser(username);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(user.getId(), start, end);

        BigDecimal total = calculateTotal(expenses);
        Map<String, BigDecimal> byCategory = calculateByCategory(expenses);
        BigDecimal change = calculateMonthOverMonthChange(user, start, total);

        return AnalyticsSummary.builder()
                .total(total)
                .byCategory(byCategory)
                .monthOverMonthChange(change)
                .build();
    }

    public Forecast getForecast(String username) {
        User user = getUser(username);
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusMonths(3).withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(1).minusDays(1);

        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(user.getId(), start, end);
        if (expenses.isEmpty()) {
            return Forecast.builder().predictedTotal(BigDecimal.ZERO).confidence(0.0).build();
        }

        BigDecimal total = calculateTotal(expenses);
        BigDecimal avg = total.divide(BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP);

        return Forecast.builder()
                .predictedTotal(avg)
                .confidence(0.7)
                .build();
    }

    public Map<LocalDate, BigDecimal> getTrends(String username, LocalDate from, LocalDate to) {
        User user = getUser(username);
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(user.getId(), from, to);

        Map<LocalDate, BigDecimal> trends = new HashMap<>();
        expenses.forEach(e -> trends.merge(e.getDate(), e.getAmount(), BigDecimal::add));
        return trends;
    }

    public List<String> getAlerts(String username) {
        User user = getUser(username);
        YearMonth currentMonth = YearMonth.now();
        List<com.smartexpensetracker.model.Budget> budgets = budgetRepository.findByUserIdAndMonth(user.getId(),
                currentMonth);
        List<String> alerts = new java.util.ArrayList<>();

        for (com.smartexpensetracker.model.Budget budget : budgets) {
            checkBudget(budget, user, currentMonth, alerts);
        }

        return alerts;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username).orElseThrow();
    }

    private BigDecimal calculateTotal(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, BigDecimal> calculateByCategory(List<Expense> expenses) {
        Map<String, BigDecimal> byCategory = new HashMap<>();
        expenses.forEach(e -> {
            String catName = e.getCategory() != null ? e.getCategory().getName() : "Uncategorized";
            byCategory.merge(catName, e.getAmount(), BigDecimal::add);
        });
        return byCategory;
    }

    private BigDecimal calculateMonthOverMonthChange(User user, LocalDate currentStart, BigDecimal currentTotal) {
        LocalDate prevStart = currentStart.minusMonths(1);
        LocalDate prevEnd = prevStart.plusMonths(1).minusDays(1);
        List<Expense> prevExpenses = expenseRepository.findByUserIdAndDateBetween(user.getId(), prevStart, prevEnd);
        BigDecimal prevTotal = calculateTotal(prevExpenses);

        if (prevTotal.compareTo(BigDecimal.ZERO) > 0) {
            return currentTotal.subtract(prevTotal).divide(prevTotal, 2, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

    private void checkBudget(com.smartexpensetracker.model.Budget budget, User user, YearMonth month,
            List<String> alerts) {
        BigDecimal limit = budget.getAmount();
        BigDecimal spent = calculateSpentForBudget(budget, user, month);

        if (spent.compareTo(limit) > 0) {
            String catName = getCategoryName(budget);
            alerts.add("Alert: You have exceeded your " + catName + " budget! ($" + spent + " / $" + limit + ")");
        } else if (spent.compareTo(limit.multiply(BigDecimal.valueOf(0.8))) > 0) {
            String catName = getCategoryName(budget);
            alerts.add(
                    "Warning: You have reached 80% of your " + catName + " budget. ($" + spent + " / $" + limit + ")");
        }
    }

    private BigDecimal calculateSpentForBudget(com.smartexpensetracker.model.Budget budget, User user,
            YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        if (budget.getCategory() != null) {
            List<Expense> expenses = expenseRepository.findByUserIdAndCategoryIdAndDateBetween(
                    user.getId(), budget.getCategory().getId(), start, end);
            return calculateTotal(expenses);
        } else {
            List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(user.getId(), start, end);
            return calculateTotal(expenses);
        }
    }

    private String getCategoryName(com.smartexpensetracker.model.Budget budget) {
        return budget.getCategory() != null ? budget.getCategory().getName() : "Total";
    }
}
