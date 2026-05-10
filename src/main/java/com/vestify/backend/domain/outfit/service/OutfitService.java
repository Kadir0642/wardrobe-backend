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


// Bu servis, uygulamanın "Gardırop ve Kombin Yönetimi" merkezi.
// Sadece veri kaydetmiyor, aynı zamanda verimlilik ve performans odaklı (Sayfalama, Batching) yaklaşımlar içeriyor.

@Service
@RequiredArgsConstructor
@Slf4j
public class OutfitService {

    // Dört farklı tabloya erişiyor : bu sebeple servis oldukça merkezi bir düğüm noktasıdır.
    private final OutfitRepository outfitRepository;
    private final OutfitLogRepository outfitLogRepository;
    private final UserRepository userRepository;
    private final ClothingItemRepository clothingItemRepository;

    @Transactional // Kullanıcın seçtiği parçaları bir araya getirip bir "Kombin" olarak kaydeder.
    public Outfit saveOutfit(Long userId, String outfitName, List<Long> clothingItemIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        List<ClothingItem> itemsList = clothingItemRepository.findAllById(clothingItemIds);
        Set<ClothingItem> itemsSet = new HashSet<>(itemsList); // Performans için Set'e çevirdik
        // HashSet Kullanımı: itemsList (Liste) olarak gelen parçaları Set'e çeviriyoruz.
        // Neden? Çünkü bir kombinde aynı kıyafetten iki tane olamaz.
        // Set veri yapısı tekrar eden (yinelenen) kayıtları engellerken arama performansını artırır


        Outfit newOutfit = Outfit.builder()
                .user(user)
                .name(outfitName)
                .clothingItems(itemsSet)
                .moderationStatus(com.vestify.backend.domain.outfit.enums.ModerationStatus.APPROVED)
                .build();

        log.info("Kullanıcı {} için yeni kombin kaydedildi: {}", userId, outfitName);
        return outfitRepository.save(newOutfit);
    }

    // 🚀 YENİ: AR Giydirme veya Canvas (Moodboard) sonucunu portfolyoya kaydeder
    @Transactional
    public Outfit saveArOutfit(com.vestify.backend.domain.outfit.dto.SaveArOutfitRequest request) {
        // 1. Kullanıcıyı bul
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        // 2. Kıyafetleri bul ve Set'e çevir (Senin performans optimizasyonun)
        List<ClothingItem> itemsList = clothingItemRepository.findAllById(request.getClothingItemIds());
        Set<ClothingItem> itemsSet = new HashSet<>(itemsList);

        // 3. Yeni Kombini (Outfit) oluştur ve AR görselini ekle
        Outfit newOutfit = Outfit.builder()
                .user(user)
                .name(request.getName())
                .clothingItems(itemsSet)
                // 🚀 AR Görselinin Cloudinary Linki:
                .outfitImageUrl(request.getOutfitImageUrl())
                .moderationStatus(com.vestify.backend.domain.outfit.enums.ModerationStatus.PENDING) // Sosyal ağ için AI moderasyon bekliyor
                .build();

        log.info("Kullanıcı {} için AR kombini portfolyoya eklendi: {}", request.getUserId(), request.getName());
        return outfitRepository.save(newOutfit);
    }


    // ARTIK SAYFALAMALI VE N+1 KORUMALI ÇALIŞIYOR!
    // Pageable -> Eğer kullanıcının 500 tane kombini varsa, hepsini tek seferde çekmek uygulamayı yavaşlatır ve belleği (RAM) tüketir.
    // Page<OutfitDto> dönerek sadece istenen sayfayı (örneğin ilk 10 kaydı) getirirsin.
    @Transactional(readOnly = true) // Veritabanına "Sadece okuma yapacağım, veriyi değiştirmeyeceğim" diyorsun. Bu, veritabanı seviyesinde performans optimizasyonu sağlar.
    public Page<OutfitDto> getUserOutfits(Long userId, Pageable pageable) {
        // Not: OutfitRepository'de sayfalamalı metod yazılmalı.
        // Bunu Controller'ı yazarken EntityGraph ile birlikte Page dönecek şekilde bağlayacağız.
        return outfitRepository.findByUserId(userId, pageable).map(this::convertToDto);
    }

    // @Transactional ekleyerek "Hibernate Batching" (Toplu İşlem) gücünü açtık. Artık 5 güncelleme
    // veritabanına tek bir paket halinde, ışık hızında gidiyor. Ayrıca listelemeyi Sayfalama (Page) yapısına geçirdik.
    @Transactional // Döngü içindeki tüm save işlemleri tek bir SQL paketinde (SQL Batch) yollanır! | veritabanı ile uygulama arasındaki "git-gel" trafiğini azaltır.
    public OutfitLog logOutfit(Long userId, Long outfitId, String weather, Integer temperature) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
        Outfit outfit = outfitRepository.findById(outfitId).orElseThrow(() -> new RuntimeException("Kombin bulunamadı!"));

        // Kombindeki her bir kıyafetin giyilme sayılarını artır kombindeki her bir kıyafetin
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

    public OutfitDto convertToDto(Outfit outfit) {
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
                .outfitImageUrl(outfit.getOutfitImageUrl()) // Database'deki AR görselini DTO'ya koyuyoruz.
                .build();
    }

    @Transactional  // Outfit adını değiştiriyoruz. 
    public OutfitDto updateOutfitName(Long id, String newName) {
        Outfit outfit = outfitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("outfit not found!"));
        outfit.setName(newName);
        Outfit updatedOutfit = outfitRepository.save(outfit);
        // Veritabanı tüneli (Transaction) hala açıkken 
        // kıyafetleri alıp DTO'ya çeviriyoruz!
        return convertToDto(updatedOutfit);
        

    }

    @Transactional   // Kombin silme 
    public void deleteOutfit(Long id) {
        if (!outfitRepository.existsById(id)) {
            throw new RuntimeException("outfit not found!");
        }
        outfitRepository.deleteById(id);
    }
}