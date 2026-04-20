package com.vestify.backend.domain.wardrobe.service;

import com.vestify.backend.domain.user.entity.User;
import com.vestify.backend.domain.user.repository.UserRepository;
import com.vestify.backend.domain.wardrobe.entity.ClothingItem;
import com.vestify.backend.domain.wardrobe.enums.ItemCondition;
import com.vestify.backend.domain.wardrobe.enums.ItemSeason;
import com.vestify.backend.domain.wardrobe.enums.ItemStatus;
import com.vestify.backend.domain.wardrobe.repository.ClothingItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClothingItemService {

    private final UserRepository userRepository;
    private final ClothingItemRepository clothingItemRepository;

    public ClothingItem addClothingItem(Long userId, ClothingItem item) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
        item.setUser(user);
        item.setStatus(ItemStatus.WARDROBE); // Yeni oluşturulanlar varsayılan olarak dolaptadır
        log.info("Kullanıcı {} dolabına yeni parça ekliyor: {}", user.getUserName(), item.getName());
        return clothingItemRepository.save(item);
    }

    // SAYFALAMALI VE SİLİNMİŞLERİ GİZLEYEN DOLAP GETİRME
    public Page<ClothingItem> getUserWardrobe(Long userId, Pageable pageable) {
        return clothingItemRepository.findByUserIdAndStatusNot(userId, ItemStatus.DELETED, pageable);
    }

    // SOFT DELETE: Veritabanından silme, durumunu değiştir!
    public void deleteClothingItem(Long itemId) {
        ClothingItem item = clothingItemRepository.findById(itemId)
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
}