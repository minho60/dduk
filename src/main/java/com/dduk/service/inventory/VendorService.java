package com.dduk.service.inventory;

import com.dduk.dto.inventory.VendorCreateDto;
import com.dduk.dto.inventory.VendorResponseDto;
import com.dduk.dto.inventory.VendorStatusUpdateDto;
import com.dduk.dto.inventory.VendorUpdateDto;
import com.dduk.entity.inventory.Vendor;
import com.dduk.repository.inventory.VendorRepository;
import lombok.RequiredArgsConstructor;
import com.dduk.config.PrincipalDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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

        Long currentUserId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PrincipalDetails) {
            currentUserId = ((PrincipalDetails) authentication.getPrincipal()).getMember().getId();
        }

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
                .status(normalizeStatus(requestDto.getStatus()))
                .memo(trimToNull(requestDto.getMemo()))
                .registeredById(currentUserId)
                .build();

        return VendorResponseDto.from(vendorRepository.save(vendor));
    }

    @Transactional
    public VendorResponseDto updateVendor(Long vendorId, VendorUpdateDto requestDto) {
        Vendor vendor = findVendor(vendorId);
        validateUpdateRequest(requestDto);

        vendor.update(
                vendor.getBusinessRegistrationNo(),
                vendor.getName(),
                vendor.getRepresentativeName(),
                vendor.getBusinessType(),
                vendor.getBusinessItem(),
                requestDto.getContactName().trim(),
                requestDto.getContactPhone().trim(),
                requestDto.getEmail().trim(),
                requestDto.getAddress().trim(),
                requestDto.getBankName().trim(),
                requestDto.getBankAccountNo().trim(),
                requestDto.getBankAccountHolder().trim(),
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
        if (isBlank(requestDto.getBusinessType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업태는 필수입니다.");
        }
        if (isBlank(requestDto.getBusinessItem())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종목은 필수입니다.");
        }
        validateRequiredEditableFields(
                requestDto.getContactName(),
                requestDto.getContactPhone(),
                requestDto.getEmail(),
                requestDto.getAddress(),
                requestDto.getBankName(),
                requestDto.getBankAccountNo(),
                requestDto.getBankAccountHolder()
        );
    }

    private void validateUpdateRequest(VendorUpdateDto requestDto) {
        if (requestDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거래처 정보가 필요합니다.");
        }
        validateRequiredEditableFields(
                requestDto.getContactName(),
                requestDto.getContactPhone(),
                requestDto.getEmail(),
                requestDto.getAddress(),
                requestDto.getBankName(),
                requestDto.getBankAccountNo(),
                requestDto.getBankAccountHolder()
        );
    }

    private void validateRequiredEditableFields(
            String contactName,
            String contactPhone,
            String email,
            String address,
            String bankName,
            String bankAccountNo,
            String bankAccountHolder
    ) {
        if (isBlank(contactName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "담당자명은 필수입니다.");
        }
        if (isBlank(contactPhone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "전화번호는 필수입니다.");
        }
        if (isBlank(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일은 필수입니다.");
        }
        if (isBlank(address)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주소는 필수입니다.");
        }
        if (isBlank(bankName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "은행명은 필수입니다.");
        }
        if (isBlank(bankAccountNo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "계좌번호는 필수입니다.");
        }
        if (isBlank(bankAccountHolder)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예금주는 필수입니다.");
        }
    }

    private String validateStatusRequest(VendorStatusUpdateDto requestDto) {
        if (requestDto == null || isBlank(requestDto.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상태값이 필요합니다.");
        }

        return normalizeStatus(requestDto.getStatus());
    }

    private String normalizeStatus(String value) {
        if (isBlank(value)) {
            return STATUS_ACTIVE;
        }

        String status = value.trim().toUpperCase();
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
