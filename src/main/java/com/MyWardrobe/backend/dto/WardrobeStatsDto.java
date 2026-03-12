package com.MyWardrobe.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WardrobeStatsDto { // MObil uygulamanın Ekranı (Dashboard) | Ekranın ihtiyacı kadar veri barındırır.
    private int totalItems; // Toplam kaç kıyafeti var?
    private Double totalWardrobeValue; // Dolabın maliyeti ne kadar?
    private ClothingItemDto mostWornItem; // En çok giydiği favori parçası hangisi ?
}
