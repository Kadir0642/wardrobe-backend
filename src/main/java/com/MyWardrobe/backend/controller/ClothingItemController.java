package com.MyWardrobe.backend.controller;

import com.MyWardrobe.backend.entity.ClothingItem;
import com.MyWardrobe.backend.service.ClothingItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // Bu sınıfın bir API hizmeti sunduğunu ve cevap olarak HTML sayfası değil, saf veri (JSON) döneceğini belirtir.
@RequestMapping("/api/v1/clothes") // Kıyafetlerle ilgili tüm işlemlerin ana adresi.
@RequiredArgsConstructor // dependency enjection
public class ClothingItemController {

    private final ClothingItemService clothingItemService;

    // Kiyafet ekleme ENDPOINT'i
    // URL'den kullanıcının ID'sini yakalayacağız (Örn: /api/v1/clothes/1)
    @PostMapping("/{userId}")
    public ResponseEntity<ClothingItem> addClothingItem(
            @PathVariable Long userId, // Kullanıcı ID'sini JSON'un içine koymak yerine doğrudan URL'den almak doğru bir pratik
            @RequestBody ClothingItem item){ // Gelen verileri ClothingItem nesnesine çevirir.

        // Gelen kıyafet verisini ve URL'deki kullanıcı ID'sini alıp Service yolluyoruz.
        ClothingItem savedItem = clothingItemService.addClothingItem(userId,item);

        //İşlem başarılıysa "200 OK" ile kaydedilen kıyafeti geri dönüyoruz
        return ResponseEntity.ok(savedItem);
    }

    //Dolabı Açma ENDPOINT (GET) | Veri okuma (GET)
    @GetMapping("/{userId}")  // http://localhost:8080/api/v1/clothes/2
    public ResponseEntity<java.util.List<ClothingItem>> getUserWardrobe(@PathVariable Long userId){
        java.util.List<ClothingItem> wardrobe =clothingItemService.getUserWardrobe(userId);
        return ResponseEntity.ok(wardrobe);
    }

}
