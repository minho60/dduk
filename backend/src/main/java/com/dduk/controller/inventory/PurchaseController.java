package com.dduk.controller.inventory;

import com.dduk.config.PrincipalDetails;
import com.dduk.domain.inventory.purchase.PurchaseService;
import com.dduk.domain.inventory.purchase.dto.PurchaseRequestCreateDto;
import com.dduk.domain.inventory.purchase.dto.PurchaseRequestResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping
    public PurchaseRequestResponseDto createPurchaseRequest(
            @RequestBody PurchaseRequestCreateDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Long memberId = principalDetails != null ? principalDetails.getMember().getId() : null;
        return purchaseService.createPurchaseRequest(requestDto, memberId);
    }
}
