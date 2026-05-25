package com.vestify.backend.domain.outfit.controller;

import com.vestify.backend.domain.outfit.service.OutfitSuggestionService;
import com.vestify.backend.domain.wardrobe.dto.ClothingItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/outfits/suggest")
@RequiredArgsConstructor
public class OutfitSuggestionController {

    private final OutfitSuggestionService suggestionService;

    @GetMapping
    public ResponseEntity<List<ClothingItemDto>> getSuggestion(
            @RequestParam Long userId,
            // 🚀 DÜZELTME: Artık blueprintIndex yerine React Native'den gelen kategorileri (Örn: TOPS,FULL_BODY) alıyoruz
            @RequestParam(required = false) String categories,
            @RequestParam(defaultValue = "Bilinmiyor") String weatherContext) {

        // Kategorileri Service'e iletiyoruz
        var suggestion = suggestionService.generateSuggestion(userId, categories, weatherContext);

        // MİMARİ KURAL: Dışarıya DTO dönüyoruz.
        List<ClothingItemDto> responseDtos = suggestion.stream()
                .map(item -> ClothingItemDto.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .imageUrl(item.getImageUrl())
                        .category(item.getCategory())
                        .costPerWear(item.getCostPerWear())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDtos);
    }
}