package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByExpenseCode(String expenseCode);

    List<Expense> findByPeriodYearAndPeriodMonthOrderByExpenseDateDesc(Short year, Short month);

    Page<Expense> findByStatusOrderByExpenseDateDesc(String status, Pageable pageable);

    Page<Expense> findByCategoryOrderByExpenseDateDesc(String category, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE " +
           "(:category IS NULL OR e.category = :category) AND " +
           "(:status   IS NULL OR e.status   = :status)   AND " +
           "(:year     IS NULL OR e.periodYear  = :year)  AND " +
           "(:month    IS NULL OR e.periodMonth = :month) " +
           "ORDER BY e.expenseDate DESC")
    Page<Expense> findFiltered(String category, String status, Short year, Short month, Pageable pageable);

    /** Tổng chi phí theo kỳ và danh mục */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.periodYear = :year AND e.periodMonth = :month AND e.status = 'APPROVED'")
    BigDecimal sumApprovedByPeriod(Short year, Short month);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.periodYear = :year AND e.periodMonth = :month " +
           "AND e.category = :category AND e.status = 'APPROVED'")
    BigDecimal sumApprovedByPeriodAndCategory(Short year, Short month, String category);

    /** Danh sách danh mục và tổng từng danh mục trong kỳ */
    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.periodYear = :year AND e.periodMonth = :month AND e.status = 'APPROVED' " +
           "GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumByCategory(Short year, Short month);

    /** Chi phí lặp lại chưa được tạo tháng này */
    @Query("SELECT e FROM Expense e WHERE e.isRecurring = TRUE " +
           "AND e.periodYear = :year AND e.periodMonth = :month")
    List<Expense> findRecurringForPeriod(Short year, Short month);

    List<Expense> findByIsRecurringTrueAndStatus(String status);

    @Query("SELECT e FROM Expense e WHERE e.expenseDate BETWEEN :from AND :to ORDER BY e.expenseDate DESC")
    List<Expense> findByDateRange(LocalDate from, LocalDate to);

    Long countByStatus(String status);
}
