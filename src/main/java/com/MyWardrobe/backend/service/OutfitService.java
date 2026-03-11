package com.MyWardrobe.backend.service;

import com.MyWardrobe.backend.entity.ClothingItem;
import com.MyWardrobe.backend.entity.Outfit;
import com.MyWardrobe.backend.entity.User;
import com.MyWardrobe.backend.repository.ClothingItemRepository;
import com.MyWardrobe.backend.repository.OutfitRepository;
import com.MyWardrobe.backend.repository.UserRepository;
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

    // 3.Kullanıcının Tüm kombinlerini listele
    public List<Outfit> getUserOutfits(Long userId){
        return outfitRepository.findByUserId(userId);
    }
}
