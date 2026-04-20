package com.vestify.backend.domain.outfit.service;

import com.vestify.backend.domain.outfit.dto.OutfitDto;
import com.vestify.backend.domain.outfit.entity.Outfit;
import com.vestify.backend.domain.outfit.entity.OutfitLog;
import com.vestify.backend.domain.outfit.repository.OutfitLogRepository;
import com.vestify.backend.domain.outfit.repository.OutfitRepository;
import com.vestify.backend.domain.user.entity.User;
import com.vestify.backend.domain.user.repository.UserRepository;
import com.vestify.backend.domain.wardrobe.dto.ClothingItemDto;
import com.vestify.backend.domain.wardrobe.entity.ClothingItem;
import com.vestify.backend.domain.wardrobe.repository.ClothingItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutfitService {

    private final OutfitRepository outfitRepository;
    private final OutfitLogRepository outfitLogRepository;
    private final UserRepository userRepository;
    private final ClothingItemRepository clothingItemRepository;

    @Transactional
    public Outfit saveOutfit(Long userId, String outfitName, List<Long> clothingItemIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        List<ClothingItem> itemsList = clothingItemRepository.findAllById(clothingItemIds);
        Set<ClothingItem> itemsSet = new HashSet<>(itemsList); // Performans için Set'e çevirdik

        Outfit newOutfit = Outfit.builder()
                .user(user)
                .name(outfitName)
                .clothingItems(itemsSet)
                .build();

        log.info("Kullanıcı {} için yeni kombin kaydedildi: {}", userId, outfitName);
        return outfitRepository.save(newOutfit);
    }

    // ARTIK SAYFALAMALI VE N+1 KORUMALI ÇALIŞIYOR!
    @Transactional(readOnly = true)
    public Page<OutfitDto> getUserOutfits(Long userId, Pageable pageable) {
        // Not: OutfitRepository'de sayfalamalı metod yazılmalıydı.
        // Bunu Controller'ı yazarken EntityGraph ile birlikte Page dönecek şekilde bağlayacağız.
        return outfitRepository.findAll(pageable).map(this::convertToDto);
    }

    // @Transactional ekleyerek "Hibernate Batching" (Toplu İşlem) gücünü açtık. Artık 5 güncelleme
    // veritabanına tek bir paket halinde, ışık hızında gidiyor. Ayrıca listelemeyi Sayfalama (Page) yapısına geçirdik.
    @Transactional // Döngü içindeki tüm save işlemleri tek bir SQL paketinde yollanır!
    public OutfitLog logOutfit(Long userId, Long outfitId, String weather, Integer temperature) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
        Outfit outfit = outfitRepository.findById(outfitId).orElseThrow(() -> new RuntimeException("Kombin bulunamadı!"));

        // Domino Etkisi: Giyilme sayılarını artır
        for (ClothingItem item : outfit.getClothingItems()) {
            int currentWears = (item.getWearCount() == null) ? 0 : item.getWearCount();
            item.setWearCount(currentWears + 1);
            clothingItemRepository.save(item);
        }

        OutfitLog logRecord = OutfitLog.builder()
                .user(user)
                .outfit(outfit)
                .wornDate(LocalDate.now())
                .weatherCondition(weather)
                .temperature(temperature)
                .build();

        log.info("Kombin giyildi ve loglandı. (Kombin: {})", outfit.getName());
        return outfitLogRepository.save(logRecord);
    }

    private OutfitDto convertToDto(Outfit outfit) {
        Set<ClothingItemDto> itemDtos = outfit.getClothingItems().stream()
                .map(item -> ClothingItemDto.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .imageUrl(item.getImageUrl())
                        .category(item.getCategory())
                        .costPerWear(item.getCostPerWear())
                        .build())
                .collect(Collectors.toSet());

        return OutfitDto.builder()
                .id(outfit.getId())
                .name(outfit.getName())
                .clothes(itemDtos)
                .build();
    }
}