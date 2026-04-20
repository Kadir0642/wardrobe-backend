package com.vestify.backend.domain.social.enums;

public enum NotificationType {
    NEW_FOLLOWER,        // Biri seni takip ettiğinde
    OUTFIT_LIKED,        // Kombinin beğenildiğinde
    ITEM_SOLD,           // Pazar yerinde eşyan satıldığında
    AI_WEATHER_ALERT,    // Yapay zeka hava durumu uyarısı yaptığında
    SYSTEM_MESSAGE       // Genel sistem duyuruları için
}