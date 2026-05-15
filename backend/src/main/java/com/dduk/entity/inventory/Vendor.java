package com.dduk.entity.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String vendorCode;

    @Column(nullable = false, unique = true, length = 20)
    private String businessRegistrationNo;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String representativeName;

    @Column(length = 100)
    private String businessType;

    @Column(length = 100)
    private String businessItem;

    @Column(length = 100)
    private String contactName;

    @Column(length = 30)
    private String contactPhone;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String bankName;

    @Column(length = 50)
    private String bankAccountNo;

    @Column(length = 100)
    private String bankAccountHolder;

    @Column(length = 255)
    private String bankbookCopyFilePath;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 255)
    private String memo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Vendor(
            String vendorCode,
            String businessRegistrationNo,
            String name,
            String representativeName,
            String businessType,
            String businessItem,
            String contactName,
            String contactPhone,
            String email,
            String address,
            String bankName,
            String bankAccountNo,
            String bankAccountHolder,
            String bankbookCopyFilePath,
            String status,
            String memo
    ) {
        this.vendorCode = vendorCode;
        this.businessRegistrationNo = businessRegistrationNo;
        this.name = name;
        this.representativeName = representativeName;
        this.businessType = businessType;
        this.businessItem = businessItem;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.email = email;
        this.address = address;
        this.bankName = bankName;
        this.bankAccountNo = bankAccountNo;
        this.bankAccountHolder = bankAccountHolder;
        this.bankbookCopyFilePath = bankbookCopyFilePath;
        this.status = status;
        this.memo = memo;
    }
}
