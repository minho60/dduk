package com.dduk.service.accounting;

import com.dduk.dto.accounting.ExpenseResponseDto;
import com.dduk.entity.accounting.Expense;
import com.dduk.repository.accounting.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Transactional
    public ExpenseResponseDto createExpense(
            Long employeeId,
            LocalDate expenseDate,
            String category,
            BigDecimal amount,
            String description,
            String receiptFilePath,
            String status
    ) {
        Expense expense = Expense.builder()
                .employeeId(employeeId)
                .expenseDate(expenseDate)
                .category(category)
                .amount(amount)
                .description(description)
                .receiptFilePath(receiptFilePath)
                .status(status)
                .build();

        return ExpenseResponseDto.fromEntity(expenseRepository.save(expense));
    }
}
