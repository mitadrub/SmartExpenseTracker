package com.smartexpensetracker.dao;

import com.smartexpensetracker.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);

    List<Budget> findByUserIdAndMonth(Long userId, YearMonth month);
}
