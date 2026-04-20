package com.vestify.backend.domain.outfit.controller;

import com.vestify.backend.domain.outfit.service.OutfitSuggestionService;
import com.vestify.backend.domain.wardrobe.dto.ClothingItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/outfits/suggest") // Bu adrese yapılan istekler doğrudan AI motorunu çalıştırır.
@RequiredArgsConstructor
public class OutfitSuggestionController { // Akıllı Öneri Kapısı

    private final OutfitSuggestionService suggestionService;

    @GetMapping
    public ResponseEntity<List<ClothingItemDto>> getSuggestion(
            @RequestParam Long userId, // Önceki controller'larda URL içinden (@PathVariable) veri alıyorduk. Burada ise sorgu parametrelerini kullanıyoruz.
            @RequestParam(defaultValue = "0") int blueprintIndex,
            @RequestParam(defaultValue = "Bilinmiyor") String weatherContext) {

        // Servisten gelen Entity listesini alıyoruz
        var suggestion = suggestionService.generateSuggestion(userId, blueprintIndex, weatherContext);

        // MİMARİ KURAL: Dışarıya DTO dönüyoruz. Gereksiz verileri gizliyoruz.
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