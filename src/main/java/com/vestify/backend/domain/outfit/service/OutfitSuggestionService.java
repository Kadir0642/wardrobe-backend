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

// Bu service, rastgele kıyafet seçmiyor; seçtiği kombinleri Python AI servisine onaylatana kadar (veya deneme sınırı dolana kadar) denemeye devam ediyor.
// Kıyafetlerin özelliklerini sayısal dizilimlere (vektörlere) çevirip, bunların birbiriyle stil uyumunu tam olarak bahsettiğin o Kosinüs Benzerliği (Cosine Similarity) ile ölçen bir puanlama sistemimiz var.
@Service
@RequiredArgsConstructor
@Slf4j
public class OutfitSuggestionService {

    private final ClothingItemRepository clothingItemRepository;
    private final AiIntegrationService aiIntegrationService;

    // 3 Katmanlı Şablon (Blueprint) Sistemi
    private static final List<List<String>> AI_BLUEPRINTS = Arrays.asList(
            Arrays.asList("Tops", "Bottoms", "Footwear", "Accessories"),                                  // 0: Basic
            Arrays.asList("Full_body", "Footwear", "Accessories", "Accessories", "Accessories"),          // 1: Full Body
            Arrays.asList("Outerwear", "Tops", "Bottoms", "Footwear", "Accessories", "Accessories")       // 2: Layered
    );

    //  --- Akıllı Seçim ve Filtreleme ---
    public List<ClothingItem> generateSuggestion(Long userId, int blueprintIndex, String weatherContext) {
        log.info("Kullanıcı {} için {} numaralı şablonla kombin üretiliyor...", userId, blueprintIndex);

        // Mimar Notu: İleride Soft Delete yapısına tam geçildiğinde burası
        // Soft Delete Kontrolü: findByUserIdAndStatusNot(userId, ItemStatus.DELETED) ile
        // kullanıcının dolabındaki silinmiş öğeleri hariç tutarak güncel kıyafet listesini çeker.
        List<ClothingItem> userWardrobe = clothingItemRepository.findByUserIdAndStatusNot(userId, ItemStatus.DELETED);

        // Dolap boşsa direkt boş liste dön, sistemi yorma
        if (userWardrobe.isEmpty()) {
            return new ArrayList<>();
        }

        int indexToUse = (blueprintIndex >= 0 && blueprintIndex < AI_BLUEPRINTS.size()) ? blueprintIndex : 0;
        List<String> selectedBlueprint = AI_BLUEPRINTS.get(indexToUse);


        int maxAiRetries = 5; // AI'ın onaylaması için maksimum deneme sayısı
        int maxLocalAttempts = 20; // Dolap darsa, kendi içimizde sonsuz döngüye girmemek için sınır!
        int aiAttempts = 0;
        int localAttempts = 0;

        List<ClothingItem> bestOutfitSoFar = new ArrayList<>();
        double highestScore = -1.0;

        // MİMARİ EKLENTİ: Daha önce denenen kombinlerin parmak izini tutan hafıza
        Set<String> triedCombinations = new HashSet<>();

        Random random = new Random();

        // Döngü: Hem AI deneme hakkımız hem de Yerel deneme hakkımız bitene kadar
        while (aiAttempts < maxAiRetries && localAttempts < maxLocalAttempts) {
            localAttempts++;
            List<ClothingItem> currentAttemptOutfit = new ArrayList<>();
            Set<Long> usedIdsInCurrentOutfit = new HashSet<>();

            // 1. Rastgele Kombini Oluştur
            for (String category : selectedBlueprint) {
                List<ClothingItem> matchingItems = userWardrobe.stream()
                        .filter(item -> category.equalsIgnoreCase(item.getCategory()) && !usedIdsInCurrentOutfit.contains(item.getId()))
                        .collect(Collectors.toList());

                if (!matchingItems.isEmpty()) {
                    ClothingItem randomItem = matchingItems.get(random.nextInt(matchingItems.size()));
                    currentAttemptOutfit.add(randomItem);
                    usedIdsInCurrentOutfit.add(randomItem.getId());
                }
            }

            // Güvenlik: Eğer hiçbir şey eşleşmediyse (boş kombin çıktıysa) atla
            if (currentAttemptOutfit.isEmpty()) {
                continue;
            }

            // OUTFIT HASHING (KOMBİN PARMAK İZİ)
            // Kombindeki kıyafetlerin ID'lerini sıralayıp birleştiriyoruz (Örn: "15-42-88")
            // Sıralıyoruz çünkü (15-42) ile (42-15) aynı kombindir!
            String outfitHash = currentAttemptOutfit.stream()
                    .map(item -> String.valueOf(item.getId()))
                    .sorted()
                    .collect(Collectors.joining("-"));

            // Eğer bu kombini daha önce denediysek, AI'ı boşuna yorma ve direkt yeni baştan üret!
            if (triedCombinations.contains(outfitHash)) {
                log.debug("Bu kombin daha önce denendi (Hash: {}). AI çağrısı yapılmadan atlanıyor.", outfitHash);
                continue; // aiAttempts'i artırmadan direkt başa dön!
            }

            // Yeni bir kombin bulduk, hafızaya ekle
            triedCombinations.add(outfitHash);
            aiAttempts++; // Artık gerçekten AI'a gidiyoruz, hakkımızı 1 düşür.

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

            // 3. AI'a Gönder ve Cevabı Al (GÜNCELLEME BURADA)
            // Asenkron metodu çağırdık ve .block() ile AI'ın cevabını bekledik.
            // Metodun çağrıldığı anda sonuç hemen dönmez. İşlem arka planda devam ederken, ana akış bloklanmaz.
            AiScoreResponseDto aiResponse = null;
            try { // scoreOutfitAsync(...) -> aslında asenkron bir metod (Mono (en fazla bir adet sonuç veya hata üretir) döner). Ancak burada kombin önerisi anlık bir sonuç gerektirdiği için .block() kullanarak AI'dan cevap gelene kadar akışı bekletiyoruz.
                aiResponse = aiIntegrationService.scoreOutfitAsync(scoreRequest).block();
            } catch (Exception e) {
                log.error("AI Skorlama sırasında hata oluştu: {}", e.getMessage());
            }

            if (aiResponse == null) {
                log.warn("AI'dan boş yanıt alındı, bir sonraki denemeye geçiliyor...");
                continue;
            }

            if (aiResponse.getScore() > highestScore) {
                highestScore = aiResponse.getScore();
                bestOutfitSoFar = new ArrayList<>(currentAttemptOutfit);
            }

            // 4. Onay Kontrolü
            if (Boolean.TRUE.equals(aiResponse.getApproved())) {
                log.info("✅ AI Kombini Onayladı! (AI Denemesi: {}, Yerel Deneme: {}, Skor: {})", aiAttempts, localAttempts, aiResponse.getScore());
                return currentAttemptOutfit;
            } else {
                log.warn("❌ AI Reddetti (Skor: {}). Yeniden deneniyor...", aiResponse.getScore());
            }
        }

        // 5. DEVRE KESİCİ (Fallback)
        log.warn("⚠️ Deneme sınırlarına ulaşıldı! (AI Çağrısı: {}, Yerel: {}). Bulunan en iyi kombin dönülüyor. (Skor: {})", aiAttempts, localAttempts, highestScore);
        return bestOutfitSoFar;
    }
}