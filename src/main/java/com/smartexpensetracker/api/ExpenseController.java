package com.smartexpensetracker.api;

import com.smartexpensetracker.model.Expense;
import com.smartexpensetracker.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<Expense>> getExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long category,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.getExpenses(userDetails.getUsername(), from, to, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpense(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.getExpense(id, userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<Expense> createExpense(
            @RequestBody Expense expense,
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (categoryId == null && expense.getCategory() != null) {
            categoryId = expense.getCategory().getId();
        }
        return ResponseEntity.ok(expenseService.createExpense(expense, userDetails.getUsername(), categoryId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(
            @PathVariable Long id,
            @RequestBody Expense expense,
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (categoryId == null && expense.getCategory() != null) {
            categoryId = expense.getCategory().getId();
        }
        return ResponseEntity.ok(expenseService.updateExpense(id, expense, userDetails.getUsername(), categoryId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        expenseService.deleteExpense(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
