package com.vestify.backend.domain.wardrobe.service;

import com.vestify.backend.domain.wardrobe.dto.ClothingItemDto;
import com.vestify.backend.domain.wardrobe.dto.WardrobeStatsDto;
import com.vestify.backend.domain.wardrobe.entity.ClothingItem;
import com.vestify.backend.domain.wardrobe.enums.ItemStatus;
import com.vestify.backend.domain.wardrobe.repository.ClothingItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;

// uygulamanın "Analitik Paneli" (Dashboard) için veri üreten kısımdır.
// Kullanıcıya gardırobunun finansal ve kullanım özetini çıkarır.
// Java Stream API'nin gücünü kullanarak verileri anlamlı istatistiklere dönüştürür.

@Service
@RequiredArgsConstructor
public class WardrobeDashboardService {

    private final ClothingItemRepository clothingItemRepository;

    //  Hibernate'e "değişiklik takibi (dirty checking) yapmana gerek yok" mesajı veriyoruz.
    //  Bu, uygulamanın daha az RAM harcamasını ve daha hızlı çalışmasını sağlar.
    @Transactional(readOnly = true) // Performans Optimizasyonu: Sadece okuma yapacağımızı belirtip Hibernate'i hızlandırıyoruz.
    public WardrobeStatsDto getUserDashboardStats(Long userId) {

        // KRİTİK: Sadece silinmemiş (aktif) kıyafetleri hesaba kat!
        // Silinmiş kıyafetleri dışarıda bırakıyoruz. (İleride bu hesaplamaları doğrudan SQL sorgusuna yıkacağız ama şimdilik stream'i güvenli hale getirdik).
        List<ClothingItem> clothes = clothingItemRepository.findByUserIdAndStatusNot(userId, ItemStatus.DELETED);

        int totalItems = clothes.size();

        // Stream API: Listenin içinde gezer (stream), fiyatı girilmemiş ürünleri eler (filter)
        // her birinin fiyatını alır (mapToDouble) ve hepsini toplar (sum).
        double totalValue = clothes.stream()
                .filter(item -> item.getPurchasePrice() != null)
                .mapToDouble(ClothingItem::getPurchasePrice)
                .sum();

        // En Çok Giyilen Parça (Most Worn)
        ClothingItem mostWorn = clothes.stream()
                .filter(item -> item.getWearCount() != null && item.getWearCount() > 0)
                .max(Comparator.comparingInt(ClothingItem::getWearCount))
                .orElse(null);

        ClothingItemDto mostWornDto = null;
        if (mostWorn != null) {
            mostWornDto = ClothingItemDto.builder() // Kullanıcı UI tarafındaki bilgileri DTO paketi yapılır.
                    .id(mostWorn.getId())
                    .name(mostWorn.getName())
                    .imageUrl(mostWorn.getImageUrl())
                    .category(mostWorn.getCategory())
                    .costPerWear(mostWorn.getCostPerWear())
                    .build();
        }

        return WardrobeStatsDto.builder() // Bütün bilgileri toplayan DTO paketi hazırlığı
                .totalItems(totalItems)
                .totalWardrobeValue(totalValue)
                .mostWornItem(mostWornDto)
                .build();
    }
}