package com.vestify.backend.domain.outfit.service;

import com.vestify.backend.core.ai.service.AiIntegrationService;
import com.vestify.backend.domain.outfit.dto.AiScoreRequestDto;
import com.vestify.backend.domain.outfit.dto.AiScoreResponseDto;
import com.vestify.backend.domain.wardrobe.entity.ClothingItem;
import com.vestify.backend.domain.wardrobe.enums.ItemStatus;
import com.vestify.backend.domain.wardrobe.repository.ClothingItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutfitSuggestionService {

    private final ClothingItemRepository clothingItemRepository;
    private final AiIntegrationService aiIntegrationService;

    // 🚀 DÜZELTME: Sabit şablonları (AI_BLUEPRINTS) tamamen devreden çıkardık!
    // Artık sistem "Kullanıcı ne isterse onu" yapmakla yükümlü.

    public List<ClothingItem> generateSuggestion(Long userId, String categoriesStr, String weatherContext) {
        log.info("Kullanıcı {} için Dinamik Şablon ({}) ile kombin üretiliyor...", userId, categoriesStr);

        List<ClothingItem> userWardrobe = clothingItemRepository.findByUserIdAndStatusNot(userId, ItemStatus.DELETED);

        // Dolap boşsa sistemi yorma
        if (userWardrobe.isEmpty()) {
            return new ArrayList<>();
        }

        // 🚀 DÜZELTME: React Native'den gelen string'i listeye çeviriyoruz (Örn: "TOPS,BOTTOMS" -> ["TOPS", "BOTTOMS"])
        List<String> selectedCategories = new ArrayList<>();
        if (categoriesStr != null && !categoriesStr.trim().isEmpty()) {
            selectedCategories = Arrays.asList(categoriesStr.split(","));
        } else {
            // Hiçbir şey gelmezse çökmeyi önlemek için Fallback (Savunmacı Programlama)
            selectedCategories = Arrays.asList("TOPS", "BOTTOMS", "FOOTWEAR");
        }

        int maxAiRetries = 5;
        int maxLocalAttempts = 20;
        int aiAttempts = 0;
        int localAttempts = 0;

        List<ClothingItem> bestOutfitSoFar = new ArrayList<>();
        double highestScore = -1.0;

        Set<String> triedCombinations = new HashSet<>();
        Random random = new Random();

        while (aiAttempts < maxAiRetries && localAttempts < maxLocalAttempts) {
            localAttempts++;
            List<ClothingItem> currentAttemptOutfit = new ArrayList<>();
            Set<Long> usedIdsInCurrentOutfit = new HashSet<>();

            // 1. Rastgele Kombini Oluştur (YENİ DİNAMİK LİSTEYE GÖRE)
            for (String category : selectedCategories) {
                // Gönderilen kategoriye uyan ve daha önce bu kombinasyon için seçilmemiş kıyafetleri bul
                List<ClothingItem> matchingItems = userWardrobe.stream()
                        .filter(item -> category.trim().equalsIgnoreCase(item.getCategory()) && !usedIdsInCurrentOutfit.contains(item.getId()))
                        .collect(Collectors.toList());

                // Eğer o kategoriye uygun kıyafet dolapta varsa, rastgele birini çek
                if (!matchingItems.isEmpty()) {
                    ClothingItem randomItem = matchingItems.get(random.nextInt(matchingItems.size()));
                    currentAttemptOutfit.add(randomItem);
                    usedIdsInCurrentOutfit.add(randomItem.getId());
                }
            }

            // Eğer kullanıcının dolabında o kategorilerde HİÇBİR ŞEY yoksa atla
            if (currentAttemptOutfit.isEmpty()) {
                continue;
            }

            // OUTFIT HASHING (KOMBİN PARMAK İZİ)
            String outfitHash = currentAttemptOutfit.stream()
                    .map(item -> String.valueOf(item.getId()))
                    .sorted()
                    .collect(Collectors.joining("-"));

            if (triedCombinations.contains(outfitHash)) {
                continue;
            }

            triedCombinations.add(outfitHash);
            aiAttempts++;

            // 2. Python AI'a Sorulacak DTO'yu Hazırla
            List<AiScoreRequestDto.ItemFeatureDto> features = currentAttemptOutfit.stream().map(item ->
                    AiScoreRequestDto.ItemFeatureDto.builder()
                            .id(item.getId())
                            .category(item.getCategory())
                            .subCategory(item.getSubCategory() != null ? item.getSubCategory() : "unknown")
                            .color(item.getColor() != null ? item.getColor() : "unknown")
                            .style(item.getFormality() != null ? item.getFormality() : "unknown")
                            .build()
            ).collect(Collectors.toList());

            AiScoreRequestDto scoreRequest = AiScoreRequestDto.builder()
                    .userId(userId)
                    .weatherContext(weatherContext)
                    .items(features)
                    .build();

            // 3. AI'a Gönder ve Cevabı Al (Python tarafı 100 üzerinden puan dönecek)
            AiScoreResponseDto aiResponse = null;
            try {
                aiResponse = aiIntegrationService.scoreOutfitAsync(scoreRequest).block();
            } catch (Exception e) {
                log.error("AI Skorlama sırasında hata oluştu: {}", e.getMessage());
            }

            if (aiResponse == null) {
                log.warn("AI'dan boş yanıt alındı, bir sonraki denemeye geçiliyor...");
                continue;
            }

            // En iyiyi hafızada tut (Devre kesici devreye girerse diye)
            if (aiResponse.getScore() > highestScore) {
                highestScore = aiResponse.getScore();
                bestOutfitSoFar = new ArrayList<>(currentAttemptOutfit);
            }

            // 4. Onay Kontrolü (Python bu uyumu beğendi mi?)
            if (Boolean.TRUE.equals(aiResponse.getApproved())) {
                log.info("✅ AI Kombini Onayladı! (Yerel Deneme: {}, Skor: {})", localAttempts, aiResponse.getScore());
                return currentAttemptOutfit;
            }
        }

        // 5. DEVRE KESİCİ (Eğer Python hiçbir şeyi 60 puandan yüksek bulmazsa en iyisini ver)
        log.warn("⚠️ Deneme sınırlarına ulaşıldı! Bulunan en iyi kombin dönülüyor. (Skor: {})", highestScore);
        return bestOutfitSoFar;
    }
}