package com.smartexpensetracker.service;

import com.smartexpensetracker.dao.BudgetRepository;
import com.smartexpensetracker.dao.CategoryRepository;
import com.smartexpensetracker.dao.ExpenseRepository;
import com.smartexpensetracker.dao.UserRepository;
import com.smartexpensetracker.model.Budget;
import com.smartexpensetracker.model.Category;
import com.smartexpensetracker.model.Expense;
import com.smartexpensetracker.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test") // Ensures H2 is used if profile configured, or falls back to default
class AnalyticsServiceIntegrationTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Category category;

    @BeforeEach
    void setUp() {
        // Clear DB
        expenseRepository.deleteAll();
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Create User
        testUser = new User();
        testUser.setFirstName("Integration");
        testUser.setLastName("User");
        testUser.setUsername("integrationUser");
        testUser.setPassword(passwordEncoder.encode("password")); // Real encoding
        testUser = userRepository.save(testUser);

        // Create Category
        category = new Category();
        category.setName("Integration Category");
        category.setUser(testUser);
        category = categoryRepository.save(category);
    }

    @Test
    void getSummary_ShouldCalculateSummaryCorrectly_WithRealDB() {
        YearMonth currentMonth = YearMonth.now();

        // Add Expenses
        Expense e1 = new Expense();
        e1.setUser(testUser);
        e1.setCategory(category);
        e1.setAmount(new BigDecimal("100.00"));
        e1.setDate(LocalDate.now());
        e1.setDescription("Test Expense 1");
        expenseRepository.save(e1);

        Expense e2 = new Expense();
        e2.setUser(testUser);
        e2.setCategory(category);
        e2.setAmount(new BigDecimal("50.00"));
        e2.setDate(LocalDate.now());
        e2.setDescription("Test Expense 2");
        expenseRepository.save(e2);

        // Act
        AnalyticsService.AnalyticsSummary summary = analyticsService.getSummary(testUser.getUsername(), currentMonth);

        // Assert
        assertNotNull(summary);
        assertEquals(0, new BigDecimal("150.00").compareTo(summary.getTotal()));
        assertEquals(0, new BigDecimal("150.00").compareTo(summary.getByCategory().get("Integration Category")));
    }

    @Test
    void getAlerts_ShouldDetectOverBudget_WithRealDB() {
        YearMonth currentMonth = YearMonth.now();

        // Define Budget
        Budget budget = new Budget();
        budget.setUser(testUser);
        budget.setCategory(category);
        budget.setAmount(new BigDecimal("100.00"));
        budget.setMonth(currentMonth);
        budgetRepository.save(budget);

        // Add Over-Budget Expense
        Expense e1 = new Expense();
        e1.setUser(testUser);
        e1.setCategory(category);
        e1.setAmount(new BigDecimal("150.00"));
        e1.setDate(LocalDate.now());
        e1.setDescription("Expensive Item");
        expenseRepository.save(e1);

        // Act
        List<String> alerts = analyticsService.getAlerts(testUser.getUsername());

        // Assert
        assertFalse(alerts.isEmpty(), "Alerts should not be empty");
        assertTrue(alerts.get(0).contains("exceeded"), "Should contain exceeded message");
    }

    @Test
    void getForecast_ShouldCalculateAvg_WithRealDB() {
        // Add expenses for past 3 months
        LocalDate now = LocalDate.now();

        // Month 1 (current - 1)
        Expense e1 = new Expense();
        e1.setUser(testUser);
        e1.setAmount(new BigDecimal("300.00"));
        e1.setDate(now.minusMonths(1).withDayOfMonth(15));
        expenseRepository.save(e1);

        // Month 2 (current - 2)
        Expense e2 = new Expense();
        e2.setUser(testUser);
        e2.setAmount(new BigDecimal("300.00"));
        e2.setDate(now.minusMonths(2).withDayOfMonth(15));
        expenseRepository.save(e2);

        // Month 3 (current - 3)
        Expense e3 = new Expense();
        e3.setUser(testUser);
        e3.setAmount(new BigDecimal("300.00"));
        e3.setDate(now.minusMonths(3).withDayOfMonth(15));
        expenseRepository.save(e3);

        // Act
        AnalyticsService.Forecast forecast = analyticsService.getForecast(testUser.getUsername());

        // Assert
        // Logic: (300 + 300 + 300) / 3 = 300
        // NOTE: getForecast logic uses `start = now.minusMonths(3).withDayOfMonth(1)`
        // and `end = now.withDayOfMonth(1).minusDays(1)`
        // This range covers [M-3, M-1] FULL months completely.

        assertNotNull(forecast);
        assertEquals(0, new BigDecimal("300.00").compareTo(forecast.getPredictedTotal()));
    }
}
