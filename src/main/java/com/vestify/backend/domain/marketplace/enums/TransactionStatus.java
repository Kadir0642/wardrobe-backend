package com.vestify.backend.domain.marketplace.enums;

public enum TransactionStatus {
    PENDING,    // Ödeme bekleniyor / Onay aşamasında
    PAID,       // Ödeme başarıyla alındı, satıcının kargolaması bekleniyor
    SHIPPED,    // Ürün kargoya verildi
    DELIVERED,  // Ürün alıcıya ulaştı
    COMPLETED,  // Alıcı siparişi onayladı ve para satıcıya aktarıldı
    CANCELLED,  // İşlem iptal edildi
    REFUNDED    // Ürün iade edildi, para geri gönderildi
}