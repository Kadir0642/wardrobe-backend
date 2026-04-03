package com.MyWardrobe.backend.service;

import com.MyWardrobe.backend.dto.WardrobeStatsDto;
import com.MyWardrobe.backend.dto.ClothingItemDto;
import com.MyWardrobe.backend.entity.ClothingItem;
import com.MyWardrobe.backend.entity.User;
import com.MyWardrobe.backend.repository.ClothingItemRepository;
import com.MyWardrobe.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClothingItemService {

    private final UserRepository userRepository; // Önce buradan kullanıcıyı bulup
    private final ClothingItemRepository clothingItemRepository; // kıyafetini kaydetmeliyiz.

    // Kıyafet Ekleme İşlemi
    public ClothingItem addClothingItem(Long userId, ClothingItem item) { // Gelen ID ile kullanıcıyı buluyor ve ikisini (user/kıyafet) birbirine zımbalayıp veritabanına yolluyor.

        // 1.KURAL: Bu ID'ye sahip kullanıcı veritabanında gerçekten var mı?
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("HATA: Böyle bir kullanıcı bulunamadı!")); // Bulamazsa çalışma zamanı hatası fırlatır uygulamyı çökertmez

        // 2. KIYAFET - SAHIP ILISKISINI AYARLA (Foreign Key Ataması)
        // bunu yapmazsak veritabanı "Bu tişört kimin? der çöker."
        item.setUser(user);

        // 3.KAYDET: Her şey tamamsa Supabase'e gönder.
        System.out.println(user.getUserName() + "adlı kullanıcının dolabına yeni bir parça ekleniyor: " + item.getName());
        return clothingItemRepository.save(item);
    }

    // Kullanıcının Tüm Dolabını Getirme İşlemi
    public java.util.List<ClothingItem> getUserWardrobe(Long userId) {
        System.out.println(userId + " numaralı kullanıcının dolabı açılıyor...");
        return clothingItemRepository.findByUserId(userId);
    }

    //Kıyafeti Giyildi Olarak İşaretle ve İstatisikleri Güncelle
    public ClothingItem wearClothingItem(Long itemId) {

        // 1.Kıyafeti bul
        ClothingItem item = clothingItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Hata: Kıyafet bulunamadı!"));

        // 2.Giyilme sayısını (wearCount) arttırır. Eğer null ise önce 0 yap, sonra 1 arttırır.
        int currentWearCount = (item.getWearCount() == null) ? 0 : item.getWearCount(); // Veritabanında eski bir kayıt varsa ve bu alan null kalmışsa, null+1 (NullPointerException hatası verip çökmesin diye)
        item.setWearCount(currentWearCount + 1);

        System.out.println(item.getName() + " bir kez daha giyildi! Toplam giyilme: " + item.getWearCount());

        // 3. Değişiklikleri veritabanına kaydet.
        return clothingItemRepository.save(item);
    }

    // Akıllı filtreleme |  Şuan veriyi Controller'dan alıp Repository'ye iletiyor (Pass-through)
    public java.util.List<ClothingItem> filterClothes(
            Long userId, String category, String season, String color,
            String size, String material, String condition) {

        System.out.println("Filtreleme çalışıyor ... Kategori: " + category + " | Sezon: " + season);
        return clothingItemRepository.filterUserWardrobe(
                userId, category, season, color, size, material, condition);
    }

    // --- ANCHOR ALGORİTHM ---
    public java.util.List<ClothingItem> generateOutfitFromAnchor(Long anchorItemId) {

        // 1. Çapa (Merkez) kıyafeti kontrolü (var/yok)
        ClothingItem anchorItem = clothingItemRepository.findById(anchorItemId)
                .orElseThrow(() -> new RuntimeException("HATA: Çapa kıyafet bulunamadı!"));

        Long userId = anchorItem.getUser().getId();
        String anchorCategory = anchorItem.getCategory(); // Örn: "Dış Giyim"

        // Kombini oluşturacağımız boş liste
        java.util.List<ClothingItem> generatedOutfit = new java.util.ArrayList<>();

        // Çapayı baş köşeye oturturuz (İlk onu ekleriz çünkü onun ETRAFINDA OLUŞACAK KOMBİN)
        generatedOutfit.add(anchorItem);

        // 2. Kural Motoru: Çapanın kategorisine göre eksikleri belirle ve tamamla
        // TEMEL KOMBİN PARÇALARI -> [ DIŞ - ÜST - ALT - AYAKKABI ]
        // İLERİDE: Buradaki rastgele seçim yerine, Python AI servisine istek atacağız!
        if ("Üst Giyim".equalsIgnoreCase(anchorCategory)) {
            addRandomItemToOutfit(userId, "Alt Giyim", generatedOutfit);
            addRandomItemToOutfit(userId, "Ayakkabı", generatedOutfit);
        } else if ("Alt Giyim".equalsIgnoreCase(anchorCategory)) {
            addRandomItemToOutfit(userId, "Üst Giyim", generatedOutfit);
            addRandomItemToOutfit(userId, "Ayakkabı", generatedOutfit);
        } else if ("Dış Giyim".equalsIgnoreCase(anchorCategory)) {
            addRandomItemToOutfit(userId, "Üst Giyim", generatedOutfit);
            addRandomItemToOutfit(userId, "Alt Giyim", generatedOutfit);
            addRandomItemToOutfit(userId, "Ayakkabı", generatedOutfit);
        } else if ("Ayakkabı".equalsIgnoreCase(anchorCategory)) {
            addRandomItemToOutfit(userId, "Üst Giyim", generatedOutfit);
            addRandomItemToOutfit(userId, "Alt Giyim", generatedOutfit);
        }
        System.out.println("✨ AI Kombin Çalıştı! Çapa: " + anchorItem.getName() + " etrafında kombin üretildi.");
        return generatedOutfit;
    }

    // Kombine eksik parçayı ekleyen yardımcı (Private) metod
    private void addRandomItemToOutfit(Long userId, String targetCategory, java.util.List<ClothingItem> outfit) {
        java.util.List<ClothingItem> availableItems = clothingItemRepository.findByUserIdAndCategory(userId, targetCategory);

        if (!availableItems.isEmpty()) {
            // Dolapta o kategoriden eşya varsa, şimdilik rastgele birini seç
            int randomIndex = new java.util.Random().nextInt(availableItems.size());
            outfit.add(availableItems.get(randomIndex));
        }
    }

    // --- YENİ: ANALİZ VE İSTATİSTİK MOTORU ---
    public WardrobeStatsDto getWardrobeStatistics(Long userId) {
        // 1. Kullanıcının tüm dolabını getir
        java.util.List<ClothingItem> allItems = clothingItemRepository.findByUserId(userId);

        int totalItems = allItems.size();
        Double totalValue = 0.0;

        // 2. Dolabın Toplam Maliyetini Hesapla
        for (ClothingItem item : allItems) {
            // Eğer kıyafetin bir fiyatı (purchasePrice) girilmişse toplama ekle
            if (item.getPurchasePrice() != null) {
                totalValue += item.getPurchasePrice();
            }
        }

        // 3. En çok giyilen favori parçayı bul
        ClothingItem mostWorn = clothingItemRepository.findFirstByUserIdOrderByWearCountDesc(userId);
        ClothingItemDto mostWornDto = null;

        if (mostWorn != null) {
            // CPW (Cost Per Wear - Giyme Başına Maliyet) Hesaplaması
            // Fiyatı, giyilme sayısına bölüyoruz. (Eğer hiç giyilmediyse 1'e böl ki sonsuzluk hatası vermesin)
            Double price = (mostWorn.getPurchasePrice() != null) ? mostWorn.getPurchasePrice() : 0.0;
            int wears = (mostWorn.getWearCount() != null && mostWorn.getWearCount() > 0) ? mostWorn.getWearCount() : 1;
            Double cpw = Math.round((price / wears) * 100.0) / 100.0; // Virgülden sonra 2 hane (Örn: 70.50₺) | Math.round -> 0.5 ve üzeriyse yukarı (pozitif yönde), 0.5'ten küçükse aşağı yuvarlama yapar

            // Ağır Entity'i, hafif DTO'ya çevir
            mostWornDto = ClothingItemDto.builder()
                    .id(mostWorn.getId())
                    .name(mostWorn.getName())
                    .imageUrl(mostWorn.getImageUrl())
                    .category(mostWorn.getCategory())
                    .costPerWear(cpw)
                    .build();
        }

        // 4. Kargo Kutusunu (DTO) Paketle ve Gönder!
        System.out.println(userId + " numaralı kullanıcının dolap analizi hesaplandı. Toplam Değer: " + totalValue + "₺");
        return WardrobeStatsDto.builder()
                .totalItems(totalItems)
                .totalWardrobeValue(totalValue)
                .mostWornItem(mostWornDto)
                .build();
    }

    // --- KIYAFET BİLGİLERİNİ GÜNCELLEME ---
    public ClothingItem updateClothingItem(Long itemId, ClothingItem updatedData) {
        ClothingItem existingItem = clothingItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Kıyafet bulunamadı!"));

        // Temel Bilgiler
        if(updatedData.getBrand() != null) existingItem.setBrand(updatedData.getBrand());
        if(updatedData.getCategory() != null) existingItem.setCategory(updatedData.getCategory());
        if(updatedData.getColor() != null) existingItem.setColor(updatedData.getColor());
        if(updatedData.getSeason() != null) existingItem.setSeason(updatedData.getSeason());
        if(updatedData.getName() != null) existingItem.setName(updatedData.getName());

        // Yeni Eklenen Form Bilgileri
        if(updatedData.getSize() != null) existingItem.setSize(updatedData.getSize());
        if(updatedData.getShoppingUrl() != null) existingItem.setShoppingUrl(updatedData.getShoppingUrl());
        if(updatedData.getPersonalNote() != null) existingItem.setPersonalNote(updatedData.getPersonalNote());
        if(updatedData.getDescription() != null) existingItem.setDescription(updatedData.getDescription());
        if(updatedData.getCondition() != null) existingItem.setCondition(updatedData.getCondition());
        if(updatedData.getMaterial() != null) existingItem.setMaterial(updatedData.getMaterial());
        if(updatedData.getOrigin() != null) existingItem.setOrigin(updatedData.getOrigin());
        if(updatedData.getPurchasedDate() != null) existingItem.setPurchasedDate(updatedData.getPurchasedDate());

        return clothingItemRepository.save(existingItem);
    }
}