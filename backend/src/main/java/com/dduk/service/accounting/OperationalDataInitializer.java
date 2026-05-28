package com.dduk.service.accounting;

import com.dduk.dto.accounting.payroll.PayrollLedgerCreateRequest;
import com.dduk.dto.accounting.payroll.PayrollLedgerResponse;
import com.dduk.dto.accounting.voucher.VoucherLineRequest;
import com.dduk.dto.accounting.voucher.VoucherRequest;
import com.dduk.dto.accounting.voucher.VoucherResponse;
import com.dduk.entity.accounting.Account;
import com.dduk.entity.accounting.AccountSide;
import com.dduk.entity.accounting.JournalEntry;
import com.dduk.entity.accounting.payroll.*;
import com.dduk.entity.accounting.period.AccountingPeriod;
import com.dduk.entity.accounting.period.AccountingPeriodStatus;
import com.dduk.entity.accounting.voucher.enums.VatType;
import com.dduk.entity.accounting.voucher.enums.VoucherStatus;
import com.dduk.entity.accounting.voucher.enums.VoucherType;
import com.dduk.entity.hr.Employee;
import com.dduk.entity.hr.PayrollContract;
import com.dduk.entity.inventory.Vendor;
import com.dduk.repository.accounting.AccountRepository;
import com.dduk.repository.accounting.JournalEntryRepository;
import com.dduk.repository.accounting.payroll.PayrollLedgerRepository;
import com.dduk.repository.accounting.period.AccountingPeriodRepository;
import com.dduk.repository.accounting.voucher.VoucherRepository;
import com.dduk.repository.hr.EmployeeRepository;
import com.dduk.repository.hr.PayrollContractRepository;
import com.dduk.repository.inventory.VendorRepository;
import com.dduk.service.accounting.payroll.PayrollManagementService;
import com.dduk.service.accounting.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;

