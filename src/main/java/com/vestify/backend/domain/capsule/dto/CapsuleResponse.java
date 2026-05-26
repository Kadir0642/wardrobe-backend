package com.vestify.backend.domain.capsule.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 🚀 KURŞUN GEÇİRMEZ KALKAN: Bilinmeyen alan gelirse çökme, yoksay!
public class CapsuleResponse {

    private String capsuleTitle;
    private List<String> coreCapsuleItemIds; // 🚀 EKSİK OLAN VE SİSTEMİ ÇÖKERTEN ALAN EKLENDİ
    private List<OutfitDto> outfits;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OutfitDto {
        private String outfitName;
        private List<String> userItems;
        private String partnerUpsellItem;
        private String stylistPitch;
    }
}