package com.smartexpensetracker.service;

import com.smartexpensetracker.dao.BudgetRepository;
import com.smartexpensetracker.dao.ExpenseRepository;
import com.smartexpensetracker.dao.UserRepository;
import com.smartexpensetracker.model.Budget;
import com.smartexpensetracker.model.Category;
import com.smartexpensetracker.model.Expense;
import com.smartexpensetracker.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testCategory = new Category();
        testCategory.setId(10L);
        testCategory.setName("Groceries");
    }

    @Test
    void getAlerts_ShouldReturnAlert_WhenBudgetExceeded() {
        // Arrange
        YearMonth currentMonth = YearMonth.now();
        Budget budget = new Budget();
        budget.setAmount(new BigDecimal("100.00"));
        budget.setCategory(testCategory);
        budget.setMonth(currentMonth);

        Expense expense = new Expense();
        expense.setAmount(new BigDecimal("150.00")); // Over budget

        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(budgetRepository.findByUserIdAndMonth(testUser.getId(), currentMonth))
                .thenReturn(Collections.singletonList(budget));
        when(expenseRepository.findByUserIdAndCategoryIdAndDateBetween(eq(testUser.getId()), eq(testCategory.getId()),
                any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(expense));

        // Act
        List<String> alerts = analyticsService.getAlerts(testUser.getUsername());

        // Assert
        assertFalse(alerts.isEmpty());
        assertTrue(alerts.get(0).contains("Alert"));
        assertTrue(alerts.get(0).contains("Groceries"));
    }

    @Test
    void getAlerts_ShouldReturnWarning_WhenBudget80PercentReached() {
        // Arrange
        YearMonth currentMonth = YearMonth.now();
        Budget budget = new Budget();
        budget.setAmount(new BigDecimal("100.00"));
        budget.setCategory(testCategory);
        budget.setMonth(currentMonth);

        Expense expense = new Expense();
        expense.setAmount(new BigDecimal("90.00")); // 90% > 80%

        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(budgetRepository.findByUserIdAndMonth(testUser.getId(), currentMonth))
                .thenReturn(Collections.singletonList(budget));
        when(expenseRepository.findByUserIdAndCategoryIdAndDateBetween(eq(testUser.getId()), eq(testCategory.getId()),
                any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(expense));

        // Act
        List<String> alerts = analyticsService.getAlerts(testUser.getUsername());

        // Assert
        assertFalse(alerts.isEmpty());
        assertTrue(alerts.get(0).contains("Warning"));
    }

    @Test
    void getSummary_ShouldCalculateTotalCorrectly() {
        yearMonthSummaryTest();
    }

    private void yearMonthSummaryTest() {
        // Arrange
        YearMonth month = YearMonth.of(2025, 12);
        Expense e1 = new Expense();
        e1.setAmount(new BigDecimal("50.00"));
        e1.setCategory(testCategory);
        Expense e2 = new Expense();
        e2.setAmount(new BigDecimal("25.00"));
        e2.setCategory(testCategory);

        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(expenseRepository.findByUserIdAndDateBetween(eq(testUser.getId()), eq(month.atDay(1)),
                eq(month.atEndOfMonth())))
                .thenReturn(List.of(e1, e2));
        when(expenseRepository.findByUserIdAndDateBetween(eq(testUser.getId()), any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(List.of(e1, e2)); // Mock previous month same to avoid nulls if checked, or modify
                                              // invocation matching

        // Act
        AnalyticsService.AnalyticsSummary summary = analyticsService.getSummary(testUser.getUsername(), month);

        // Assert
        assertEquals(new BigDecimal("75.00"), summary.getTotal());
        assertEquals(new BigDecimal("75.00"), summary.getByCategory().get("Groceries"));
    }
}
