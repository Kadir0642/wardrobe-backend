package com.vestify.backend.domain.outfit.service;

import com.vestify.backend.core.ai.service.AiIntegrationService; // YENİ: AiServiceClient yerine Core'daki kabloyu kullanıyoruz.
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
    private final AiIntegrationService aiIntegrationService; // YENİ KABLO BAĞLANDI

    // 3 Katmanlı Şablon (Blueprint) Sistemi
    private static final List<List<String>> AI_BLUEPRINTS = Arrays.asList(
            Arrays.asList("Tops", "Bottoms", "Footwear", "Accessories"),                                  // 0: Basic
            Arrays.asList("Full_body", "Footwear", "Accessories", "Accessories", "Accessories"),          // 1: Full Body
            Arrays.asList("Outerwear", "Tops", "Bottoms", "Footwear", "Accessories", "Accessories")       // 2: Layered
    );

    public List<ClothingItem> generateSuggestion(Long userId, int blueprintIndex, String weatherContext) {
        log.info("Kullanıcı {} için {} numaralı şablonla kombin üretiliyor...", userId, blueprintIndex);

        // Mimar Notu: İleride Soft Delete yapısına tam geçildiğinde burası
        // clothingItemRepository.findByUserIdAndStatusNot(userId, ItemStatus.DELETED) olarak güncellenecek!
        List<ClothingItem> userWardrobe = clothingItemRepository.findByUserIdAndStatusNot(userId, ItemStatus.DELETED);

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
                            // SubCategory eklendi, Pattern SİLİNDİ!
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

            // 3. AI'a Gönder ve Cevabı Al (GÜNCELLEME BURADA)
            // Asenkron metodu çağırdık ve .block() ile AI'ın cevabını bekledik.
            AiScoreResponseDto aiResponse = null;
            try {
                aiResponse = aiIntegrationService.scoreOutfitAsync(scoreRequest).block();
            } catch (Exception e) {
                log.error("AI Skorlama sırasında hata oluştu: {}", e.getMessage());
            }

            // Fallback (Güvenlik Ağı): Eğer AI çökerse döngüyü kırma, pas geç.
            if (aiResponse == null) {
                log.warn("AI'dan boş yanıt alındı, bir sonraki denemeye geçiliyor...");
                continue;
            }

            // En yüksek skorlu olanı cepte tutuyoruz (Eğer hiçbiri onaylanmazsa en iyisini vereceğiz)
            if (aiResponse.getScore() > highestScore) {
                highestScore = aiResponse.getScore();
                bestOutfitSoFar = new ArrayList<>(currentAttemptOutfit);
            }

            // 4. Onay Kontrolü
            if (Boolean.TRUE.equals(aiResponse.getApproved())) {
                log.info("✅ AI Kombini Onayladı! (Deneme: {}, Skor: {})", attempts, aiResponse.getScore());
                return currentAttemptOutfit; // Onaylandıysa döngüyü kır ve kombini ver!
            } else {
                log.warn("❌ AI Reddetti (Skor: {}). Yeniden deneniyor... (Deneme: {})", aiResponse.getScore(), attempts);
            }
        }

        // 5. DEVRE KESİCİ (Fallback)
        log.warn("⚠️ Maksimum deneme sayısına ulaşıldı! Bulunan en iyi kombin dönülüyor. (Skor: {})", highestScore);
        return bestOutfitSoFar;
    }
}