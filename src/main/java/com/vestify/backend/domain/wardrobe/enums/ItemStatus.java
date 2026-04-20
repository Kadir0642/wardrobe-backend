package com.vestify.backend.domain.wardrobe.enums;

public enum ItemStatus {
    WARDROBE,
    FOR_SALE,
    SOLD,
    ARCHIVED,
    DELETED // Kullanıcı "Sildi" sanacak ama biz veriyi AI için saklayacağız.
}
