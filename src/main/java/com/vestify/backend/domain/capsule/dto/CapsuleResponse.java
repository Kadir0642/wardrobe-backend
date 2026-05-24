package com.vestify.backend.domain.capsule.dto;

import lombok.Data;
import java.util.List;

@Data
public class CapsuleResponse {  // AI'ın üreteceği nihai çıktı
    private String capsuleTitle;
    private List<OutfitDetails> outfits;

    @Data
    public static class OutfitDetails {
        private String outfitName;
        private List<String> userItems;        // Kullanıcının dolabından seçilen Kıyafet ID'leri
        private String partnerUpsellItem;     // (Affiliate) Mağazadan satılacak ortak marka Kıyafet ID'si | Ortaklık kurarak ürün satışı
        private String stylistPitch;          // AI'ın lüks satış/stil cümlesi
    }
}