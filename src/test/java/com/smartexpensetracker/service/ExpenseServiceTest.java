package com.smartexpensetracker.service;

import com.smartexpensetracker.dao.CategoryRepository;
import com.smartexpensetracker.dao.ExpenseRepository;
import com.smartexpensetracker.dao.UserRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private User testUser;
    private Category testCategory;
    private Expense testExpense;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testCategory = new Category();
        testCategory.setId(10L);
        testCategory.setName("Food");
        testCategory.setUser(testUser);

        testExpense = new Expense();
        testExpense.setId(100L);
        testExpense.setAmount(BigDecimal.TEN);
        testExpense.setDate(LocalDate.now());
        testExpense.setDescription("Lunch");
        testExpense.setUser(testUser);
        testExpense.setCategory(testCategory);
    }

    @Test
    void getExpenses_ShouldReturnAllExpenses_WhenCategoryNull() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(expenseRepository.findByUserIdAndDateBetween(eq(1L), any(), any()))
                .thenReturn(Collections.singletonList(testExpense));

        List<Expense> result = expenseService.getExpenses("testuser", LocalDate.now(), LocalDate.now(), null);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(expenseRepository).findByUserIdAndDateBetween(any(), any(), any());
    }

    @Test
    void getExpenses_ShouldFilterByCategory_WhenCategoryProvided() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(expenseRepository.findByUserIdAndCategoryIdAndDateBetween(eq(1L), eq(10L), any(), any()))
                .thenReturn(Collections.singletonList(testExpense));

        List<Expense> result = expenseService.getExpenses("testuser", LocalDate.now(), LocalDate.now(), 10L);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(expenseRepository).findByUserIdAndCategoryIdAndDateBetween(any(), any(), any(), any());
    }

    @Test
    void getExpense_ShouldReturnExpense_WhenFoundAndAuthorized() {
        when(expenseRepository.findById(100L)).thenReturn(Optional.of(testExpense));

        Expense result = expenseService.getExpense(100L, "testuser");

        assertNotNull(result);
        assertEquals(100L, result.getId());
    }

    @Test
    void getExpense_ShouldThrow_WhenNotFound() {
        when(expenseRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> expenseService.getExpense(100L, "testuser"));
    }

    @Test
    void getExpense_ShouldThrow_WhenUserNotAuthorized() {
        User otherUser = new User();
        otherUser.setUsername("other");
        testExpense.setUser(otherUser);

        when(expenseRepository.findById(100L)).thenReturn(Optional.of(testExpense));

        assertThrows(RuntimeException.class, () -> expenseService.getExpense(100L, "testuser"));
    }

    @Test
    void createExpense_ShouldSaveExpense() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(testCategory));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

        Expense request = new Expense();
        request.setAmount(BigDecimal.TEN);
        request.setDate(LocalDate.now());
        request.setDescription("New Expense");

        Expense result = expenseService.createExpense(request, "testuser", 10L);

        assertNotNull(result);
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    void updateExpense_ShouldUpdateAndSave() {
        when(expenseRepository.findById(100L)).thenReturn(Optional.of(testExpense));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(testCategory));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

        Expense updateReq = new Expense();
        updateReq.setAmount(BigDecimal.valueOf(20));
        updateReq.setDate(LocalDate.now().plusDays(1));
        updateReq.setDescription("Updated");

        Expense result = expenseService.updateExpense(100L, updateReq, "testuser", 10L);

        assertEquals(BigDecimal.valueOf(20), testExpense.getAmount());
        assertEquals("Updated", testExpense.getDescription());
        verify(expenseRepository).save(testExpense);
    }

    @Test
    void deleteExpense_ShouldDelete() {
        when(expenseRepository.findById(100L)).thenReturn(Optional.of(testExpense));

        expenseService.deleteExpense(100L, "testuser");

        verify(expenseRepository).delete(testExpense);
    }
}
