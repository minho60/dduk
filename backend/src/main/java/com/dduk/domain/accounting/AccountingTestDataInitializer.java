package com.dduk.domain.accounting;

import com.dduk.domain.accounting.journal.AccountingService;
import com.dduk.domain.accounting.journal.JournalLineRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 회계 테스트 데이터 이니셜라이저
 * - Profile: local 또는 dev 에서만 작동
 * - Property: app.accounting.seed=true 인 경우에만 실행
 */
@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
@Slf4j
public class AccountingTestDataInitializer {

    private final AccountingService accountingService;

    @Value("${app.accounting.seed:false}")
    private boolean seedEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (!seedEnabled) {
            log.info("Accounting test data seeding is disabled.");
            return;
        }

        log.info("Starting accounting test data seeding...");
        try {
            createSampleJournals();
            log.info("Accounting test data seeding completed successfully.");
        } catch (Exception e) {
            log.error("Failed to seed accounting test data: {}", e.getMessage());
        }
    }

    private void createSampleJournals() {
        // 이미 전표가 있으면 중복 생성 방지 (간이 체크)
        if (!accountingService.findAll().isEmpty()) {
            log.info("Journals already exist. Skipping seed.");
            return;
        }

        LocalDate today = LocalDate.now();

        // 1. 초기 자본금 전입
        createManualJournal(today.minusMonths(1).withDayOfMonth(1), "초기 자본금 납입", List.of(
                line(AccountingConstants.BANK_ACCOUNT, new BigDecimal("100000000"), BigDecimal.ZERO),
                line(AccountingConstants.CASH, BigDecimal.ZERO, new BigDecimal("100000000")) // 실제로는 자본금 계정이어야 하지만 상수를 사용
        ));

        // 2. 비품 구입 (현금)
        createManualJournal(today.minusDays(10), "사무용 비품 구입", List.of(
                line(AccountingConstants.WELFARE_EXPENSE, new BigDecimal("500000"), BigDecimal.ZERO),
                line(AccountingConstants.CASH, BigDecimal.ZERO, new BigDecimal("500000"))
        ));

        // 3. 매출 발생 (미수금)
        createManualJournal(today.minusDays(5), "상품 매출", List.of(
                line(AccountingConstants.ACCOUNTS_RECEIVABLE, new BigDecimal("2000000"), BigDecimal.ZERO),
                line(AccountingConstants.SALES_REVENUE, BigDecimal.ZERO, new BigDecimal("2000000"))
        ));
    }

    private void createManualJournal(LocalDate date, String desc, List<JournalLineRequest> lines) {
        accountingService.createAndPostJournal(date, desc, lines, AccountingConstants.SOURCE_MANUAL, null);
    }

    private JournalLineRequest line(String code, BigDecimal dr, BigDecimal cr) {
        JournalLineRequest req = new JournalLineRequest();
        req.setAccountCode(code);
        req.setDebitAmount(dr);
        req.setCreditAmount(cr);
        req.setDescription("테스트 데이터");
        return req;
    }
}
