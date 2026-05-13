package com.vestify.backend.domain.outfit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor // 🚀 JSON parse hatalarını önler
@AllArgsConstructor // 🚀 JSON parse hatalarını önler
public class SaveArOutfitRequest {
    private Long userId;
    private String name;
    private String outfitImageUrl;
    private String type;
    private String canvasData;
    private List<Long> clothingItemIds; // Kombindeki kıyafetlerin ID'leri
}