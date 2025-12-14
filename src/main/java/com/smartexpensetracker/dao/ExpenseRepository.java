package com.smartexpensetracker.dao;

import com.smartexpensetracker.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
        List<Expense> findByUserId(Long userId);

        @Query("SELECT e FROM Expense e WHERE e.user.id = :userId AND e.date BETWEEN :startDate AND :endDate")
        List<Expense> findByUserIdAndDateBetween(@Param("userId") Long userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT e FROM Expense e WHERE e.user.id = :userId AND e.category.id = :categoryId AND e.date BETWEEN :startDate AND :endDate")
        List<Expense> findByUserIdAndCategoryIdAndDateBetween(@Param("userId") Long userId,
                        @Param("categoryId") Long categoryId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        List<Expense> findByUserIdAndCategoryId(Long userId, Long categoryId);

        @Query("SELECT e FROM Expense e WHERE e.user.id = :userId " +
                        "AND (:categoryId IS NULL OR e.category.id = :categoryId) " +
                        "AND (cast(:startDate as date) IS NULL OR e.date >= :startDate) " +
                        "AND (cast(:endDate as date) IS NULL OR e.date <= :endDate) " +
                        "AND (:minAmount IS NULL OR e.amount >= :minAmount) " +
                        "AND (:maxAmount IS NULL OR e.amount <= :maxAmount)")
        List<Expense> findFilteredExpenses(@Param("userId") Long userId,
                        @Param("categoryId") Long categoryId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("minAmount") java.math.BigDecimal minAmount,
                        @Param("maxAmount") java.math.BigDecimal maxAmount);
}
