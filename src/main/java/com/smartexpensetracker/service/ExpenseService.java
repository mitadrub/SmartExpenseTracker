package com.smartexpensetracker.service;

import com.smartexpensetracker.dao.CategoryRepository;
import com.smartexpensetracker.dao.ExpenseRepository;
import com.smartexpensetracker.dao.UserRepository;
import com.smartexpensetracker.model.Category;
import com.smartexpensetracker.model.Expense;
import com.smartexpensetracker.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public List<Expense> getExpenses(String username, LocalDate from, LocalDate to, Long categoryId) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (categoryId != null) {
            return expenseRepository.findByUserIdAndCategoryIdAndDateBetween(user.getId(), categoryId, from, to);
        }
        return expenseRepository.findByUserIdAndDateBetween(user.getId(), from, to);
    }

    public Expense getExpense(Long id, String username) {
        return expenseRepository.findById(id)
                .filter(e -> e.getUser().getUsername().equals(username))
                .orElseThrow(() -> new RuntimeException("Expense not found or access denied"));
    }

    public Expense createExpense(Expense expenseRequest, String username, Long categoryId) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Category category = resolveCategory(categoryId, expenseRequest);

        Expense expense = Expense.builder()
                .amount(expenseRequest.getAmount())
                .date(expenseRequest.getDate())
                .description(expenseRequest.getDescription())
                .category(category)
                .user(user)
                .build();
        return expenseRepository.save(expense);
    }

    public Expense updateExpense(Long id, Expense expenseRequest, String username, Long categoryId) {
        Expense existing = getExpense(id, username);

        Category category = existing.getCategory();
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId).orElse(category);
        } else if (expenseRequest.getCategory() != null && expenseRequest.getCategory().getId() != null) {
            category = categoryRepository.findById(expenseRequest.getCategory().getId()).orElse(category);
        }

        existing.setAmount(expenseRequest.getAmount());
        existing.setDate(expenseRequest.getDate());
        existing.setDescription(expenseRequest.getDescription());
        existing.setCategory(category);

        return expenseRepository.save(existing);
    }

    private Category resolveCategory(Long categoryId, Expense expenseRequest) {
        if (categoryId != null) {
            return categoryRepository.findById(categoryId).orElse(null);
        }
        if (expenseRequest.getCategory() != null && expenseRequest.getCategory().getId() != null) {
            return categoryRepository.findById(expenseRequest.getCategory().getId()).orElse(null);
        }
        return null;
    }

    public void deleteExpense(Long id, String username) {
        Expense existing = getExpense(id, username);
        expenseRepository.delete(existing);
    }
}
