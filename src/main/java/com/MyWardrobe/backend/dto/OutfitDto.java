package com.MyWardrobe.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class OutfitDto {
    private Long id;
    private String name;
    private List<ClothingItemDto> clothes; // İçinde sadece temizlenmiş DTO kıyafetler var
}
