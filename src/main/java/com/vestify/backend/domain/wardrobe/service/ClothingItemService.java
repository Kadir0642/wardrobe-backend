package com.vestify.backend.domain.wardrobe.service;

import com.vestify.backend.domain.user.entity.User;
import com.vestify.backend.domain.user.repository.UserRepository;
import com.vestify.backend.domain.wardrobe.entity.ClothingItem;
import com.vestify.backend.domain.wardrobe.enums.ItemCondition;
import com.vestify.backend.domain.wardrobe.enums.ItemSeason;
import com.vestify.backend.domain.wardrobe.enums.ItemStatus;
import com.vestify.backend.domain.wardrobe.enums.ModerationStatus;
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

    @Transactional 
    public List<ClothingItem> saveAiGeneratedItems(Long userId, List<Map<String, Object>> aiItems) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        List<ClothingItem> itemsToSave = aiItems.stream().map(data -> {
            // Python'dan (AI) gelen 'tags' sözlüğünü al
            Map<String, String> tags = (Map<String, String>) data.get("tags");

            // 1. Python'dan Gelenleri Doğru Yakala (Eğer tag gelmezse UNKNOWN veya Belirtilmedi bas)
            String category = (tags != null && tags.containsKey("category")) ? tags.get("category") : "UNKNOWN";
            String subCategory = (tags != null && tags.containsKey("sub_category")) ? tags.get("sub_category") : "UNKNOWN";
            String color = (tags != null && tags.containsKey("color")) ? tags.get("color") : "UNKNOWN";
            String formality = (tags != null && tags.containsKey("formality")) ? tags.get("formality") : "UNKNOWN";

            // 2. Mevsimi (Season) Enum'a Çevirme
            ItemSeason parsedSeason = null;
            try {
                if (tags != null && tags.containsKey("season")) {
                    parsedSeason = ItemSeason.valueOf(tags.get("season").toUpperCase());
                }
            } catch (Exception e) {}

            // 3. Veritabanına Yazılacak (Builder) Nesnesi
            return ClothingItem.builder()
                    .user(user)
                    .name("AI Ayıklaması") // Mobilde kullanıcı burayı sonradan değiştirebilir.
                    .imageUrl((String) data.get("url"))
                    .category(category)
                    .subCategory(subCategory)
                    .color(color)
                    .formality(formality)
                    .season(parsedSeason)
                    .status(com.vestify.backend.domain.wardrobe.enums.ItemStatus.WARDROBE)
                    .wearCount(0)
                    .isSharable(false)
                    .isFavorite(false)
                    .moderationStatus(com.vestify.backend.domain.wardrobe.enums.ModerationStatus.APPROVED)
                    .purchasePrice(0.0)
                    .loveFactor(0)
                    .build();
        }).collect(Collectors.toList());

        // 4. Oluşturulan listeyi veritabanına TEK SEFERDE kaydet
        return clothingItemRepository.saveAll(itemsToSave);
    }
    
}