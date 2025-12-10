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

        existing.setAmount(budgetRequest.getAmount());
        // Allow updating month? Usually fixed. Let's allow amount update mainly.
        return budgetRepository.save(existing);
    }
}
