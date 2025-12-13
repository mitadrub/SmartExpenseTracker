package com.smartexpensetracker.service;

import com.smartexpensetracker.dao.BudgetRepository;
import com.smartexpensetracker.dao.CategoryRepository;
import com.smartexpensetracker.dao.UserRepository;
import com.smartexpensetracker.model.Budget;
import com.smartexpensetracker.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BudgetService budgetService;

    private User testUser;
    private Budget testBudget;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testBudget = new Budget();
        testBudget.setId(10L);
        testBudget.setAmount(BigDecimal.valueOf(100));
        testBudget.setUser(testUser);
    }

    @Test
    void getBudgets_ShouldReturnAll_WhenMonthNull() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(budgetRepository.findByUserId(1L)).thenReturn(Collections.singletonList(testBudget));

        List<Budget> result = budgetService.getBudgets("testuser", null);
        assertEquals(1, result.size());
    }

    @Test
    void getBudgets_ShouldFilterByMonth_WhenMonthProvided() {
        YearMonth month = YearMonth.now();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(budgetRepository.findByUserIdAndMonth(1L, month)).thenReturn(Collections.singletonList(testBudget));

        List<Budget> result = budgetService.getBudgets("testuser", month);
        assertEquals(1, result.size());
    }

    @Test
    void createBudget_ShouldSave() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);

        Budget req = new Budget();
        req.setAmount(BigDecimal.valueOf(500));

        Budget result = budgetService.createBudget(req, "testuser", null);
        assertNotNull(result);
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void updateBudget_ShouldUpdateAmount() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);

        Budget updateReq = new Budget();
        updateReq.setAmount(BigDecimal.valueOf(999));

        Budget result = budgetService.updateBudget(10L, updateReq, "testuser");

        assertEquals(BigDecimal.valueOf(999), testBudget.getAmount());
        verify(budgetRepository).save(testBudget);
    }

    @Test
    void updateBudget_ShouldThrow_WhenNotFound() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.empty());
        Budget updateReq = new Budget();
        assertThrows(RuntimeException.class, () -> budgetService.updateBudget(10L, updateReq, "testuser"));
    }
}
