package com.dduk.domain.accounting.journal;

import com.dduk.domain.accounting.period.AccountingPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 전표 유효성 검증 서비스
 * - 복식부기 검증 (차변 합계 == 대변 합계)
 * - 음수 금액 방지
 * - 빈 라인 방지
 * - POSTED 상태 수정 금지
 * - 마감 기간 전표 생성 금지
 */
@Service
@RequiredArgsConstructor
public class JournalValidationService {

    private final AccountingPeriodRepository accountingPeriodRepository;

    /** 전표 라인 유효성 + 복식부기 검증 */
    public void validateLines(List<JournalLineRequest> lineRequests) {
        if (lineRequests == null || lineRequests.isEmpty()) {
            throw new IllegalArgumentException("전표 라인이 비어 있습니다.");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (JournalLineRequest req : lineRequests) {
            if (req.getDebitAmount() == null) req.setDebitAmount(BigDecimal.ZERO);
            if (req.getCreditAmount() == null) req.setCreditAmount(BigDecimal.ZERO);

            if (req.getDebitAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("차변 금액은 음수일 수 없습니다.");
            }
            if (req.getCreditAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("대변 금액은 음수일 수 없습니다.");
            }
            if (req.getDebitAmount().compareTo(BigDecimal.ZERO) == 0
                    && req.getCreditAmount().compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("차변 또는 대변 중 하나는 0보다 커야 합니다.");
            }
            if (req.getDebitAmount().compareTo(BigDecimal.ZERO) > 0
                    && req.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("하나의 라인에 차변과 대변 금액을 동시에 입력할 수 없습니다.");
            }

            totalDebit = totalDebit.add(req.getDebitAmount());
            totalCredit = totalCredit.add(req.getCreditAmount());
        }

        if (totalDebit.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("차변 합계가 0입니다. 유효한 분개를 입력하세요.");
        }
        if (totalCredit.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("대변 합계가 0입니다. 유효한 분개를 입력하세요.");
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException(
                    "차대 불일치: 차변(" + totalDebit + ") ≠ 대변(" + totalCredit + ")");
        }
    }

    /** 회계 마감 기간 검증 */
    public void validatePeriodNotClosed(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        boolean isClosed = accountingPeriodRepository
                .existsByFiscalYearAndFiscalMonthAndStatus(year, month, "CLOSED");
        if (isClosed) {
            throw new IllegalStateException(
                    "마감된 회계 기간(" + year + "-" + String.format("%02d", month) + ")에는 전표를 생성할 수 없습니다.");
        }
    }

    /** POSTED/REVERSED 상태 전표 수정 불가 검증 */
    public void validateMutable(JournalEntry entry) {
        entry.assertNotPosted();
    }
}
