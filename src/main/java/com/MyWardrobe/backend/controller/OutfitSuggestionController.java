package com.MyWardrobe.backend.controller;

import com.MyWardrobe.backend.entity.ClothingItem;
import com.MyWardrobe.backend.service.OutfitSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/outfits/suggest")
@RequiredArgsConstructor
public class OutfitSuggestionController {

    private final OutfitSuggestionService suggestionService;

    // Örnek İstek: GET http://localhost:8080/api/v1/outfits/suggest?userId=3&blueprintIndex=2&weatherContext=Bilecik,10C
    @GetMapping
    public ResponseEntity<List<ClothingItem>> getSuggestion(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int blueprintIndex,
            @RequestParam(defaultValue = "Bilinmiyor") String weatherContext) { // 🚀 YENİ EKLENDİ

        // 🚀 METODA 3. PARAMETRE OLARAK EKLENDİ
        List<ClothingItem> suggestion = suggestionService.generateSuggestion(userId, blueprintIndex, weatherContext);
        return ResponseEntity.ok(suggestion);
    }
}