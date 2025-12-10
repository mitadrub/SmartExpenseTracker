package com.smartexpensetracker.api;

import com.smartexpensetracker.model.Budget;
import com.smartexpensetracker.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<Budget>> getBudgets(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserDetails userDetails) {
        YearMonth ym = null;
        if (month != null) {
            ym = YearMonth.parse(month);
        }
        return ResponseEntity.ok(budgetService.getBudgets(userDetails.getUsername(), ym));
    }

    @PostMapping
    public ResponseEntity<Budget> createBudget(
            @RequestBody Budget budget,
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long catId = categoryId;
        if (catId == null && budget.getCategory() != null) {
            catId = budget.getCategory().getId();
        }
        return ResponseEntity.ok(budgetService.createBudget(budget, userDetails.getUsername(), catId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(
            @PathVariable Long id,
            @RequestBody Budget budget,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(budgetService.updateBudget(id, budget, userDetails.getUsername()));
    }
}
