package com.dduk.domain.accounting.journal;

import com.dduk.domain.accounting.AccountingConstants;
import com.dduk.domain.accounting.ledger.Account;
import com.dduk.domain.accounting.ledger.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 회계 전표 서비스 (Accounting Engine Core)
 *
 * 상태 흐름:
 *   DRAFT → APPROVED → POSTED → REVERSED
 *
 * - POSTED 이후 수정/삭제 불가 (immutable)
 * - 역분개(REVERSED)는 새 전표 생성 + 원본 markReversed() 처리
 */
@Service
@RequiredArgsConstructor
public class AccountingService {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final JournalValidationService journalValidationService;

    // ===================== CRUD / 조회 =====================

    @Transactional(readOnly = true)
    public JournalEntry findById(Long id) {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("전표를 찾을 수 없습니다. id=" + id));
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> findAll() {
        return journalEntryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> findByFiscal(int year, int month) {
        return journalEntryRepository.findByFiscalYearAndFiscalMonth(year, month);
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> findByStatus(String status) {
        return journalEntryRepository.findByStatus(status);
    }

    // ===================== 전표 생성 =====================

    /**
     * 전표 생성 (DRAFT 상태)
     */
    @Transactional
    public JournalEntry createJournal(LocalDate date, String description,
                                      List<JournalLineRequest> lineRequests,
                                      String sourceType, Long sourceId) {
        journalValidationService.validatePeriodNotClosed(date);
        journalValidationService.validateLines(lineRequests);

        // 중복 전표 체크 (동일 원천)
        if (sourceType != null && sourceId != null
                && journalEntryRepository.existsBySourceTypeAndSourceId(sourceType, sourceId)) {
            throw new IllegalStateException("이미 해당 원천에 대한 전표가 존재합니다: " + sourceType + " #" + sourceId);
        }

        JournalEntry entry = JournalEntry.builder()
                .journalNo(generateJournalNo())
                .transactionDate(date)
                .description(description)
                .status(AccountingConstants.JOURNAL_STATUS_DRAFT)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .totalDebit(BigDecimal.ZERO)
                .totalCredit(BigDecimal.ZERO)
                .build();

        for (JournalLineRequest req : lineRequests) {
            Account account = accountRepository.findByCode(req.getAccountCode())
                    .orElseThrow(() -> new IllegalArgumentException("계정코드를 찾을 수 없습니다: " + req.getAccountCode()));

            JournalLine line = JournalLine.builder()
                    .account(account)
                    .debitAmount(req.getDebitAmount() != null ? req.getDebitAmount() : BigDecimal.ZERO)
                    .creditAmount(req.getCreditAmount() != null ? req.getCreditAmount() : BigDecimal.ZERO)
                    .description(req.getDescription())
                    .referenceType(req.getReferenceType())
                    .referenceId(req.getReferenceId())
                    .build();
            entry.addLine(line);
        }

        return journalEntryRepository.save(entry);
    }

    // ===================== 상태 전이 =====================

    /** DRAFT → APPROVED */
    @Transactional
    public JournalEntry approveJournal(Long id) {
        JournalEntry entry = findById(id);
        entry.approve();
        return journalEntryRepository.save(entry);
    }

    /** APPROVED → POSTED (불변 전환) */
    @Transactional
    public JournalEntry postJournal(Long id) {
        JournalEntry entry = findById(id);
        entry.post();
        return journalEntryRepository.save(entry);
    }

    /**
     * 역분개 (POSTED → REVERSED + 역분개 전표 생성)
     * - 원본 전표 markReversed()
     * - 차대변 반전된 새 전표 생성 후 POSTED
     */
    @Transactional
    public JournalEntry reverseJournal(Long id) {
        JournalEntry original = findById(id);
        journalValidationService.validatePeriodNotClosed(original.getTransactionDate());

        // 역분개 라인 생성 (차대변 반전)
        List<JournalLineRequest> reversedLines = new ArrayList<>();
        for (JournalLine line : original.getLines()) {
            JournalLineRequest req = new JournalLineRequest();
            req.setAccountCode(line.getAccount().getCode());
            req.setDebitAmount(line.getCreditAmount());   // 반전
            req.setCreditAmount(line.getDebitAmount());   // 반전
            req.setDescription("[역분개] " + (line.getDescription() != null ? line.getDescription() : ""));
            req.setReferenceType(original.getSourceType());
            req.setReferenceId(original.getId());
            reversedLines.add(req);
        }

        // 역분개 전표 생성 → APPROVED → POSTED
        JournalEntry reversal = createJournal(
                LocalDate.now(),
                "[역분개] " + original.getDescription(),
                reversedLines,
                AccountingConstants.SOURCE_REVERSAL,
                original.getId()
        );
        reversal.approve();
        reversal.post();
        journalEntryRepository.save(reversal);

        // 원본 전표 REVERSED 처리
        original.markReversed();
        journalEntryRepository.save(original);

        return reversal;
    }

    /**
     * 전표 삭제 (DRAFT 상태만 허용)
     */
    @Transactional
    public void deleteJournal(Long id) {
        JournalEntry entry = findById(id);
        if (!AccountingConstants.JOURNAL_STATUS_DRAFT.equals(entry.getStatus())) {
            throw new IllegalStateException("DRAFT 상태의 전표만 삭제할 수 있습니다.");
        }
        journalEntryRepository.delete(entry);
    }

    // ===================== 내부 헬퍼 =====================

    /**
     * 자동분개 전략 연동용: 생성 후 즉시 POSTED 상태로 전환
     */
    @Transactional
    public JournalEntry createAndPostJournal(LocalDate date, String description,
                                             List<JournalLineRequest> lineRequests,
                                             String sourceType, Long sourceId) {
        JournalEntry entry = createJournal(date, description, lineRequests, sourceType, sourceId);
        entry.approve();
        entry.post();
        return journalEntryRepository.save(entry);
    }

    /**
     * 하위 호환: Map 기반 아이템 입력 지원 (레거시 API용)
     */
    @Transactional
    public JournalEntry createAndPost(LocalDate date, String description,
                                      List<Map<String, Object>> items,
                                      String sourceType, Long sourceId) {
        List<JournalLineRequest> lineRequests = new ArrayList<>();
        for (Map<String, Object> item : items) {
            JournalLineRequest req = new JournalLineRequest();
            req.setAccountCode((String) item.get("accountCode"));
            Object amt = item.get("amount");
            BigDecimal amount = new BigDecimal(amt.toString());
            String side = (String) item.get("side");
            if ("DEBIT".equals(side)) {
                req.setDebitAmount(amount);
                req.setCreditAmount(BigDecimal.ZERO);
            } else {
                req.setDebitAmount(BigDecimal.ZERO);
                req.setCreditAmount(amount);
            }
            lineRequests.add(req);
        }
        return createAndPostJournal(date, description, lineRequests, sourceType, sourceId);
    }

    private String generateJournalNo() {
        return "JRN-" + System.currentTimeMillis();
    }
}
