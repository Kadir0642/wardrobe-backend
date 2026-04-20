package com.vestify.backend.domain.outfit.enums;

public enum ModerationStatus {
    PENDING,    // AI tarafından henüz inceleniyor (Keşfet'e çıkamaz)
    APPROVED,   // AI onayladı, temiz içerik (Keşfet'te görünebilir)
    REJECTED,   // AI uygunsuz buldu (NSFW/Şiddet) - Otomatik engellendi
    FLAGGED     // AI emin olamadı, bir insan (Admin) incelemeli
}