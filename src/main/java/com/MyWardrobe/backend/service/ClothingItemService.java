package com.MyWardrobe.backend.service;

import com.MyWardrobe.backend.entity.ClothingItem;
import com.MyWardrobe.backend.entity.User;
import com.MyWardrobe.backend.repository.ClothingItemRepository;
import com.MyWardrobe.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClothingItemService {

    private final UserRepository userRepository; // Önce buradan kullanıcıyı bulup
    private final ClothingItemRepository clothingItemRepository; // kıyafetini kaydetmeliyiz.

    // Kıyafet Ekleme İşlemi
    public ClothingItem addClothingItem(Long userId, ClothingItem item){ // Gelen ID ile kullanıcıyı buluyor ve ikisini (user/kıyafet) birbirine zımbalayıp veritabanına yolluyor.

        // 1.KURAL: Bu ID'ye sahip kullanıcı veritabanında gerçekten var mı?
        User user=userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("HATA: Böyle bir kullanıcı bulunamadı!")); // Bulamazsa çalışma zamanı hatası fırlatır uygulamyı çökertmez

        // 2. KIYAFET - SAHIP ILISKISINI AYARLA (Foreign Key Ataması)
        // bunu yapmazsak veritabanı "Bu tişört kimin? der çöker."
        item.setUser(user);

        // 3.KAYDET: Her şey tamamsa Supabase'e gönder.
        System.out.println(user.getUserName()+"adlı kullanıcının dolabına yeni bir parça ekleniyor: "+ item.getName());
        return clothingItemRepository.save(item);
    }

    // Kullanıcının Tüm Dolabını Getirme İşlemi
    public java.util.List<ClothingItem> getUserWardrobe(Long userId){
        System.out.println(userId + " numaralı kullanıcının dolabı açılıyor...");
        return clothingItemRepository.findByUserId(userId);
    }

    //Kıyafeti Giyildi Olarak İşaretle ve İstatisikleri Güncelle
    public ClothingItem wearClothingItem(Long itemId){

        // 1.Kıyafeti bul
        ClothingItem item = clothingItemRepository.findById(itemId)
                .orElseThrow(()-> new RuntimeException("Hata: Kıyafet bulunamadı!"));

        // 2.Giyilme sayısını (wearCount) arttırır. Eğer null ise önce 0 yap, sonra 1 arttırır.
        int currentWearCount = (item.getWearCount()== null) ? 0: item.getWearCount(); // Veritabanında eski bir kayıt varsa ve bu alan null kalmışsa, null+1 (NullPointerException hatası verip çökmesin diye)
        item.setWearCount(currentWearCount+1);

        System.out.println(item.getName()+" bir kez daha giyildi! Toplam giyilme: "+item.getWearCount());

        // 3. Değişiklikleri veritabanına kaydet.
        return clothingItemRepository.save(item);
    }
}
