package com.vestify.backend.domain.wardrobe.service;

import com.vestify.backend.domain.user.entity.User;
import com.vestify.backend.domain.user.repository.UserRepository;
import com.vestify.backend.domain.wardrobe.entity.ClothingItem;
import com.vestify.backend.domain.wardrobe.enums.ItemCondition;
import com.vestify.backend.domain.wardrobe.enums.ItemSeason;
import com.vestify.backend.domain.wardrobe.enums.ItemStatus;
import com.vestify.backend.domain.wardrobe.repository.ClothingItemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Bu servis, uygulamanın "Dijital Gardırop" yöneticisidir.
// Kıyafetlerin sisteme dahil edilmesi, listelenmesi ve "silinmesi" gibi kritik işlemleri yönetir.
// Veriyi gerçekten silmek yerine "Soft Delete" kullanmasıdır. | Sadece kullanıcı görmeyecek ama sistemde durmaya devam edecek.
//  Enum kullanımı sayesinde veritabanında "Kış" yerine "WINTER" gibi standart değerlerin tutulmasını garanti altına alıyor.

@Service
@RequiredArgsConstructor
@Slf4j
public class ClothingItemService {

    private final UserRepository userRepository;
    private final ClothingItemRepository clothingItemRepository;

    public ClothingItem addClothingItem(Long userId, ClothingItem item) { // Gelen kıyafeti (item), userId üzerinden bulduğu kullanıcıyla eşleştirir.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
        item.setUser(user);
        item.setStatus(ItemStatus.WARDROBE); // Yeni oluşturulanlar varsayılan olarak dolaptadır
        log.info("Kullanıcı {} dolabına yeni parça ekliyor: {}", user.getUserName(), item.getName());
        return clothingItemRepository.save(item);
    }

    // SAYFALAMALI VE SİLİNMİŞLERİ GİZLEYEN DOLAP GETİRME
    public Page<ClothingItem> getUserWardrobe(Long userId, Pageable pageable) { // binlerce kıyafeti tek seferde çekip sistemi yormamak için sayfalama (Pageable) kullanır.
        return clothingItemRepository.findByUserIdAndStatusNot(userId, ItemStatus.DELETED, pageable);
    }

    // SOFT DELETE: Veritabanından silmeyip, durumunu "DELETED" olarak değiştir!
    public void deleteClothingItem(Long itemId) { // Eğer kullanıcı yanlışlıkla sildiyse geri getirebilirsin veya o kıyafet geçmişteki bir kombinde (OutfitLog) kayıtlıysa
        ClothingItem item = clothingItemRepository.findById(itemId) // veritabanı ilişkileri bozulmaz. Veri bütünlüğünü korumak için en güvenli yoldur.
                .orElseThrow(() -> new RuntimeException("Kıyafet bulunamadı!"));
        item.setStatus(ItemStatus.DELETED);
        clothingItemRepository.save(item);
        log.info("Kıyafet Soft Delete ile silindi. ID: {}", itemId);
    }

    // ENUM DÜZELTMELERİ YAPILMIŞ AKILLI FİLTRE
    public Page<ClothingItem> filterClothes(Long userId, String category, String subCategory, String seasonStr, String color, String size, String conditionStr, Pageable pageable) {
        ItemSeason season = (seasonStr != null) ? ItemSeason.valueOf(seasonStr.toUpperCase()) : null;
        ItemCondition condition = (conditionStr != null) ? ItemCondition.valueOf(conditionStr.toUpperCase()) : null;

        return clothingItemRepository.filterUserWardrobe(userId, category, subCategory, season, color, size, condition, pageable);
    }


    public List<ClothingItem> saveAiGeneratedItems(Long userId, List<Map<String, Object>> aiItems) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        // Arka planı silinen kıyafetlerin CLIP tarafından özellikleri etiketleniyor ve  bunları supabase veritabanımıza kaydediyoruz.
        List<ClothingItem> itemsToSave = aiItems.stream().map(data -> {
            Map<String, String> tags = (Map<String, String>) data.get("tags");

            String category = (tags != null && tags.containsKey("category")) ? tags.get("category") : "UNKNOWN";
            String color = (tags != null && tags.containsKey("color")) ? tags.get("color") : "Belirtilmedi";
            String subCategory = (tags != null && tags.containsKey("sub_category")) ? tags.get("sub_category") : "Belirtilmedi";

            // String metni ItemSeason Enum'una güvenli bir şekilde çeviren zırh
            ItemSeason parsedSeason = null; // Eğer null kabul etmiyorsa kendi default değerini yaz (Örn: ItemSeason.UNKNOWN)
            try {
                if (tags != null && tags.containsKey("season")) {
                    // Gelen metni BÜYÜK HARFE çevir (summer -> SUMMER) ve Enum ile eşleştir
                    parsedSeason = ItemSeason.valueOf(tags.get("season").toUpperCase());
                }
            } catch (IllegalArgumentException e) {
                // Eğer AI senin Enum listende olmayan bir mevsim uydurursa sistem çökmez, null (veya varsayılan) atanır.
                System.out.println("Eşleşmeyen mevsim değeri atlandı: " + tags.get("season"));
            }

            return ClothingItem.builder()
                    .user(user)
                    .imageUrl((String) data.get("url"))
                    .name("AI Ayıklaması")
                    .category(category)
                    .color(color)
                    .season(parsedSeason) // <-- Artık String değil, güvenli ItemSeason nesnesini veriyoruz!
                    .subCategory(subCategory)
                    .moderationStatus(com.vestify.backend.domain.wardrobe.enums.ModerationStatus.APPROVED)
                    .build();
        }).collect(Collectors.toList());

        return clothingItemRepository.saveAll(itemsToSave);
    }
    
}