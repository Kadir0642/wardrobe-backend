package com.MyWardrobe.backend.controller;

import com.MyWardrobe.backend.entity.Outfit;
import com.MyWardrobe.backend.service.OutfitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController //  RESTAPI, sistemlere JSON formatında bilgi döner
@RequestMapping("/api/v1/outfits")
@RequiredArgsConstructor
public class OutfitController {

    private final OutfitService outfitService;

    // Yeni kombin oluşturma kapısı
    @PostMapping("/{userId}") // POST /api/v1/outfits/1?name=Kışlık
    public ResponseEntity<Outfit> createOutfit(
            @PathVariable Long userId,
            @RequestParam String name){
        return ResponseEntity.ok(outfitService.createOutfit(userId,name));
    }

    // KOmbine kıyafet ekleme kapısı | Belirli bir kombinin içine, belirli bir kıyafeti ekliyor
    @PostMapping("/{outfitId}/items/{itemId}") // POST /api/v1/outfits/5/items/12
    public ResponseEntity<Outfit> addItemToOutfit(
            @PathVariable Long outfitId,
            @PathVariable Long itemId){
        return ResponseEntity.ok(outfitService.addItemToOutfit(outfitId,itemId));
    }

    // Tüm kombinleri ve içindekileri dünyaya JSON olarak sununan ENDPOINT
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Outfit>> getUserOutfits(@PathVariable Long userId){
        return ResponseEntity.ok(outfitService.getUserOutfits(userId));
    }
}
