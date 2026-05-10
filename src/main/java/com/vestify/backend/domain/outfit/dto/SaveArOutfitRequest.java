package com.vestify.backend.domain.outfit.dto;

import lombok.Data;
import java.util.List;

@Data
public class SaveArOutfitRequest {
    private Long userId;
    private String name;
    private String outfitImageUrl;
    private List<Long> clothingItemIds; // Kombindeki kıyafetlerin ID'leri
}