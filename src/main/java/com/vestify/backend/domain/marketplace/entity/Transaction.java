package com.vestify.backend.domain.marketplace.entity;

import com.vestify.backend.domain.user.entity.User;
import com.vestify.backend.domain.marketplace.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Alıcı (Satın alan kişi)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    // Satıcı (Parayı alacak kişi)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    // Hangi ürün satıldı? (Bunu önceden konuştuğumuz MarketplaceListing tablosuna bağlıyoruz)
    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    // Ödenen net tutar (Kargo ve komisyon dahil)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status; // PENDING, PAID, SHIPPED, DELIVERED, REFUNDED

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber; // Kargo takip numarası

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}