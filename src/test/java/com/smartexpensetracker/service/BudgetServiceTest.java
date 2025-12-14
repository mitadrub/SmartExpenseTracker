package com.smartexpensetracker.service;

import com.smartexpensetracker.dao.BudgetRepository;
import com.smartexpensetracker.dao.CategoryRepository;
import com.smartexpensetracker.dao.UserRepository;
import com.smartexpensetracker.model.Budget;
import com.smartexpensetracker.model.Category;
import com.smartexpensetracker.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    private Category testCategory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Food");

        testBudget = new Budget();
        testBudget.setId(10L);
        testBudget.setAmount(BigDecimal.valueOf(100));
        testBudget.setMonth(YearMonth.now());
        testBudget.setUser(testUser);
    }

    // --- Get Budgets Tests ---

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

    // --- Create Budget Tests ---

    @Test
    void createBudget_ShouldSave_WhenValid() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);
        // Mock validation: findByUserIdAndMonth returns empty list so no limits exist
        when(budgetRepository.findByUserIdAndMonth(anyLong(), any())).thenReturn(Collections.emptyList());

        Budget req = new Budget();
        req.setAmount(BigDecimal.valueOf(500));
        req.setMonth(YearMonth.now());

        Budget result = budgetService.createBudget(req, "testuser", 1L);
        assertNotNull(result);
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void createBudget_ShouldFailValidation_WhenCategoryExceedsOverall() {
        // Setup existing Overall Budget of 100
        Budget overallBudget = new Budget();
        overallBudget.setAmount(BigDecimal.valueOf(100));
        overallBudget.setMonth(YearMonth.now());
        overallBudget.setCategory(null); // Overall
        overallBudget.setUser(testUser);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        // Mock validation: return existing overall budget
        when(budgetRepository.findByUserIdAndMonth(anyLong(), any())).thenReturn(List.of(overallBudget));

        Budget req = new Budget();
        req.setAmount(BigDecimal.valueOf(150)); // Try to set 150 when limit is 100
        req.setMonth(YearMonth.now());

        assertThrows(IllegalArgumentException.class, () -> budgetService.createBudget(req, "testuser", 1L));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    // --- Update Budget Tests ---

    @Test
    void updateBudget_ShouldUpdateAmount_WhenValid() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);
        // Mock validation: empty list means no constraints
        when(budgetRepository.findByUserIdAndMonth(anyLong(), any())).thenReturn(Collections.emptyList());

        Budget updateReq = new Budget();
        updateReq.setAmount(BigDecimal.valueOf(999));
        updateReq.setMonth(YearMonth.now());

        Budget result = budgetService.updateBudget(10L, updateReq, "testuser");
        assertEquals(BigDecimal.valueOf(999), testBudget.getAmount());
        verify(budgetRepository).save(testBudget);
    }

    @Test
    void updateBudget_ShouldExcludingSelfValidation_WhenUpdatingCategoryBudget() {
        // Setup Overall Budget = 200
        Budget overallBudget = new Budget();
        overallBudget.setAmount(BigDecimal.valueOf(200));
        overallBudget.setCategory(null);

        // Setup Current Budget (ID 10) = 100
        testBudget.setCategory(testCategory);
        testBudget.setAmount(BigDecimal.valueOf(100));

        // Setup Another Category Budget = 50
        Budget otherBudget = new Budget();
        otherBudget.setId(11L);
        otherBudget.setCategory(new Category());
        otherBudget.setAmount(BigDecimal.valueOf(50));

        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));
        // Mock validation returns all these
        when(budgetRepository.findByUserIdAndMonth(anyLong(), any()))
                .thenReturn(List.of(overallBudget, testBudget, otherBudget));
        when(budgetRepository.save(any())).thenReturn(testBudget);

        // We want to update ID 10 to 150.
        // Logic: (Total Category - Self) + New
        // (100 + 50) - 100 + 150 = 200. Should be <= Overall (200). OK.
        Budget updateReq = new Budget();
        updateReq.setAmount(BigDecimal.valueOf(150)); // Increase to 150
        updateReq.setMonth(YearMonth.now());

        assertDoesNotThrow(() -> budgetService.updateBudget(10L, updateReq, "testuser"));

        // Try updating to 151 -> Total 201 > 200. FAIL.
        updateReq.setAmount(BigDecimal.valueOf(151));
        assertThrows(IllegalArgumentException.class, () -> budgetService.updateBudget(10L, updateReq, "testuser"));
    }

    @Test
    void updateBudget_ShouldValidate_WhenReducingOverallLimit() {
        // Update Overall Budget (ID 10 is now overall)
        testBudget.setCategory(null);
        testBudget.setAmount(BigDecimal.valueOf(200));

        // Existing Category Budget = 150
        Budget catBudget = new Budget();
        catBudget.setId(11L);
        catBudget.setCategory(testCategory);
        catBudget.setAmount(BigDecimal.valueOf(150));

        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));
        when(budgetRepository.findByUserIdAndMonth(anyLong(), any())).thenReturn(List.of(testBudget, catBudget));

        // Try reducing overall to 100 (which is < 150 allocated). Should Fail.
        Budget updateReq = new Budget();
        updateReq.setAmount(BigDecimal.valueOf(100));

        assertThrows(IllegalArgumentException.class, () -> budgetService.updateBudget(10L, updateReq, "testuser"));

        // Try reducing overall to 150. Should Pass.
        updateReq.setAmount(BigDecimal.valueOf(150));
        when(budgetRepository.save(any())).thenReturn(testBudget);
        assertDoesNotThrow(() -> budgetService.updateBudget(10L, updateReq, "testuser"));
    }

    @Test
    void updateBudget_ShouldThrow_WhenNotFound() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.empty());
        Budget updateReq = new Budget();
        assertThrows(RuntimeException.class, () -> budgetService.updateBudget(10L, updateReq, "testuser"));
    }

    // --- Delete Budget Tests ---

    @Test
    void deleteBudget_ShouldDelete_WhenFoundAndOwned() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));

        budgetService.deleteBudget(10L, "testuser");
        verify(budgetRepository).delete(testBudget);
    }

    @Test
    void deleteBudget_ShouldThrow_WhenNotFound() {
        when(budgetRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> budgetService.deleteBudget(10L, "testuser"));
    }

    @Test
    void deleteBudget_ShouldThrow_WhenNotOwned() {
        User otherUser = new User();
        otherUser.setUsername("other");
        testBudget.setUser(otherUser);

        when(budgetRepository.findById(10L)).thenReturn(Optional.of(testBudget));
        assertThrows(RuntimeException.class, () -> budgetService.deleteBudget(10L, "testuser"));
    }
}
