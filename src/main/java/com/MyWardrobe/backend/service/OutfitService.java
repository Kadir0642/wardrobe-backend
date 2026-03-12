package com.MyWardrobe.backend.service;

import com.MyWardrobe.backend.entity.ClothingItem;
import com.MyWardrobe.backend.entity.Outfit;
import com.MyWardrobe.backend.entity.User;
import com.MyWardrobe.backend.repository.ClothingItemRepository;
import com.MyWardrobe.backend.repository.OutfitRepository;
import com.MyWardrobe.backend.repository.UserRepository;
import com.MyWardrobe.backend.dto.ClothingItemDto;
import com.MyWardrobe.backend.dto.OutfitDto;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutfitService {

    // Bir kombin oluşturmak için kullanıcıyı doğrulamak, kombine parça eklemek için ise o kıyafeti doğrulamalıyız
    private final OutfitRepository outfitRepository; // Dependency Injection X 3 kez
    private final UserRepository userRepository;
    private final ClothingItemRepository clothingItemRepository;

    // 1. Yeni ve Boş Bir Kombin Oluştur.
    public Outfit createOutfit(Long userId, String outfitName){
        User user = userRepository.findById(userId) // Kullanıcı kontrolü
                .orElseThrow(()->new RuntimeException("Hata: Kullanıcı bulunamadı !"));

        Outfit outfit = Outfit.builder() // BOŞ kombin oluşturma
                .name(outfitName)
                .user(user)
                .clothingItems(new ArrayList<>()) // Başlangıçta içi boş
                .build();

        return outfitRepository.save(outfit); // Boş kombini kaydediyor
    }

    // 2.Kombinin İçine Kıyafet Ekle (Many-to-Many)
    public Outfit addItemToOutfit(Long outfitId, Long itemId){
        Outfit outfit = outfitRepository.findById(outfitId) // Kombin kontrolü
                .orElseThrow(()-> new RuntimeException("Hata: Kombin bulunamadı !"));

        ClothingItem item = clothingItemRepository.findById(itemId)
                .orElseThrow(()-> new RuntimeException("Hata: Kıyafet bulunamadı !"));

        // Eğer kıyafet (kıyafeti içerme durumu) zaten kombinde yoksa, ekle!
        if(!outfit.getClothingItems().contains(item)){ // "Defansif Programlama" örneği. Kullanıcı yanlışlıkla butona iki kez bassa bile
            outfit.getClothingItems().add(item); // aynı tişört aynı kombine iki kere eklenemeyecek.
        }
        return outfitRepository.save(outfit);
    }

    // 3. Kullanıcının Tüm Kombinlerini Listele (ARTIK DTO DÖNÜYOR!)
    // Servis katmanı artık sadece veri çekmiyor, aynı zamanda veriyi "sunuma hazırlama" görevini de üstleniyor.
    // Repository'den çıkan ağır Outfit nesneleri, kapıdan (Controller) dışarı çıkmadan hemen önce convertToDto filtresinden geçerek OutfitDto'ya dönüşüyor.
    public List<OutfitDto> getUserOutfits(Long userId) { //
        return outfitRepository.findByUserId(userId)
                .stream()
                .map(this::convertToDto) // Veritabanından geleni kargo kutusuna koy
                .collect(Collectors.toList());
    }

    // 4. Kombini Giy (İçindeki tüm kıyafetlerin istatistiklerini otomatik güncelle!)
    public Outfit wearOutfit(Long outfitId) {
        Outfit outfit = outfitRepository.findById(outfitId) // kombin kontrolü
                .orElseThrow(() -> new RuntimeException("Hata: Kombin bulunamadı!"));

        System.out.println(outfit.getName() + " kombini giyiliyor. İstatistikler güncelleniyor...");

        // Kombinin içindeki HER BİR kıyafeti tek tek gez
        for (ClothingItem item : outfit.getClothingItems()) {
            int currentWearCount = (item.getWearCount() == null) ? 0 : item.getWearCount();
            item.setWearCount(currentWearCount + 1); // Giyilme sayısını 1 artır

            // Güncel istatistikleriyle kıyafeti kaydet
            clothingItemRepository.save(item);
        }

        return outfit; // Güncellenmiş kombini geri döndür
    }

    // --- MİMARİ DÖNÜŞTÜRÜCÜ (MAPPER) ---
    private OutfitDto convertToDto(Outfit outfit) {
        // 1. Kombinin içindeki ağır kıyafetleri, hafif DTO'lara çevir (Gereksiz veri yükünden kurtarır)
        // Kombinin içindeki her bir ağır ClothingItem nesnesini bandın (stream) üzerine koyar, map işlemiyle onları hafif ClothingItemDto kutularına dönüştürür ve en son collect ile yepyeni, temiz bir liste oluşturur.
        List<ClothingItemDto> itemDtos = outfit.getClothingItems().stream()
                .map(item -> ClothingItemDto.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .imageUrl(item.getImageUrl())
                        .category(item.getCategory())
                        .costPerWear(item.getCostPerWear())
                        .build())
                .collect(Collectors.toList());

        // 2. Hafif kıyafetleri, hafif kombin kutusuna koy ve gönder
        return OutfitDto.builder()
                .id(outfit.getId())
                .name(outfit.getName())
                .clothes(itemDtos)
                .build();
    }
}

