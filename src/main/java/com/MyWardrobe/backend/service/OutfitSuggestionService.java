package com.MyWardrobe.backend.service;

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

    //  3 Katmanlı Şablon (Blueprint) Sistemi
    private static final List<List<String>> AI_BLUEPRINTS = Arrays.asList(
            Arrays.asList("Tops", "Bottoms", "Footwear", "Accessories"),                                  // 0: Basic
            Arrays.asList("Full_body", "Footwear", "Accessories", "Accessories", "Accessories"),          // 1: Full Body
            Arrays.asList("Outerwear", "Tops", "Bottoms", "Footwear", "Accessories", "Accessories")       // 2: Layered
    );

    public List<ClothingItem> generateSuggestion(Long userId, int blueprintIndex) {
        log.info("Kullanıcı {} için {} numaralı şablonla kombin üretiliyor...", userId, blueprintIndex);

        // 1. Kullanıcının tüm dolabını veritabanından çek (Örn: /api/v1/clothes/3 ucunun kullandığı metot)
        List<ClothingItem> userWardrobe = clothingItemRepository.findByUserId(userId);

        // Şablon sınırlarını koruma (0, 1 veya 2)
        int indexToUse = (blueprintIndex >= 0 && blueprintIndex < AI_BLUEPRINTS.size()) ? blueprintIndex : 0;
        List<String> selectedBlueprint = AI_BLUEPRINTS.get(indexToUse);

        List<ClothingItem> suggestedOutfit = new ArrayList<>();
        Set<Long> usedIds = new HashSet<>();
        Random random = new Random();

        // 2. Şablondaki her kategori için dolaptan rastgele, benzersiz bir parça seç
        for (String category : selectedBlueprint) {
            List<ClothingItem> matchingItems = userWardrobe.stream()
                    .filter(item -> item.getCategory().equalsIgnoreCase(category) && !usedIds.contains(item.getId()))
                    .collect(Collectors.toList());

            if (!matchingItems.isEmpty()) {
                // Şimdilik rastgele seçiyoruz. PYTHON AI eklendiğinde bu satır: "Python'dan en yüksek skorlu olanı getir" olacak!
                ClothingItem randomItem = matchingItems.get(random.nextInt(matchingItems.size()));
                suggestedOutfit.add(randomItem);
                usedIds.add(randomItem.getId());
            }
        }

        return suggestedOutfit;
    }
}