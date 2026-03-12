package com.MyWardrobe.backend.service;

import com.MyWardrobe.backend.dto.ClothingItemDto;
import com.MyWardrobe.backend.dto.WardrobeStatsDto;
import com.MyWardrobe.backend.entity.ClothingItem;
import com.MyWardrobe.backend.repository.ClothingItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClothingItemRepository clothingItemRepository;

    public WardrobeStatsDto getUserDashboardStats(Long userId) {
        List<ClothingItem> clothes = clothingItemRepository.findByUserId(userId);

        // 1. Toplam Kıyafet Sayısı
        int totalItems = clothes.size();

        // 2. Dolabın Toplam Değeri (Fiyatı girilmiş olanları topla) | Stream API (Veri Akışı)
        double totalValue = clothes.stream()
                .filter(item -> item.getPurchasePrice() != null) // "Kıyafetleri banda koyup (stream), sadece fiyatı girilmiş olanları filtreler, o kıyafetlerin içinden sadece fiyat etiketlerini kopar al (mapToDouble) ve hepsini topla" diyorsun.
                .mapToDouble(ClothingItem::getPurchasePrice)
                .sum();

        // 3. En Çok Giyilen Kıyafeti Bul (Giyilme sayısı en yüksek olanı seç)
        ClothingItem mostWorn = clothes.stream()
                .filter(item -> item.getWearCount() != null && item.getWearCount() > 0) // Önce hiç giyilmemişleri ve hata payı olanları (null) eliriz. Sonra kalanları giyilme sayısına göre birbiriyle kıyaslayıp en büyüğünü alıyoruz.
                .max(Comparator.comparingInt(ClothingItem::getWearCount))
                .orElse(null); // Bulamazsak null döneriz

        // Bulunan favori kıyafeti DTO kargo kutusuna çevir
        ClothingItemDto mostWornDto = null; // Ağır bilginin olduğu mostWorn verisini daha gereksiz yüklerden ayırır şekilde kutular.
        if (mostWorn != null) {
            mostWornDto = ClothingItemDto.builder()
                    .id(mostWorn.getId())
                    .name(mostWorn.getName())
                    .imageUrl(mostWorn.getImageUrl())
                    .category(mostWorn.getCategory())
                    .costPerWear(mostWorn.getCostPerWear())
                    .build();
        }

        // 4. Tüm verileri paketle ve gönder!
        return WardrobeStatsDto.builder()
                .totalItems(totalItems)
                .totalWardrobeValue(totalValue)
                .mostWornItem(mostWornDto)
                .build();
    }
}
