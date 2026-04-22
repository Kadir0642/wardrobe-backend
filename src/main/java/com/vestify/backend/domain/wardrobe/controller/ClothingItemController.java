package com.vestify.backend.domain.wardrobe.controller;

import com.vestify.backend.domain.wardrobe.dto.ClothingItemDto;
import com.vestify.backend.domain.wardrobe.entity.ClothingItem;
import com.vestify.backend.domain.wardrobe.service.ClothingItemService;
import com.vestify.backend.core.ai.service.AiIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Bu controller; görsel yükleme desteği, tip güvenliği olan
// filtreleme ve yüksek performanslı sayfalama yapısıyla projenin en sağlam parçalarından biri


// "Bean" (Spring'in yönettiği nesne)
@RestController // Bu sınıfın bir API hizmeti sunduğunu ve cevap olarak HTML sayfası değil, saf veri (JSON) döneceğini belirtir.
@RequestMapping("/api/v1/clothes") // Kıyafetlerle ilgili tüm işlemlerin ana adresi.
@RequiredArgsConstructor // dependency enjection
public class ClothingItemController {

    private final ClothingItemService clothingItemService;
    private final AiIntegrationService aiIntegrationService;

    // Spring Boot, Multipart form içindeki JSON'ı @RequestPart ile otomatik DTO'ya çevirir!
    @PostMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // Bu, API'ye "Sana sadece metin gelmeyecek, aynı zamanda bir görsel dosyası da gelecek" demektir.
    public ResponseEntity<ClothingItemDto> addClothingItem(
            @PathVariable Long userId,
            @RequestPart("image") MultipartFile image, // Kullanıcının telefonundan seçtiği ham görsel dosyasını (fotoğrafı) yakalar
            @RequestPart("data") ClothingItem itemData) { // Görselin yanındaki JSON verisini (kıyafetin adı, rengi vb.) yakalayıp otomatik olarak ClothingItem nesnesine çeviri

        // Not: Gerçek senaryoda burada önce image'i Cloudinary'e yükleyip URL'sini itemData'ya set edeceğiz.
        ClothingItem savedItem = clothingItemService.addClothingItem(userId, itemData);

        // Entity'yi DTO'ya çevirip dışarı veriyoruz (Ağır veriler gizlendi)
        ClothingItemDto responseDto = ClothingItemDto.builder()
                .id(savedItem.getId())
                .name(savedItem.getName())
                .category(savedItem.getCategory())
                .imageUrl(savedItem.getImageUrl())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    // Örn: /api/v1/clothes/2?page=0&size=20
    @GetMapping("/{userId}")
    public ResponseEntity<Page<ClothingItem>> getUserWardrobe(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) { // Varsayılan 20 parça getir

        Page<ClothingItem> wardrobe = clothingItemService.getUserWardrobe(userId, pageable);
        return ResponseEntity.ok(wardrobe);
    }

    // Filtreleme (Sayfalamalı)
    @GetMapping("/{userId}/filter")
    public ResponseEntity<Page<ClothingItem>> filterWardrobe(
            @PathVariable Long userId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subCategory,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String condition,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ClothingItem> filteredWardrobe = clothingItemService.filterClothes(
                userId, category, subCategory, season, color, size, condition, pageable);
        return ResponseEntity.ok(filteredWardrobe);
    }

    @PostMapping(value = "/{userId}/ai-extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Map<String, String>>> extractClothesAi(
            @PathVariable Long userId,
            @RequestPart("image") MultipartFile image) {

        try {
            // Orijinal servisindeki extractClothesAsync metodunu çağırıyoruz
            return aiIntegrationService.extractClothesAsync(image, "flat_lay")
                    .map(taskId -> ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                            "task_id", taskId,
                            "message", "Yapay Zeka analizi başladı."
                    )));
        } catch (IOException e) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Görsel okunurken bir hata oluştu.")));
        }
    }

    // ClothingItemController.java içindeki status metodunu güncelliyoruz:
    @GetMapping("/{userId}/ai-status/{taskId}")
    public Mono<ResponseEntity<?>> getAiExtractionStatus(
            @PathVariable Long userId,
            @PathVariable String taskId) {

        return aiIntegrationService.getAiExtractionStatus(taskId)
                .flatMap(aiResponse -> {
                    String status = (String) aiResponse.get("status");

if ("SUCCESS".equals(status)) {
                    List<Map<String, Object>> items = (List<Map<String, Object>>) aiResponse.get("items");
                    
                    // Kıyafetler veritabanına kaydediliyor (Bu kısım zaten çalışıyor!)
                    List<ClothingItem> savedItems = clothingItemService.saveAiGeneratedItems(userId, items);
                    
                    // 🚀 KESİN ÇÖZÜM: JSON hatasını engellemek için tüm nesneyi değil, sadece Cloudinary URL'lerini dönüyoruz!
                    List<String> savedUrls = savedItems.stream()
                            .map(ClothingItem::getImageUrl)
                            .collect(Collectors.toList());
                    
                    return Mono.just(ResponseEntity.ok(Map.of(
                            "status", "COMPLETED",
                            "message", savedItems.size() + " adet kıyafet gardırobuna başarıyla eklendi!",
                            "saved_urls", savedUrls
                    )));
                }

                    // Hala işleniyorsa veya hata varsa durumu dön
                    return Mono.just(ResponseEntity.ok(aiResponse));
                });
    }
}