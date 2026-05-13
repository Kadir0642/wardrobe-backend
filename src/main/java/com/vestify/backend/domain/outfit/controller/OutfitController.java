package com.vestify.backend.domain.outfit.controller;

import com.vestify.backend.domain.outfit.dto.OutfitDto;
import com.vestify.backend.domain.outfit.dto.SaveArOutfitRequest;
import com.vestify.backend.domain.outfit.entity.Outfit;
import com.vestify.backend.domain.outfit.entity.OutfitLog;
import com.vestify.backend.domain.outfit.service.OutfitService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

//  Kombinlerin kaydedilmesini ve listelenmesini yönetir.

@RestController //  RESTAPI, sistemlere JSON formatında bilgi döner
@RequestMapping("/api/v1/outfits")
@RequiredArgsConstructor
public class OutfitController {

    private final OutfitService outfitService;

    // DTO (Veri Taşıma Objesi) - Mobilden sadece ID listesi ve isim gelecek
    @Data
    public static class OutfitRequest {
        private String name;
        private List<Long> clothingItemIds;
    }

    // Kombin Kaydetme Endpoint'i
    //@PathVariable Long userId: URL'deki (örn: /api/v1/outfits/5/save) 5 değerini yakalar ve hangi kullanıcının kombin yaptığını anlar.
    @PostMapping("/{userId}/save") // ResponseEntity: Sadece veriyi değil, HTTP durum kodunu da (örneğin "200 OK" veya "201 Created") kontrol etmeni sağlar.
    public ResponseEntity<OutfitDto> saveOutfit(@PathVariable Long userId, @RequestBody OutfitRequest request){  // @RequestBody -> Gönderilen JSON paketini (name ve clothingItemIds) alıp OutfitRequest nesnesine doldurur

            // 1. İşlem: Kombini veritabanına ağır Entity olarak kaydet
            Outfit savedOutfit = outfitService.saveOutfit(userId, request.getName(), request.getClothingItemIds());

            // Sadece ID dönmek yerine, Service içindeki "Çevirmen" metodu kullanarak
            // kombinin içindeki tüm kıyafetleri (görselleriyle birlikte) güvenli DTO paketine koyuyoruz!
            OutfitDto responseDto = outfitService.convertToDto(savedOutfit);

            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        }

    //Ör: adresteki .../123/save kısmındaki 123 sayısını alır ve userId değişkenine koyar. Böylece "Hangi kullanıcı için kayıt yapıyorum?" sorusunun cevabını buradan alır.
    //@RequestBody OutfitRequest request -> İsteğin gövdesindeki (body) veriyi alır.
    //Kullanıcı API'ye JSON formatında veri gönderir (Örneğin: {"name": "Mavi Gömlek", "color": "Blue"}). Spring bu JSON'ı otomatik olarak OutfitRequest isimli Java nesnes
    // Örn: /api/v1/outfits/user/1?type=LOOKBOOK&page=0&size=15
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OutfitDto>> getUserOutfits( //@PathVariable Long userId -> URL'deki {userId} kısmını yakalar
            @PathVariable Long userId, // Parametre Bağlama: @PathVariable("id") Long id URL değerini değişkene bağlamak için kullanılır
            @RequestParam(required = false) String type, // Mobilden gelen filtre
            @PageableDefault(size = 15) Pageable pageable){ //  Sayfa boyutu 15
        return ResponseEntity.ok(outfitService.getUserOutfits(userId, type, pageable));
    }

    // Kombin İsmi Güncelleme
    @PutMapping("/{id}")
    public ResponseEntity<OutfitDto> updateOutfit(@PathVariable Long id, @RequestBody OutfitRequest request) {
        // Servis bize hazır çevrilmiş DTO paketi verecek.
        OutfitDto responseDto = outfitService.updateOutfitName(id, request.getName());
        return ResponseEntity.ok(responseDto);
    }

    // Kombin Silme
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOutfit(@PathVariable Long id) {
        outfitService.deleteOutfit(id);
        return ResponseEntity.noContent().build();
    }

    // 🚀 YENİ: AR Kombin Kaydetme Endpoint'i (Sadece Service'e yönlendirir)
    @PostMapping("/save-ar-look")
    public ResponseEntity<OutfitDto> saveArOutfitLook(@RequestBody SaveArOutfitRequest request) {

        // Tüm ağır veritabanı işlemlerini OutfitService'e devrediyoruz!
        Outfit savedOutfit = outfitService.saveArOutfit(request);

        // Kaydedilen kombini güvenli DTO'ya çevirip telefona dönüyoruz
        OutfitDto responseDto = outfitService.convertToDto(savedOutfit);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

}