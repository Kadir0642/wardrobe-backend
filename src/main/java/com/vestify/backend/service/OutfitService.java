package com.vestify.backend.service;

import com.vestify.backend.entity.ClothingItem;
import com.vestify.backend.entity.Outfit;
import com.vestify.backend.entity.OutfitLog;
import com.vestify.backend.entity.User;
import com.vestify.backend.repository.ClothingItemRepository;
import com.vestify.backend.repository.OutfitLogRepository;
import com.vestify.backend.repository.OutfitRepository;
import com.vestify.backend.repository.UserRepository;
import com.vestify.backend.dto.ClothingItemDto;
import com.vestify.backend.dto.OutfitDto;

import java.time.LocalDate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutfitService {

    // Bir kombin oluşturmak için kullanıcıyı doğrulamak, kombine parça eklemek için ise o kıyafeti doğrulamalıyız
    private final OutfitRepository outfitRepository; // Dependency Injection X 4 kez
    private final OutfitLogRepository outfitLogRepository;
    private final UserRepository userRepository;
    private final ClothingItemRepository clothingItemRepository; // Parçaların giyilme sayısını artırmak için lazım

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

    // 4. KOMBİNİ DOLABA KAYDET (Save Outfit)
    // Yapay zekanın ürettiği kombini kullanıcı beğenirse bu metot çalışır.
    public Outfit saveOutfit(Long userId, String outfitName,List<Long> clothingItemIds){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("Kullanıcı bulunamadı! "));

        // ID'leri verilen tüm kıyafetleri veritabanından bul ve bir liste yap
        List<ClothingItem> items =  clothingItemRepository.findAllById(clothingItemIds);

        Outfit newOutfit = Outfit.builder()
                .user(user)
                .name(outfitName)
                .clothingItems(items)
                .build();

        System.out.println("Yeni Kombin Kaydedildi: "+ outfitName);
        return outfitRepository.save(newOutfit);
    }

    // 5. KOMBİNİ GİYİLDİ OLARAK İŞARETLE (Log Outfit)
    public OutfitLog logOutfit(Long userId, Long outfitId, String weather, Integer temperature){

        User user = userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("Kullanıcı bulunamadı! "));

        Outfit outfit = outfitRepository.findById(outfitId)
                .orElseThrow(()->new RuntimeException("Kombin bulunamadı! "));

        //DOMİNO ETKİSİ: Kombinin içindeki HER BİR kıyafetin wearCount (Giyilme) sayısını 1 artır!
        for(ClothingItem item : outfit.getClothingItems()) {
            int currentWears = (item.getWearCount() == null) ? 0 : item.getWearCount();
            item.setWearCount(currentWears + 1);
            clothingItemRepository.save(item); // Veritabanını güncelle
        }

        // Takvim kaydını (Log) oluşturur
        OutfitLog log = OutfitLog.builder()
                .user(user)
                .outfit(outfit)
                .wornDate(LocalDate.now()) // Bugünü atar
                .weatherCondition(weather)
                .temperature(temperature)
                .build();

        System.out.println("Kombin Giyme Kaydı Oluşturuldu! Giydiği Kombin: "+ outfit.getName());
        return outfitLogRepository.save(log);
    }
}

