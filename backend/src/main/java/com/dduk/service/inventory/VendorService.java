package com.dduk.service.inventory;

import com.dduk.dto.inventory.VendorCreateDto;
import com.dduk.dto.inventory.VendorResponseDto;
import com.dduk.dto.inventory.VendorStatusUpdateDto;
import com.dduk.dto.inventory.VendorUpdateDto;
import com.dduk.entity.inventory.Vendor;
import com.dduk.repository.inventory.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** 
    com.dduk.service.inventory
   ├─ VendorService              // 거래처 등록/조회/수정/비활성화
   ├─ PurchaseService            // 구매 요청/승인
   └─ PurchaseOrderService       // 발주 생성/상태 변경 

    InboundService: 입고 등록, 입고 목록 조회, 입고 취소
    OutboundService: 출고 등록, 출고 목록 조회, 출고 취소
    PurchaseService: 구매 요청, 구매 승인, 구매 내역
    PurchaseOrderService: 발주 생성, 발주 상태 변경, 발주서 조회
    InventoryService: 현재 재고 조회, 재고 반영, 안전재고 확인
    VendorService: 거래처 등록, 거래처 조회, 거래처 수정, 거래처 비활성화

    기본 정보

    거래처명: 업체 이름
    사업자등록번호: 거래처 식별용 번호
    대표자명: 대표자 이름
    업태: 제조업, 도소매업 등
    종목: 식자재, 포장재, 물류 등
    사용 여부: 현재 거래 가능한 업체인지
    
    연락처 정보

    담당자명
    전화번호
    이메일
    주소
    정산/거래 정보

    은행명
    계좌번호
    예금주
    결제 조건
    예: 당일 결제, 월말 마감 후 익월 10일 지급
    관리 정보

    비고
    생성일
    수정일
    구매/발주와 연결해서 보면 거래처는 이렇게 쓰입니다.

    품목(Item)
    → 기본 거래처(defaultVendor) 참조 가능

    발주(PurchaseOrder)
    → 실제 발주할 거래처(vendor) 참조

    
    거래처(Vendor)
    → 발주 가능 업체 정보 관리
    예를 들어 “밀가루”라는 품목의 기본 거래처가 “OO식자재”라면, 
    발주서를 만들 때 그 거래처를 기본값으로 가져올 수 있습니다. 
    하지만 발주 시점에 다른 거래처를 선택할 수도 있습니다.


    거래처 관리 API에서 나중에 만들 기능:

    거래처 등록
    거래처 목록 조회
    거래처 상세 조회
    거래처 정보 수정
    거래처 활성/비활성 변경
    거래처별 발주 내역 조회
    
    중요한 점은 거래처를 삭제하기보다는 비활성화 처리하는 경우가 많다는 것입니다. 
    이미 발주 이력이 있는 거래처를 삭제하면 과거 데이터가 깨질 수 있어서, 
    active 같은 상태값으로 관리하는 게 안전합니다.
