package com.dduk.service.inventory;

import org.springframework.stereotype.Service;

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
public class VendorService {
}
