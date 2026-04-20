package com.vestify.backend.domain.outfit.enums;

public enum FeedbackReason {
    NONE,                   // Beğenme durumunda boş kalır
    MISMATCHED_CATEGORIES,  // Kategoriler uyumsuz (Örn: Eşofman üstüne klasik ceket)
    COLOR_MISMATCH,         // Renkler uymadı
    TOO_WARM_FOR_WEATHER,   // Hava için çok sıcak
    TOO_COOL_FOR_WEATHER,   // Hava için çok soğuk
    EXCLUDE_SPECIFIC_ITEM,  // Bu parçayı bugün önerme
    DONT_PAIR_THESE         // Bu iki parçayı asla yan yana getirme
}