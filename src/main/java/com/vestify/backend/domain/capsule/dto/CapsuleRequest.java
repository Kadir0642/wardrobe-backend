package com.vestify.backend.domain.capsule.dto;

import lombok.Data;

@Data
public class CapsuleRequest { // mobil uygulamanın bize göndereceği istek paketi
    private String userId;
    private String mode;        // "travel" veya "event"
    private String target;      // "London" veya "Wedding"
    private String date;        // "Nov 12 - Nov 16"
    private String temperature; // Mobilin Open-Meteo'dan çektiği derece (Örn: "12°C")
    private String tripPurpose; // "Leisure" veya "Business"
}