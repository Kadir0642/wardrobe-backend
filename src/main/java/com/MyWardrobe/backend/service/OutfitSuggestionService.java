package com.MyWardrobe.backend.service;

import com.MyWardrobe.backend.dto.AiScoreRequestDto;
import com.MyWardrobe.backend.dto.AiScoreResponseDto;
import com.MyWardrobe.backend.entity.ClothingItem;
import com.MyWardrobe.backend.repository.ClothingItemRepository;
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
    private final AiServiceClient aiServiceClient;

    // 3 Katmanlı Şablon (Blueprint) Sistemi
    private static final List<List<String>> AI_BLUEPRINTS = Arrays.asList(
            Arrays.asList("Tops", "Bottoms", "Footwear", "Accessories"),                                  // 0: Basic
            Arrays.asList("Full_body", "Footwear", "Accessories", "Accessories", "Accessories"),          // 1: Full Body
            Arrays.asList("Outerwear", "Tops", "Bottoms", "Footwear", "Accessories", "Accessories")       // 2: Layered
    );

    public List<ClothingItem> generateSuggestion(Long userId, int blueprintIndex, String weatherContext) {
        log.info("Kullanıcı {} için {} numaralı şablonla kombin üretiliyor...", userId, blueprintIndex);

        List<ClothingItem> userWardrobe = clothingItemRepository.findByUserId(userId);

        // Dolap boşsa direkt boş liste dön, sistemi yorma
        if (userWardrobe.isEmpty()) {
            return new ArrayList<>();
        }

        int indexToUse = (blueprintIndex >= 0 && blueprintIndex < AI_BLUEPRINTS.size()) ? blueprintIndex : 0;
        List<String> selectedBlueprint = AI_BLUEPRINTS.get(indexToUse);

        int maxRetries = 5; // AI'ın onaylaması için maksimum deneme sayısı
        int attempts = 0;
        List<ClothingItem> bestOutfitSoFar = new ArrayList<>();
        double highestScore = -1.0;

        Random random = new Random();

        // 🚀 DÖNGÜ BAŞLIYOR (Maksimum 5 kere dener)
        while (attempts < maxRetries) {
            attempts++;
            List<ClothingItem> currentAttemptOutfit = new ArrayList<>();
            Set<Long> usedIds = new HashSet<>();

            // 1. Rastgele Kombini Oluştur
            for (String category : selectedBlueprint) {
                List<ClothingItem> matchingItems = userWardrobe.stream()
                        .filter(item -> category.equalsIgnoreCase(item.getCategory()) && !usedIds.contains(item.getId()))
                        .collect(Collectors.toList());

                if (!matchingItems.isEmpty()) {
                    ClothingItem randomItem = matchingItems.get(random.nextInt(matchingItems.size()));
                    currentAttemptOutfit.add(randomItem);
                    usedIds.add(randomItem.getId());
                }
            }

// 2. Python AI'a Sorulacak DTO'yu Hazırla
            List<AiScoreRequestDto.ItemFeatureDto> features = currentAttemptOutfit.stream().map(item ->
                    AiScoreRequestDto.ItemFeatureDto.builder()
                            .id(item.getId())
                            .category(item.getCategory())
                            .color(item.getColor() != null ? item.getColor() : "unknown")
                            .style(item.getFormality() != null ? item.getFormality() : "unknown") // style yerine formality kullandık
                            .pattern(item.getPattern() != null ? item.getPattern() : "unknown")
                            .build()
            ).collect(Collectors.toList());

            AiScoreRequestDto scoreRequest = AiScoreRequestDto.builder()
                    .userId(userId) // user_id yerine userId yaptık!
                    .weatherContext(weatherContext) // weather_context yerine weatherContext yaptık!
                    .items(features)
                    .build();

            // 3. AI'a Gönder ve Cevabı Al
            AiScoreResponseDto aiResponse = aiServiceClient.scoreOutfit(scoreRequest);

            // En yüksek skorlu olanı cepte tutuyoruz (Eğer hiçbiri onaylanmazsa en iyisini vereceğiz)
            if (aiResponse.getScore() > highestScore) {
                highestScore = aiResponse.getScore();
                bestOutfitSoFar = new ArrayList<>(currentAttemptOutfit);
            }

            // 4. Onay Kontrolü
            if (aiResponse.getApproved()) {
                log.info("✅ AI Kombini Onayladı! (Deneme: {}, Skor: {})", attempts, aiResponse.getScore());
                return currentAttemptOutfit; // Onaylandıysa döngüyü kır ve kombini ver!
            } else {
                log.warn("❌ AI Reddetti (Skor: {}). Yeniden deneniyor... (Deneme: {})", aiResponse.getScore(), attempts);
            }
        }

        // 5. DEVRE KESİCİ (Fallback)
        // Eğer 5 denemede de AI onaylamadıysa, bu 5 deneme içindeki EN YÜKSEK skorlu olanı veriyoruz.
        log.warn("⚠️ Maksimum deneme sayısına ulaşıldı! Bulunan en iyi kombin dönülüyor. (Skor: {})", highestScore);
        return bestOutfitSoFar;
    }
}