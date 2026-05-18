package com.dduk.service.inventory;

import com.dduk.dto.inventory.ItemCreateDto;
import com.dduk.dto.inventory.ItemResponseDto;
import com.dduk.entity.inventory.Item;
import com.dduk.entity.inventory.ItemType;
import com.dduk.entity.inventory.Vendor;
import com.dduk.repository.inventory.ItemRepository;
import com.dduk.repository.inventory.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private static final DateTimeFormatter ITEM_CODE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ItemRepository itemRepository;
    private final VendorRepository vendorRepository;

    public List<ItemResponseDto> searchItems(String name) {
        String keyword = normalizeName(name);
        if (keyword.isEmpty()) {
            return itemRepository.findAllByOrderByIdDesc().stream()
                    .limit(10)
                    .map(ItemResponseDto::from)
                    .toList();
        }

        return itemRepository.findTop10ByNameContainingIgnoreCaseOrderByIdAsc(keyword).stream()
                .map(ItemResponseDto::from)
                .toList();
    }

    @Transactional
    public ItemResponseDto createItem(ItemCreateDto requestDto, Long registeredById) {
        if (requestDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "품목 정보가 필요합니다.");
        }

        String name = normalizeName(requestDto.getName());
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "품목명을 입력해야 합니다.");
        }

        String category = requireText(requestDto.getCategory(), "카테고리를 입력해야 합니다.");
        String spec = requireText(requestDto.getSpec(), "규격을 입력해야 합니다.");
        String unit = requireText(requestDto.getUnit(), "단위를 입력해야 합니다.");
        ItemType itemType = parseItemType(requestDto.getItemType());
        String barcode = normalizeToNull(requestDto.getBarcode());
        BigDecimal standardCost = requireNonNegativeAmount(requestDto.getStandardCost(), "표준원가는 0 이상이어야 합니다.");
        BigDecimal unitPrice = requireUnitPrice(requestDto.getUnitPrice());
        int safetyStock = requireNonNegativeInteger(requestDto.getSafetyStock(), "안전재고는 0 이상이어야 합니다.");
        boolean active = requestDto.getActive() == null || requestDto.getActive();

        Vendor defaultVendor = findDefaultVendor(requestDto.getVendorId());

        List<Item> existingItems = itemRepository.findByNameIgnoreCaseOrderByIdAsc(name);
        if (!existingItems.isEmpty()) {
            return ItemResponseDto.from(existingItems.get(0));
        }
        if (barcode != null && itemRepository.findByBarcode(barcode).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 바코드입니다.");
        }

        Item item = Item.builder()
                .itemCode(generateItemCode())
                .barcode(barcode != null ? barcode : generateBarcode())
                .name(name)
                .itemType(itemType)
                .category(category)
                .spec(spec)
                .unit(unit)
                .defaultVendor(defaultVendor)
                .standardCost(standardCost)
                .unitPrice(unitPrice)
                .safetyStock(safetyStock)
                .active(active)
                .registeredById(registeredById)
                .build();

        return ItemResponseDto.from(itemRepository.save(item));
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private String normalizeToNull(String value) {
        String normalizedValue = value == null ? "" : value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    private ItemType parseItemType(String value) {
        String normalizedValue = value == null ? "" : value.trim();
        if (normalizedValue.isEmpty()) {
            return ItemType.FINISHED_GOOD;
        }
        try {
            return ItemType.valueOf(normalizedValue.toUpperCase());
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "품목 유형을 확인해 주세요.");
        }
    }

    private String requireText(String value, String message) {
        String normalizedValue = value == null ? "" : value.trim();
        if (normalizedValue.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalizedValue;
    }

    private BigDecimal requireUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "단가를 입력해야 합니다.");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "단가는 0 이상이어야 합니다.");
        }
        return unitPrice;
    }

    private BigDecimal requireNonNegativeAmount(BigDecimal amount, String message) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return amount;
    }

    private int requireNonNegativeInteger(Integer value, String message) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }

    private Vendor findDefaultVendor(Long vendorId) {
        if (vendorId == null) {
            return null;
        }
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "거래처를 찾을 수 없습니다."));
    }

    private String generateItemCode() {
        String itemCode;
        do {
            itemCode = "ITEM-" + LocalDateTime.now().format(ITEM_CODE_FORMAT);
        } while (itemRepository.findByItemCode(itemCode).isPresent());
        return itemCode;
    }

    private String generateBarcode() {
        String barcode;
        do {
            barcode = "BC-" + LocalDateTime.now().format(ITEM_CODE_FORMAT);
        } while (itemRepository.findByBarcode(barcode).isPresent());
        return barcode;
    }
}
