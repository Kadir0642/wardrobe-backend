package com.vestify.backend.domain.capsule.dto;

import lombok.Data;

@Data
public class CapsuleRequest {
    private Long userId;          // Doğrudan Long olarak alalım, çevirmeyle uğraşmayalım
    private String mode;          // "TRAVEL" veya "EVENT"
    private String magicContext;  // Sihirli Kutu'dan gelen metin (Örn: "Eski sevgilimin düğünü" veya "Bodrum Tatili")
    private String weatherContext; //(Örn: "Sunny, 28°C" veya mobilin algıladığı hava)
    private Integer days;         // Seyahat için gün sayısı
    private Integer totalOutfits; // Mobilden hesaplanıp gelen üretilecek toplam kombin (days + 5)
}