@Component
@Profile("!test")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class OperationalDataInitializer {

    private final EmployeeRepository employeeRepository;
    private final PayrollContractRepository payrollContractRepository;
    private final VendorRepository vendorRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final AccountRepository accountRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherService voucherService;
    private final JournalEntryRepository journalEntryRepository;
    private final PayrollLedgerRepository payrollLedgerRepository;
    private final PayrollManagementService payrollManagementService;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("ERP 초기 운영 데이터 적재를 시작합니다...");
        try {
            // 1. 거래처 데이터 적재
            List<Vendor> vendors = seedVendors();

            // 2. 사원 및 급여 계약 데이터 적재
            List<Employee> employees = seedEmployees();

            // 3. 2026년 마감 기간 초기 OPEN 상태로 생성
            List<AccountingPeriod> periods = seedAccountingPeriods();

            // 4. 전표 데이터 적재
            seedVouchers(vendors);

            // 5. 급여대장 데이터 적재
            seedPayrollLedgers(employees);

            // 6. 월마감 자동 실행(closePastPeriods) 제거

            log.info("ERP 초기 운영 데이터 적재가 성공적으로 완료되었습니다.");
        } catch (Exception e) {
            log.error("ERP 초기 운영 데이터 적재 중 오류 발생", e);
            throw e;
        }
    }

    @Transactional
    public List<Vendor> seedVendors() {
        List<Vendor> saved = new ArrayList<>();

        if (!vendorRepository.findByVendorCode("V001").isPresent()) {
            Vendor v = vendorRepository.save(Vendor.builder()
                    .vendorCode("V001")
                    .businessRegistrationNo("123-45-67890")
                    .name("(주)뚝딱물산")
                    .representativeName("김뚝딱")
                    .businessType("도소매")
                    .businessItem("사무용품")
                    .contactName("정대리")
                    .contactPhone("010-1111-2222")
                    .email("tt@dduk.com")
                    .address("서울시 강남구 테헤란로 123")
                    .bankName("국민은행")
                    .bankAccountNo("110-123-456789")
                    .bankAccountHolder("김뚝딱")
                    .status("ACTIVE")
                    .build());
            saved.add(v);
        }

        if (!vendorRepository.findByVendorCode("V002").isPresent()) {
            Vendor v = vendorRepository.save(Vendor.builder()
                    .vendorCode("V002")
                    .businessRegistrationNo("123-45-67891")
                    .name("(주)그린테크")
                    .representativeName("이그린")
                    .businessType("제조")
                    .businessItem("컴퓨터기기")
                    .contactName("김과장")
                    .contactPhone("010-1111-2223")
                    .email("gt@green.com")
                    .address("서울시 서초구 반포대로 456")
                    .bankName("신한은행")
                    .bankAccountNo("110-123-456790")
                    .bankAccountHolder("이그린")
                    .status("ACTIVE")
                    .build());
            saved.add(v);
        }

        if (!vendorRepository.findByVendorCode("V003").isPresent()) {
            Vendor v = vendorRepository.save(Vendor.builder()
                    .vendorCode("V003")
                    .businessRegistrationNo("123-45-67892")
                    .name("(주)글로벌네트웍스")
                    .representativeName("박네트")
                    .businessType("서비스")
                    .businessItem("물류대행")
                    .contactName("최대리")
                    .contactPhone("010-1111-2224")
                    .email("gn@global.com")
                    .address("부산시 중구 중앙대로 789")
                    .bankName("우리은행")
                    .bankAccountNo("110-123-456791")
                    .bankAccountHolder("박네트")
                    .status("ACTIVE")
                    .build());
            saved.add(v);
        }

        if (!vendorRepository.findByVendorCode("V004").isPresent()) {
            Vendor v = vendorRepository.save(Vendor.builder()
                    .vendorCode("V004")
                    .businessRegistrationNo("123-45-67893")
                    .name("뚝딱세무법인")
                    .representativeName("박세무")
                    .businessType("전문직")
                    .businessItem("세무대리")
                    .contactName("박세무")
                    .contactPhone("02-123-4567")
                    .email("tax@dduk.com")
                    .address("서울시 영등포구 여의도대로 12")
                    .bankName("하나은행")
                    .bankAccountNo("110-123-456792")
                    .bankAccountHolder("박세무")
                    .status("ACTIVE")
                    .build());
            saved.add(v);
        }

        if (!saved.isEmpty()) {
            log.info("[SEED] 거래처 데이터 {}건 적재 완료.", saved.size());
        } else {
            log.info("[SEED] 거래처 데이터 적재 스킵 - 이미 데이터가 존재합니다.");
        }
        return vendorRepository.findAll();
    }

    @Transactional
    public List<Employee> seedEmployees() {
        List<Employee> saved = new ArrayList<>();

        if (!employeeRepository.findByEmployeeNo("E0001").isPresent()) {
            Employee e = employeeRepository.save(Employee.builder()
                    .employeeNo("E0001")
                    .name("홍길동")
                    .department("개발부")
                    .position("사원")
                    .employmentStatus("ACTIVE")
                    .hireDate(LocalDate.of(2026, 1, 1))
                    .email("hong@dduk.com")
                    .phone("010-1234-5678")
                    .build());
            saved.add(e);
        }

        if (!employeeRepository.findByEmployeeNo("E0002").isPresent()) {
            Employee e = employeeRepository.save(Employee.builder()
                    .employeeNo("E0002")
                    .name("김철수")
                    .department("인사부")
                    .position("대리")
                    .employmentStatus("ACTIVE")
                    .hireDate(LocalDate.of(2026, 1, 1))
                    .email("kim@dduk.com")
                    .phone("010-1234-5679")
                    .build());
            saved.add(e);
        }

        if (!employeeRepository.findByEmployeeNo("E0003").isPresent()) {
            Employee e = employeeRepository.save(Employee.builder()
                    .employeeNo("E0003")
                    .name("이영희")
                    .department("회계부")
                    .position("과장")
                    .employmentStatus("ACTIVE")
                    .hireDate(LocalDate.of(2026, 1, 1))
                    .email("lee@dduk.com")
                    .phone("010-1234-5680")
                    .build());
            saved.add(e);
        }

        if (!employeeRepository.findByEmployeeNo("E0004").isPresent()) {
            Employee e = employeeRepository.save(Employee.builder()
                    .employeeNo("E0004")
                    .name("박민수")
                    .department("영업부")
                    .position("차장")
                    .employmentStatus("ACTIVE")
                    .hireDate(LocalDate.of(2026, 1, 1))
                    .email("park@dduk.com")
                    .phone("010-1234-5681")
                    .build());
            saved.add(e);
        }

        List<Employee> allEmployees = employeeRepository.findAll();
        int contractCount = 0;

        // 급여 계약 등록
        for (Employee emp : allEmployees) {
            String contractNo = "";
            BigDecimal baseSalary = BigDecimal.ZERO;
            BigDecimal hourlyRate = BigDecimal.ZERO;

            if ("E0001".equals(emp.getEmployeeNo())) {
                contractNo = "C20260101-01";
                baseSalary = new BigDecimal("3500000");
                hourlyRate = new BigDecimal("16000");
            } else if ("E0002".equals(emp.getEmployeeNo())) {
                contractNo = "C20260101-02";
                baseSalary = new BigDecimal("4500000");
                hourlyRate = new BigDecimal("20000");
            } else if ("E0003".equals(emp.getEmployeeNo())) {
                contractNo = "C20260101-03";
                baseSalary = new BigDecimal("5500000");
                hourlyRate = new BigDecimal("25000");
            } else if ("E0004".equals(emp.getEmployeeNo())) {
                contractNo = "C20260101-04";
                baseSalary = new BigDecimal("6500000");
                hourlyRate = new BigDecimal("30000");
            }

            if (!contractNo.isEmpty() && !payrollContractRepository.findByContractNo(contractNo).isPresent()) {
                payrollContractRepository.save(PayrollContract.builder()
                        .employee(emp)
                        .contractNo(contractNo)
                        .baseSalary(baseSalary)
                        .hourlyRate(hourlyRate)
                        .contractDate(LocalDate.of(2026, 1, 1))
                        .status("ACTIVE")
                        .build());
                contractCount++;
            }
        }

        if (!saved.isEmpty() || contractCount > 0) {
            log.info("[SEED] 사원 및 급여 계약 데이터 적재 완료. (신규 사원: {}건, 신규 계약: {}건)", saved.size(), contractCount);
        } else {
            log.info("[SEED] 사원 및 급여 계약 데이터 적재 스킵 - 이미 데이터가 존재합니다.");
        }

        return allEmployees;
    }

    @Transactional
    public List<AccountingPeriod> seedAccountingPeriods() {
        List<AccountingPeriod> periods = new ArrayList<>();
        int createdCount = 0;

        for (int month = 1; month <= 12; month++) {
            Optional<AccountingPeriod> existing = accountingPeriodRepository.findByFiscalYearAndFiscalMonth(2026, month);
            if (existing.isPresent()) {
                periods.add(existing.get());
            } else {
                LocalDate firstDay = LocalDate.of(2026, month, 1);
                LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
                AccountingPeriod period = accountingPeriodRepository.save(AccountingPeriod.builder()
                        .fiscalYear(2026)
                        .fiscalMonth(month)
                        .startDate(firstDay)
                        .endDate(lastDay)
                        .status(AccountingPeriodStatus.OPEN)
                        .build());
                periods.add(period);
                createdCount++;
            }
        }

        if (createdCount > 0) {
            log.info("[SEED] 2026년 회계 마감 기간 {}개월 OPEN 상태로 생성 완료.", createdCount);
        } else {
            log.info("[SEED] 회계 마감 기간 생성 스킵 - 이미 데이터가 존재합니다.");
        }
        return periods;
    }

    @Transactional
    public void seedVouchers(List<Vendor> vendors) {
        // 사용될 계정과목 사전 조회
        Account cashDep = findAccount("1112"); // 보통예금
        Account ar = findAccount("1121");      // 외상매출금
        Account vatRec = findAccount("1146");  // 부가세대급금
        Account ap = findAccount("2111");      // 외상매입금
        Account payables = findAccount("2121"); // 미지급금
        Account vatPay = findAccount("2128");  // 부가세예수금
        Account sales = findAccount("4110");   // 상품매출
        Account welfare = findAccount("5250"); // 복리후생비
        Account officeSup = findAccount("5380"); // 소모품비

        int createdCount = 0;
        int skippedClosedCount = 0;
        int skippedExistCount = 0;

        // 1월 ~ 4월 전표 (POSTED 상태로 승인 기표 처리할 전표들)
        for (int month = 1; month <= 4; month++) {
            String periodKey = "2026-" + String.format("%02d", month);
            
            // target period CLOSED 여부 검사
            boolean isPeriodClosed = accountingPeriodRepository.findByFiscalYearAndFiscalMonth(2026, month)
                    .map(AccountingPeriod::isClosed)
                    .orElse(false);
            if (isPeriodClosed) {
                log.info("[SEED] Period already closed: {}. Skipping voucher seed for this month.", periodKey);
                skippedClosedCount++;
                continue;
            }

            LocalDate date = LocalDate.of(2026, month, 15);
            Vendor vendor1 = vendors.get(0);
            Vendor vendor2 = vendors.get(1);

            // 매출 전표
            String salesDesc = month + "월 정기 상품 매출 분";
            if (isVoucherAlreadySeeded(date, salesDesc)) {
                log.info("[SEED] Voucher seeding skipped - existing data: {} - {}", date, salesDesc);
                skippedExistCount++;
            } else {
                BigDecimal supplySales = new BigDecimal("10000000").multiply(BigDecimal.valueOf(month));
                BigDecimal vatSales = supplySales.multiply(new BigDecimal("0.10"));
                BigDecimal totalSales = supplySales.add(vatSales);

                VoucherRequest request1 = new VoucherRequest();
                request1.setVoucherDate(date);
                request1.setVoucherType(VoucherType.SALES);
                request1.setVatType(VatType.TAX_INVOICE);
                request1.setVendorId(vendor1.getId());
                request1.setVendorNameSnapshot(vendor1.getName());
                request1.setDescription(salesDesc);
                
                List<VoucherLineRequest> lines1 = new ArrayList<>();
                lines1.add(line(cashDep, AccountSide.DEBIT, BigDecimal.ZERO, BigDecimal.ZERO, totalSales, "정기 매출 대금 보통예금 입금"));
                lines1.add(line(sales, AccountSide.CREDIT, supplySales, BigDecimal.ZERO, supplySales, "상품 매출액"));
                lines1.add(line(vatPay, AccountSide.CREDIT, BigDecimal.ZERO, vatSales, vatSales, "부가세예수금 수취"));
                request1.setLines(lines1);
                
                VoucherResponse res1 = voucherService.createVoucher(request1);
                postVoucher(res1.getId());
                createdCount++;
            }

            // 매입 전표
            String purchaseDesc = month + "월 컴퓨터 비품/소모품 구매";
            if (isVoucherAlreadySeeded(date, purchaseDesc)) {
                log.info("[SEED] Voucher seeding skipped - existing data: {} - {}", date, purchaseDesc);
                skippedExistCount++;
            } else {
                BigDecimal supplyPurchase = new BigDecimal("4000000").multiply(BigDecimal.valueOf(month));
                BigDecimal vatPurchase = supplyPurchase.multiply(new BigDecimal("0.10"));
                BigDecimal totalPurchase = supplyPurchase.add(vatPurchase);

                VoucherRequest request2 = new VoucherRequest();
                request2.setVoucherDate(date);
                request2.setVoucherType(VoucherType.PURCHASE);
                request2.setVatType(VatType.TAX_INVOICE);
                request2.setVendorId(vendor2.getId());
                request2.setVendorNameSnapshot(vendor2.getName());
                request2.setDescription(purchaseDesc);

                List<VoucherLineRequest> lines2 = new ArrayList<>();
                lines2.add(line(officeSup, AccountSide.DEBIT, supplyPurchase, BigDecimal.ZERO, supplyPurchase, "사무 소모품비"));
                lines2.add(line(vatRec, AccountSide.DEBIT, BigDecimal.ZERO, vatPurchase, vatPurchase, "부가세대급금 매입세액"));
                lines2.add(line(cashDep, AccountSide.CREDIT, BigDecimal.ZERO, BigDecimal.ZERO, totalPurchase, "소모품 보통예금 출금"));
                request2.setLines(lines2);

                VoucherResponse res2 = voucherService.createVoucher(request2);
                postVoucher(res2.getId());
                createdCount++;
            }

            // [추가시드] 매월 5일: 소모성 물품 매입
            LocalDate date5 = LocalDate.of(2026, month, 5);
            String desc5 = month + "월 소모성 비품 정기 매입";
            if (!isVoucherAlreadySeeded(date5, desc5)) {
                BigDecimal supply = new BigDecimal("1000000");
                BigDecimal vat = supply.multiply(new BigDecimal("0.10"));
                BigDecimal total = supply.add(vat);

                VoucherRequest req = new VoucherRequest();
                req.setVoucherDate(date5);
                req.setVoucherType(VoucherType.PURCHASE);
                req.setVatType(VatType.TAX_INVOICE);
                req.setVendorId(vendor2.getId());
                req.setVendorNameSnapshot(vendor2.getName());
                req.setDescription(desc5);

                List<VoucherLineRequest> lines = new ArrayList<>();
                lines.add(line(officeSup, AccountSide.DEBIT, supply, BigDecimal.ZERO, supply, "소모성 비품 매입"));
                lines.add(line(vatRec, AccountSide.DEBIT, BigDecimal.ZERO, vat, vat, "매입 부가세대급금"));
                lines.add(line(cashDep, AccountSide.CREDIT, BigDecimal.ZERO, BigDecimal.ZERO, total, "보통예금 지출"));
                req.setLines(lines);

                VoucherResponse res = voucherService.createVoucher(req);
                postVoucher(res.getId());
                createdCount++;
            }

            // [추가시드] 매월 10일: 임직원 식대 지급
            LocalDate date10 = LocalDate.of(2026, month, 10);
            String desc10 = month + "월 임직원 복리후생 식대 지급";
            if (!isVoucherAlreadySeeded(date10, desc10)) {
                BigDecimal supply = new BigDecimal("2000000");
                BigDecimal vat = supply.multiply(new BigDecimal("0.10"));
                BigDecimal total = supply.add(vat);

                VoucherRequest req = new VoucherRequest();
                req.setVoucherDate(date10);
                req.setVoucherType(VoucherType.PURCHASE);
                req.setVatType(VatType.TAX_INVOICE);
                req.setVendorId(vendor1.getId());
                req.setVendorNameSnapshot(vendor1.getName());
                req.setDescription(desc10);

                List<VoucherLineRequest> lines = new ArrayList<>();
                lines.add(line(welfare, AccountSide.DEBIT, supply, BigDecimal.ZERO, supply, "복리후생 식대비"));
                lines.add(line(vatRec, AccountSide.DEBIT, BigDecimal.ZERO, vat, vat, "매입 부가세대급금"));
                lines.add(line(cashDep, AccountSide.CREDIT, BigDecimal.ZERO, BigDecimal.ZERO, total, "보통예금 지출"));
                req.setLines(lines);

                VoucherResponse res = voucherService.createVoucher(req);
                postVoucher(res.getId());
                createdCount++;
            }

            // [추가시드] 매월 20일: 거래처 정기 납품 매출
            LocalDate date20 = LocalDate.of(2026, month, 20);
            String desc20 = month + "월 거래처 정기 납품 매출";
            if (!isVoucherAlreadySeeded(date20, desc20)) {
                BigDecimal supply = new BigDecimal("5000000");
                BigDecimal vat = supply.multiply(new BigDecimal("0.10"));
                BigDecimal total = supply.add(vat);

                VoucherRequest req = new VoucherRequest();
                req.setVoucherDate(date20);
                req.setVoucherType(VoucherType.SALES);
                req.setVatType(VatType.TAX_INVOICE);
                req.setVendorId(vendor1.getId());
                req.setVendorNameSnapshot(vendor1.getName());
                req.setDescription(desc20);

                List<VoucherLineRequest> lines = new ArrayList<>();
                lines.add(line(cashDep, AccountSide.DEBIT, BigDecimal.ZERO, BigDecimal.ZERO, total, "매출 대금 예입"));
                lines.add(line(sales, AccountSide.CREDIT, supply, BigDecimal.ZERO, supply, "정기 상품 매출"));
                lines.add(line(vatPay, AccountSide.CREDIT, BigDecimal.ZERO, vat, vat, "매출 부가세예수금"));
                req.setLines(lines);

                VoucherResponse res = voucherService.createVoucher(req);
                postVoucher(res.getId());
                createdCount++;
            }

            // [추가시드] 매월 25일: 소모품 추가 구매
            LocalDate date25 = LocalDate.of(2026, month, 25);
            String desc25 = month + "월 소모품비 추가 발생분";
            if (!isVoucherAlreadySeeded(date25, desc25)) {
                BigDecimal supply = new BigDecimal("300000");
                BigDecimal vat = supply.multiply(new BigDecimal("0.10"));
                BigDecimal total = supply.add(vat);

                VoucherRequest req = new VoucherRequest();
                req.setVoucherDate(date25);
                req.setVoucherType(VoucherType.PURCHASE);
                req.setVatType(VatType.TAX_INVOICE);
                req.setVendorId(vendor2.getId());
                req.setVendorNameSnapshot(vendor2.getName());
                req.setDescription(desc25);

                List<VoucherLineRequest> lines = new ArrayList<>();
                lines.add(line(officeSup, AccountSide.DEBIT, supply, BigDecimal.ZERO, supply, "소모품비 추가 결제"));
                lines.add(line(vatRec, AccountSide.DEBIT, BigDecimal.ZERO, vat, vat, "매입 부가세대급금"));
                lines.add(line(cashDep, AccountSide.CREDIT, BigDecimal.ZERO, BigDecimal.ZERO, total, "보통예금 지출"));
                req.setLines(lines);

                VoucherResponse res = voucherService.createVoucher(req);
                postVoucher(res.getId());
                createdCount++;
            }
        }

        // 5월 전표 (현재 월 - DRAFT, REQUESTED, APPROVED 등의 상태를 믹스하여 생성)
        LocalDate mayDate = LocalDate.of(2026, 5, 20);
        boolean isMayClosed = accountingPeriodRepository.findByFiscalYearAndFiscalMonth(2026, 5)
                .map(AccountingPeriod::isClosed)
                .orElse(false);
        if (isMayClosed) {
            log.info("[SEED] Period already closed: 2026-05. Skipping voucher seed for this month.");
            skippedClosedCount++;
        } else {
            // 5월 매출 전표 1 (APPROVED 상태)
            String maySalesDesc = "5월 물류서비스 완료분 매출";
            if (isVoucherAlreadySeeded(mayDate, maySalesDesc)) {
                log.info("[SEED] Voucher seeding skipped - existing data: {} - {}", mayDate, maySalesDesc);
                skippedExistCount++;
            } else {
                VoucherRequest requestMaySales = new VoucherRequest();
                requestMaySales.setVoucherDate(mayDate);
                requestMaySales.setVoucherType(VoucherType.SALES);
                requestMaySales.setVatType(VatType.TAX_INVOICE);
                requestMaySales.setVendorId(vendors.get(2).getId());
                requestMaySales.setVendorNameSnapshot(vendors.get(2).getName());
                requestMaySales.setDescription(maySalesDesc);

                List<VoucherLineRequest> linesMaySales = new ArrayList<>();
                linesMaySales.add(line(ar, AccountSide.DEBIT, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("11000000"), "매출채권(외상매출금) 등록"));
                linesMaySales.add(line(sales, AccountSide.CREDIT, new BigDecimal("10000000"), BigDecimal.ZERO, new BigDecimal("10000000"), "상품 매출액"));
                linesMaySales.add(line(vatPay, AccountSide.CREDIT, BigDecimal.ZERO, new BigDecimal("1000000"), new BigDecimal("1000000"), "부가세예수금"));
                requestMaySales.setLines(linesMaySales);

                VoucherResponse resMaySales = voucherService.createVoucher(requestMaySales);
                approveVoucher(resMaySales.getId());
                createdCount++;
            }

            // 5월 매입 전표 1 (REQUESTED 상태)
            String mayPurchaseDesc = "5월 세무대행 전문직 서비스 매입";
            if (isVoucherAlreadySeeded(mayDate, mayPurchaseDesc)) {
                log.info("[SEED] Voucher seeding skipped - existing data: {} - {}", mayDate, mayPurchaseDesc);
                skippedExistCount++;
            } else {
                VoucherRequest requestMayPurchase = new VoucherRequest();
                requestMayPurchase.setVoucherDate(mayDate);
                requestMayPurchase.setVoucherType(VoucherType.PURCHASE);
                requestMayPurchase.setVatType(VatType.TAX_INVOICE);
                requestMayPurchase.setVendorId(vendors.get(3).getId());
                requestMayPurchase.setVendorNameSnapshot(vendors.get(3).getName());
                requestMayPurchase.setDescription(mayPurchaseDesc);

                List<VoucherLineRequest> linesMayPurchase = new ArrayList<>();
                linesMayPurchase.add(line(welfare, AccountSide.DEBIT, new BigDecimal("2000000"), BigDecimal.ZERO, new BigDecimal("2000000"), "회계교육비 및 복리비"));
                linesMayPurchase.add(line(vatRec, AccountSide.DEBIT, BigDecimal.ZERO, new BigDecimal("200000"), new BigDecimal("200000"), "부가세대급금"));
                linesMayPurchase.add(line(payables, AccountSide.CREDIT, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("2200000"), "미지급금 거래처 미결제"));
                requestMayPurchase.setLines(linesMayPurchase);

                VoucherResponse resMayPurchase = voucherService.createVoucher(requestMayPurchase);
                requestApprovalVoucher(resMayPurchase.getId());
                createdCount++;
            }

            // 5월 매입 전표 2 (DRAFT 상태)
            String mayDraftDesc = "5월 하순 소모성 필기구 다량 구매";
            if (isVoucherAlreadySeeded(mayDate, mayDraftDesc)) {
                log.info("[SEED] Voucher seeding skipped - existing data: {} - {}", mayDate, mayDraftDesc);
                skippedExistCount++;
            } else {
                VoucherRequest requestMayDraft = new VoucherRequest();
                requestMayDraft.setVoucherDate(mayDate);
                requestMayDraft.setVoucherType(VoucherType.PURCHASE);
                requestMayDraft.setVatType(VatType.TAX_INVOICE);
                requestMayDraft.setVendorId(vendors.get(0).getId());
                requestMayDraft.setVendorNameSnapshot(vendors.get(0).getName());
                requestMayDraft.setDescription(mayDraftDesc);

                List<VoucherLineRequest> linesMayDraft = new ArrayList<>();
                linesMayDraft.add(line(officeSup, AccountSide.DEBIT, new BigDecimal("500000"), BigDecimal.ZERO, new BigDecimal("500000"), "사무실 소모품비"));
                linesMayDraft.add(line(vatRec, AccountSide.DEBIT, BigDecimal.ZERO, new BigDecimal("50000"), new BigDecimal("50000"), "부가세대급금"));
                linesMayDraft.add(line(cashDep, AccountSide.CREDIT, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("550000"), "보통예금 즉시 현금 지출"));
                requestMayDraft.setLines(linesMayDraft);

                voucherService.createVoucher(requestMayDraft);
                createdCount++;
            }
        }

        if (createdCount > 0) {
            log.info("[SEED] 전표 데이터 적재 완료. (신규 전표: {}건, 마감 스킵: {}건, 중복 스킵: {}건)", createdCount, skippedClosedCount, skippedExistCount);
        } else {
            log.info("[SEED] 전표 데이터 적재 스킵 - 이미 데이터가 존재하거나 모든 대상 회계 기간이 마감되었습니다.");
        }
    }

    private boolean isVoucherAlreadySeeded(LocalDate date, String description) {
        return voucherRepository.findWithFilters(null, null, date, date, description)
                .stream()
                .anyMatch(v -> description.equals(v.getDescription()));
    }

    @Transactional
    public void seedPayrollLedgers(List<Employee> employees) {
        int createdCount = 0;
        int skippedClosedCount = 0;
        int skippedExistCount = 0;

        // 1월 ~ 4월 급여대장
        for (int month = 1; month <= 4; month++) {
            String periodKey = "2026-" + String.format("%02d", month);

            // target period CLOSED 여부 검사
            boolean isPeriodClosed = accountingPeriodRepository.findByFiscalYearAndFiscalMonth(2026, month)
                    .map(AccountingPeriod::isClosed)
                    .orElse(false);
            if (isPeriodClosed) {
                log.info("[SEED] Period already closed: {}. Skipping payroll seed for this month.", periodKey);
                skippedClosedCount++;
                continue;
            }

            if (payrollLedgerRepository.findFirstByPaymentYearMonthOrderByPaymentDateAscIdAsc(periodKey).isPresent()) {
                log.info("[SEED] PayrollLedger seeding skipped - existing data: {}", periodKey);
                skippedExistCount++;
                continue;
            }

            PayrollLedgerCreateRequest request = new PayrollLedgerCreateRequest();
            request.setAttributionYearMonth(periodKey);
            request.setPayrollType(PayrollType.SALARY);
            request.setTaxType(PayrollTaxType.TAXABLE);
            request.setSettlementCycle(PayrollSettlementCycle.MONTHLY);
            request.setTargetPeriodMode(PayrollTargetPeriodMode.BULK);
            request.setPaymentDate(LocalDate.of(2026, month, 25));
            request.setPaymentYearMonth(periodKey);
            request.setLedgerName("2026년 " + month + "월 정기 급여대장");
            request.setSettlementItemSelectionMode(PayrollSelectionMode.ALL);
            request.setEmployeeSelectionMode(PayrollSelectionMode.ALL);
            request.setCreatedBy("admin");

            // 생성 및 계산 자동 실행
            PayrollLedgerResponse response = payrollManagementService.createLedger(request, true);
            
            // CONFIRMED 확정 처리
            payrollManagementService.confirmLedger(response.getId());

            // 급여 전표 강제 POSTED 로 변경하여 Ledger에 반영되게 함
            PayrollLedger ledger = payrollLedgerRepository.findById(response.getId()).orElse(null);
            if (ledger != null && ledger.getJournalEntry() != null) {
                JournalEntry je = ledger.getJournalEntry();
                // 강제 상태 변경 및 반영
                je.post();
                journalEntryRepository.save(je);
                
                // 급여대장도 POSTED 로 승격
                ledger.setStatus(PayrollStatus.POSTED);
                payrollLedgerRepository.save(ledger);
            }
            createdCount++;
        }

        // 5월 급여대장 (현재 월 - CALCULATED 상태로 두어 사용자가 확인하도록 함)
        String mayPeriodKey = "2026-05";
        boolean isMayClosed = accountingPeriodRepository.findByFiscalYearAndFiscalMonth(2026, 5)
                .map(AccountingPeriod::isClosed)
                .orElse(false);
        if (isMayClosed) {
            log.info("[SEED] Period already closed: 2026-05. Skipping payroll seed for this month.");
            skippedClosedCount++;
        } else {
            if (payrollLedgerRepository.findFirstByPaymentYearMonthOrderByPaymentDateAscIdAsc(mayPeriodKey).isPresent()) {
                log.info("[SEED] PayrollLedger seeding skipped - existing data: 2026-05");
                skippedExistCount++;
            } else {
                PayrollLedgerCreateRequest requestMay = new PayrollLedgerCreateRequest();
                requestMay.setAttributionYearMonth(mayPeriodKey);
                requestMay.setPayrollType(PayrollType.SALARY);
                requestMay.setTaxType(PayrollTaxType.TAXABLE);
                requestMay.setSettlementCycle(PayrollSettlementCycle.MONTHLY);
                requestMay.setTargetPeriodMode(PayrollTargetPeriodMode.BULK);
                requestMay.setPaymentDate(LocalDate.of(2026, 5, 25));
                requestMay.setPaymentYearMonth(mayPeriodKey);
                requestMay.setLedgerName("2026년 5월 정기 급여대장");
                requestMay.setSettlementItemSelectionMode(PayrollSelectionMode.ALL);
                requestMay.setEmployeeSelectionMode(PayrollSelectionMode.ALL);
                requestMay.setCreatedBy("admin");

                payrollManagementService.createLedger(requestMay, true);
                createdCount++;
            }
        }

        if (createdCount > 0) {
            log.info("[SEED] 급여대장 데이터 적재 완료. (신규 급여대장: {}건, 마감 스킵: {}건, 중복 스킵: {}건)", createdCount, skippedClosedCount, skippedExistCount);
        } else {
            log.info("[SEED] 급여대장 데이터 적재 스킵 - 이미 데이터가 존재하거나 모든 대상 회계 기간이 마감되었습니다.");
        }
    }

    private Account findAccount(String code) {
        return accountRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("필수 초기 계정과목 누락: " + code));
    }

    private VoucherLineRequest line(Account account, AccountSide side, BigDecimal supply, BigDecimal vat, BigDecimal total, String desc) {
        VoucherLineRequest req = new VoucherLineRequest();
        req.setAccountId(account.getId());
        req.setAccountCode(account.getCode());
        req.setAccountName(account.getName());
        req.setDebitCredit(side);
        req.setSupplyAmount(supply);
        req.setVatAmount(vat);
        req.setTotalAmount(total);
        req.setDescription(desc);
        return req;
    }

    private void requestApprovalVoucher(Long id) {
        voucherService.updateStatus(id, VoucherStatus.REQUESTED);
    }

    private void approveVoucher(Long id) {
        voucherService.updateStatus(id, VoucherStatus.REQUESTED);
        voucherService.updateStatus(id, VoucherStatus.APPROVED);
    }

    private void postVoucher(Long id) {
        voucherService.updateStatus(id, VoucherStatus.REQUESTED);
        voucherService.updateStatus(id, VoucherStatus.APPROVED);
        voucherService.updateStatus(id, VoucherStatus.POSTED);
    }
}
