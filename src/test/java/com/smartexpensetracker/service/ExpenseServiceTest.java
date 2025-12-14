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

    @Test
    void getExpenses_ShouldReturnAll_WhenDatesNull_AndCategoryNull() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(expenseRepository.findByUserId(1L)).thenReturn(Collections.singletonList(testExpense));

        List<Expense> result = expenseService.getExpenses("testuser", null, null, null);

        assertFalse(result.isEmpty());
        verify(expenseRepository).findByUserId(1L);
    }

    @Test
    void getExpenses_ShouldFilterByCategory_WhenDatesNull() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(expenseRepository.findByUserIdAndCategoryId(1L, 10L)).thenReturn(Collections.singletonList(testExpense));

        List<Expense> result = expenseService.getExpenses("testuser", null, null, 10L);

        assertFalse(result.isEmpty());
        verify(expenseRepository).findByUserIdAndCategoryId(1L, 10L);
    }

    @Test
    void getExpenses_ShouldSetDefaultFrom_WhenFromIsNull() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        // Expecting default from: 1 month ago, default to: provided to (now)
        when(expenseRepository.findByUserIdAndDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(testExpense));

        LocalDate toDate = LocalDate.now();
        List<Expense> result = expenseService.getExpenses("testuser", null, toDate, null);

        assertFalse(result.isEmpty());
        // Verify 'from' was calculated (approximately 1 month ago)
        verify(expenseRepository).findByUserIdAndDateBetween(eq(1L),
                argThat(date -> date.isBefore(LocalDate.now()) && date.isAfter(LocalDate.now().minusMonths(2))),
                eq(toDate));
    }

    @Test
    void getExpenses_ShouldSetDefaultTo_WhenToIsNull() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(expenseRepository.findByUserIdAndDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(testExpense));

        LocalDate fromDate = LocalDate.now().minusDays(5);
        List<Expense> result = expenseService.getExpenses("testuser", fromDate, null, null);

        assertFalse(result.isEmpty());
        // Verify 'to' was set to LocalDate.now()
        verify(expenseRepository).findByUserIdAndDateBetween(eq(1L), eq(fromDate), eq(LocalDate.now()));
    }

    @Test
    void updateExpense_ShouldUpdateCategoryFromBody_WhenParamNull() {
        when(expenseRepository.findById(100L)).thenReturn(Optional.of(testExpense));
        // Different category in body/repo
        Category newCategory = new Category();
        newCategory.setId(20L);
        newCategory.setName("Travel");

        when(categoryRepository.findById(20L)).thenReturn(Optional.of(newCategory));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Expense updateReq = new Expense();
        updateReq.setAmount(BigDecimal.valueOf(50));
        updateReq.setDate(LocalDate.now());
        updateReq.setDescription("Updated with body category");
        Category bodyCategory = new Category();
        bodyCategory.setId(20L);
        updateReq.setCategory(bodyCategory);

        Expense result = expenseService.updateExpense(100L, updateReq, "testuser", null);

        assertNotNull(result);
        assertEquals(20L, result.getCategory().getId());
        verify(categoryRepository).findById(20L);
    }

    @Test
    void createExpense_ShouldResolveCategoryFromBody_WhenParamNull() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        Category bodyCategory = new Category();
        bodyCategory.setId(30L);
        bodyCategory.setName("Utilities");

        when(categoryRepository.findById(30L)).thenReturn(Optional.of(bodyCategory));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Expense request = new Expense();
        request.setAmount(BigDecimal.valueOf(100));
        request.setDate(LocalDate.now());
        request.setDescription("New Utility");
        Category reqCat = new Category();
        reqCat.setId(30L);
        request.setCategory(reqCat);

        Expense result = expenseService.createExpense(request, "testuser", null);

        assertNotNull(result);
        assertEquals(30L, result.getCategory().getId());
        verify(categoryRepository).findById(30L);
    }

    @Test
    void createExpense_ShouldReturnNullCategory_WhenCategoryNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Expense request = new Expense();
        request.setAmount(BigDecimal.valueOf(10));
        request.setDate(LocalDate.now());
        request.setDescription("No Category");

        // Pass invalid ID
        Expense result = expenseService.createExpense(request, "testuser", 999L);

        assertNotNull(result);
        assertNull(result.getCategory());
        verify(categoryRepository).findById(999L);
    }
}
