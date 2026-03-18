package com.MyWardrobe.backend.controller;

import com.MyWardrobe.backend.entity.Outfit;
import com.MyWardrobe.backend.entity.OutfitLog;
import com.MyWardrobe.backend.service.OutfitService;
import com.MyWardrobe.backend.dto.OutfitDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController //  RESTAPI, sistemlere JSON formatında bilgi döner
@RequestMapping("/api/v1/outfits")
@RequiredArgsConstructor
public class OutfitController {

    private final OutfitService outfitService;

    // DTO (Veri Taşıma Objesi) - Mobilden sadece ID listesi ve isim gelecek
    public static class OutfitRequest{
        public String name;
        public List<Long> clothingItemIds;
    }

    public static class LogRequest{
        public String weather;
        public List<Long> temperature;
    }

    // Kombin Kaydetme Endpoint'i
    @PostMapping("/{userId}/save")   // ResponseEntity: Sadece veriyi değil, HTTP durum kodunu da (örneğin "200 OK" veya "201 Created") kontrol etmeni sağlar.
    public ResponseEntity<Outfit> saveOutfit(@PathVariable Long userId, @RequestBody OutfitRequest request){

        Outfit savedOutfit = outfitService.saveOutfit(userId, request.name, request.clothingItemIds);
        return ResponseEntity.ok(savedOutfit);
    }

    // Kombini Giyme (LOG) Endpoint'i  | // Parametre Bağlama: @PathVariable("id") Long id URL değerini değişkene bağlamak için kullanılır
    //@PathVariable Long userId -> URL'deki {userId} kısmını yakalar.
    //Ör: adresteki .../123/save kısmındaki 123 sayısını alır ve userId değişkenine koyar. Böylece "Hangi kullanıcı için kayıt yapıyorum?" sorusunun cevabını buradan alır.
    //@RequestBody OutfitRequest request -> İsteğin gövdesindeki (body) veriyi alır.
    //Kullanıcı API'ye JSON formatında veri gönderir (Örneğin: {"name": "Mavi Gömlek", "color": "Blue"}). Spring bu JSON'ı otomatik olarak OutfitRequest isimli Java nesnesine dönüştürür
    @PostMapping("/{userId}/log/{outfitId}")
    public ResponseEntity<OutfitLog> logOutfit(
            @PathVariable Long userId,
            @PathVariable Long outfitId,
            @RequestBody(required = false) LogRequest request){

        String weather = (request != null) ? request.weather : null;
        Integer temp = (request != null) ? request.temperature.size() : null;

        OutfitLog log = outfitService.logOutfit(userId, outfitId, weather, temp);
        return ResponseEntity.ok(log);
    }

    // Yeni kombin oluşturma kapısı
    @PostMapping("/{userId}") // POST /api/v1/outfits/1?name=Kışlık
    public ResponseEntity<Outfit> createOutfit(
            @PathVariable Long userId,
            @RequestParam String name){
        return ResponseEntity.ok(outfitService.createOutfit(userId,name));
    }

    // Kombine kıyafet ekleme kapısı | Belirli bir kombinin içine, belirli bir kıyafeti ekliyor
    @PostMapping("/{outfitId}/items/{itemId}") // POST /api/v1/outfits/5/items/12
    public ResponseEntity<Outfit> addItemToOutfit(
            @PathVariable Long outfitId,
            @PathVariable Long itemId){
        return ResponseEntity.ok(outfitService.addItemToOutfit(outfitId,itemId));
    }

    // Tüm kombinleri ve içindekileri dışarıya JSON olarak sununan ENDPOINT
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OutfitDto>> getUserOutfits(@PathVariable Long userId){
        return ResponseEntity.ok(outfitService.getUserOutfits(userId));
    }

}
