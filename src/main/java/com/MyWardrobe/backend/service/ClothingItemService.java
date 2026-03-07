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
}
