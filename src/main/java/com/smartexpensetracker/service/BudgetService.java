package com.smartexpensetracker.service;

import com.smartexpensetracker.dao.BudgetRepository;
import com.smartexpensetracker.dao.CategoryRepository;
import com.smartexpensetracker.dao.UserRepository;
import com.smartexpensetracker.model.Budget;
import com.smartexpensetracker.model.Category;
import com.smartexpensetracker.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public List<Budget> getBudgets(String username, YearMonth month) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (month != null) {
            return budgetRepository.findByUserIdAndMonth(user.getId(), month);
        }
        return budgetRepository.findByUserId(user.getId());
    }

    public Budget createBudget(Budget budgetRequest, String username, Long categoryId) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId).orElse(null);
        }

        validateBudgetLimit(user, budgetRequest.getMonth(), budgetRequest.getAmount(), categoryId, null);

        Budget budget = Budget.builder()
                .amount(budgetRequest.getAmount())
                .month(budgetRequest.getMonth())
                .category(category)
                .user(user)
                .build();
        return budgetRepository.save(budget);
    }

    public Budget updateBudget(Long id, Budget budgetRequest, String username) {
        Budget existing = budgetRepository.findById(id)
                .filter(b -> b.getUser().getUsername().equals(username))
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        validateBudgetLimit(existing.getUser(), existing.getMonth(), budgetRequest.getAmount(),
                existing.getCategory() != null ? existing.getCategory().getId() : null, id);

        existing.setAmount(budgetRequest.getAmount());
        return budgetRepository.save(existing);
    }

    public void deleteBudget(Long id, String username) {
        Budget budget = budgetRepository.findById(id)
                .filter(b -> b.getUser().getUsername().equals(username))
                .orElseThrow(() -> new RuntimeException("Budget not found or access denied"));
        budgetRepository.delete(budget);
    }

    private void validateBudgetLimit(User user, YearMonth month, java.math.BigDecimal newAmount, Long categoryId,
            Long currentBudgetId) {
        List<Budget> budgets = budgetRepository.findByUserIdAndMonth(user.getId(), month);

        java.math.BigDecimal overallLimit = budgets.stream()
                .filter(b -> b.getCategory() == null)
                .findFirst()
                .map(Budget::getAmount)
                .orElse(java.math.BigDecimal.ZERO); // Or MAX_VALUE if no overall limit implies no limit? Assuming 0
                                                    // means no limit set yet.

        if (categoryId == null) {
            // Updating Overall Budget
            // Check if new overall limit < sum of category budgets
            java.math.BigDecimal sumCategoryBudgets = budgets.stream()
                    .filter(b -> b.getCategory() != null)
                    .map(Budget::getAmount)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            if (newAmount.compareTo(sumCategoryBudgets) < 0) {
                throw new IllegalArgumentException(
                        "Overall budget cannot be less than total allocated category budgets (" + sumCategoryBudgets
                                + ")");
            }
        } else {
            // Updating Category Budget
            if (overallLimit.compareTo(java.math.BigDecimal.ZERO) == 0) {
                // No overall limit set, so any amount is fine (or maybe warn?)
                return;
            }

            java.math.BigDecimal otherCategoryBudgets = budgets.stream()
                    .filter(b -> b.getCategory() != null && !b.getId().equals(currentBudgetId))
                    .map(Budget::getAmount)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            java.math.BigDecimal totalAfterUpdate = otherCategoryBudgets.add(newAmount);

            if (totalAfterUpdate.compareTo(overallLimit) > 0) {
                throw new IllegalArgumentException("Total category budgets (" + totalAfterUpdate
                        + ") exceed Overall Budget limit (" + overallLimit + ")");
            }
        }
    }
}
