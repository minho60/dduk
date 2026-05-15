package com.dduk.repository.accounting;

import com.dduk.entity.accounting.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> saveExpenses(Expense expense);
    List<Expense> findByEmployeeId(Long employeeId);
    List<Expense> findByStatus(String status);
    List<Expense> findAllByOrderByIdDesc();
}