*/
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final VendorRepository vendorRepository;

    public List<VendorResponseDto> getVendors() {
        return vendorRepository.findAllByOrderByIdDesc().stream()
                .map(VendorResponseDto::from)
                .toList();
    }

    public List<VendorResponseDto> searchVendors(
            String name,
            String representativeName,
            String contactPhone,
            String businessRegistrationNo
    ) {
        return vendorRepository.searchVendors(
                        trimToNull(name),
                        trimToNull(representativeName),
                        trimToNull(contactPhone),
                        trimToNull(businessRegistrationNo)
                ).stream()
                .map(VendorResponseDto::from)
                .toList();
    }

    public VendorResponseDto getVendor(Long vendorId) {
        return VendorResponseDto.from(findVendor(vendorId));
    }

    @Transactional
    public VendorResponseDto createVendor(VendorCreateDto requestDto) {
        validateCreateRequest(requestDto);

        if (vendorRepository.findByBusinessRegistrationNo(requestDto.getBusinessRegistrationNo().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 사업자등록번호입니다.");
        }

        String vendorCode = generateVendorCode();

        Vendor vendor = Vendor.builder()
                .vendorCode(vendorCode)
                .businessRegistrationNo(requestDto.getBusinessRegistrationNo().trim())
                .name(requestDto.getName().trim())
                .representativeName(requestDto.getRepresentativeName().trim())
                .businessType(trimToNull(requestDto.getBusinessType()))
                .businessItem(trimToNull(requestDto.getBusinessItem()))
                .contactName(trimToNull(requestDto.getContactName()))
                .contactPhone(trimToNull(requestDto.getContactPhone()))
                .email(trimToNull(requestDto.getEmail()))
                .address(trimToNull(requestDto.getAddress()))
                .bankName(trimToNull(requestDto.getBankName()))
                .bankAccountNo(trimToNull(requestDto.getBankAccountNo()))
                .bankAccountHolder(trimToNull(requestDto.getBankAccountHolder()))
                .bankbookCopyFilePath(trimToNull(requestDto.getBankbookCopyFilePath()))
                .status(STATUS_ACTIVE)
                .memo(trimToNull(requestDto.getMemo()))
                .build();

        return VendorResponseDto.from(vendorRepository.save(vendor));
    }

    @Transactional
    public VendorResponseDto updateVendor(Long vendorId, VendorUpdateDto requestDto) {
        Vendor vendor = findVendor(vendorId);
        validateUpdateRequest(requestDto);

        String businessRegistrationNo = requestDto.getBusinessRegistrationNo().trim();
        vendorRepository.findByBusinessRegistrationNo(businessRegistrationNo)
                .filter(foundVendor -> !foundVendor.getId().equals(vendorId))
                .ifPresent(foundVendor -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 사업자등록번호입니다.");
                });

        vendor.update(
                businessRegistrationNo,
                requestDto.getName().trim(),
                requestDto.getRepresentativeName().trim(),
                trimToNull(requestDto.getBusinessType()),
                trimToNull(requestDto.getBusinessItem()),
                trimToNull(requestDto.getContactName()),
                trimToNull(requestDto.getContactPhone()),
                trimToNull(requestDto.getEmail()),
                trimToNull(requestDto.getAddress()),
                trimToNull(requestDto.getBankName()),
                trimToNull(requestDto.getBankAccountNo()),
                trimToNull(requestDto.getBankAccountHolder()),
                trimToNull(requestDto.getBankbookCopyFilePath()),
                trimToNull(requestDto.getMemo())
        );

        return VendorResponseDto.from(vendor);
    }

    @Transactional
    public VendorResponseDto updateVendorStatus(Long vendorId, VendorStatusUpdateDto requestDto) {
        Vendor vendor = findVendor(vendorId);
        String status = validateStatusRequest(requestDto);

        vendor.updateStatus(status);

        return VendorResponseDto.from(vendor);
    }

    private Vendor findVendor(Long vendorId) {
        if (vendorId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거래처 ID가 필요합니다.");
        }
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "거래처를 찾을 수 없습니다."));
    }

    private void validateCreateRequest(VendorCreateDto requestDto) {
        if (requestDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거래처 정보가 필요합니다.");
        }
        if (isBlank(requestDto.getBusinessRegistrationNo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사업자등록번호는 필수입니다.");
        }
        if (isBlank(requestDto.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거래처명은 필수입니다.");
        }
        if (isBlank(requestDto.getRepresentativeName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대표자명은 필수입니다.");
        }
    }

    private void validateUpdateRequest(VendorUpdateDto requestDto) {
        if (requestDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거래처 정보가 필요합니다.");
        }
        if (isBlank(requestDto.getBusinessRegistrationNo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사업자등록번호는 필수입니다.");
        }
        if (isBlank(requestDto.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거래처명은 필수입니다.");
        }
        if (isBlank(requestDto.getRepresentativeName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대표자명은 필수입니다.");
        }
    }

    private String validateStatusRequest(VendorStatusUpdateDto requestDto) {
        if (requestDto == null || isBlank(requestDto.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상태값이 필요합니다.");
        }

        String status = requestDto.getStatus().trim().toUpperCase();
        if (!STATUS_ACTIVE.equals(status) && !STATUS_INACTIVE.equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거래처 상태는 ACTIVE 또는 INACTIVE만 가능합니다.");
        }

        return status;
    }

    private String generateVendorCode() {
        long nextNumber = vendorRepository.findTopByOrderByIdDesc()
                .map(vendor -> vendor.getId() + 1)
                .orElse(1L);

        String vendorCode = formatVendorCode(nextNumber);
        while (vendorRepository.findByVendorCode(vendorCode).isPresent()) {
            nextNumber++;
            vendorCode = formatVendorCode(nextNumber);
        }

        return vendorCode;
    }

    private String formatVendorCode(long number) {
        return String.format("VND-%03d", number);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }
}
