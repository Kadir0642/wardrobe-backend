package com.vestify.backend.domain.wardrobe.dto;

import lombok.*;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardrobeStatsDto { // Mobil uygulamanın Ekranı (Dashboard) | Ekranın ihtiyacı kadar veri barındırır.
    private int totalItems; // Toplam kaç kıyafeti var?
    private Double totalWardrobeValue; // Dolabın maliyeti ne kadar?
    private ClothingItemDto mostWornItem; // En çok giydiği favori parçası hangisi ?
}