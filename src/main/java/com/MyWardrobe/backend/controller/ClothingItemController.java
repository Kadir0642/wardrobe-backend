package com.MyWardrobe.backend.controller;

import com.MyWardrobe.backend.entity.ClothingItem;
import com.MyWardrobe.backend.service.ClothingItemService;
import com.MyWardrobe.backend.service.FileUploadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// "Bean" (Spring'in yönettiği nesne)
@RestController // Bu sınıfın bir API hizmeti sunduğunu ve cevap olarak HTML sayfası değil, saf veri (JSON) döneceğini belirtir.
@RequestMapping("/api/v1/clothes") // Kıyafetlerle ilgili tüm işlemlerin ana adresi.
@RequiredArgsConstructor // dependency enjection
public class ClothingItemController {

    private final ClothingItemService clothingItemService;
    private final FileUploadService fileUploadService; // Pipeline
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON metnini Java Objesine çevirir

    // Kiyafet ekleme ENDPOINT'i
    // Artık sadece JSON değil, MULTIPART (Dosya + Metin) paketi kabul ediyoruz.
    // URL'den kullanıcının ID'sini yakalayacağız (Örn: /api/v1/clothes/1)
    @PostMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClothingItem> addClothingItem(
            @PathVariable Long userId, // Kullanıcı ID'sini JSON'un içine koymak yerine doğrudan URL'den almak doğru bir pratik
            @RequestPart("image") MultipartFile image, // Kargodan fotoyu al | 'image' etiketli parçasını al dosyaya çevir,
            @RequestPart("data")String clothingDataJson) // Kargodan kıyafet bilgilerini al | 'data' etiketli parçasını al metne çevir"
        { // Gelen verileri ClothingItem nesnesine çevirir.

            try{
                // 1. JSON metnini (Renk, marka vs.) Java'nın anlayacağı nesneye çevir
                ClothingItem item = objectMapper.readValue(clothingDataJson, ClothingItem.class);

                // 2. Fotoğrafı Python'a yollar, temizler, Cloudinary'e atar ve linki alır!
                String secureImageUrl = fileUploadService.uploadAndRemoveBackground(image);

                // 3. Buluttan gelen bu temiz linki, kıyafetimizin içine yerleştir
                item.setImageUrl(secureImageUrl);

                // 4. Gelen kıyafet verisini ve URL'deki kullanıcı ID'sini alıp Service yolluyoruz.
                ClothingItem savedItem = clothingItemService.addClothingItem(userId, item);

                return ResponseEntity.ok(savedItem);
            }catch (Exception e) {
                throw new RuntimeException("Sistem mükemmel çalışırken bir pürüz çıktı: " + e.getMessage());
            }
    }

    //Dolabı Görme ENDPOINT (GET) | Veri okuma (GET)
    @GetMapping("/{userId}")  // http://localhost:8080/api/v1/clothes/2
    public ResponseEntity<java.util.List<ClothingItem>> getUserWardrobe(@PathVariable Long userId){
        java.util.List<ClothingItem> wardrobe =clothingItemService.getUserWardrobe(userId);
        return ResponseEntity.ok(wardrobe); //İşlem başarılıysa "200 OK" ile kaydedilen kıyafeti geri dönüyoruz
    }

    //  Veritabanında yepyeni bir kayıt oluştururken  "Create -> POST"
    //  Veriyi okurken "Read -> GET" kullanmıştık.
    //  Var olan bir kaydın üzerindeki bir veriyi (burada giyilme sayısını) güncellediğimiz için " Update -> PUT (TKaydı komple ezip yenile	-> Kıyafetin tüm detaylarını (renk, marka, isim) düzenle)"
    //  Spesifik alan güncellemeleri  için "Update -> PATCH (Kısmi güncellemeler (Yama), Sadece belli bir alanı yama yap -> Sadece giyilme sayısını (+1) artır)"

    // Kıyafeti Giyme Endpoint'i (Kullanıcı kıyafeti giydikçe bu adrese istek atacak)
    @PutMapping("/{itemId}/wear") // Mevcut veriyi güncellerken kullanırız.
    public ResponseEntity<ClothingItem> wearItem(@PathVariable Long itemId){
        ClothingItem updatedItem = clothingItemService.wearClothingItem(itemId);
        return ResponseEntity.ok(updatedItem);
    }

    // Akıllı Fİltrelem ENDPOINT
    // URL: /api/v1/clothes/{userId}/filter?category=Üst Giyim&season=Yaz
    // Mobil Uygulamanın Kullanım Senaryoları:
    //Sadece kışlıkları getir: GET /api/v1/clothes/1/filter?season=Kış
    //Siyah ve Üst Giyim getir: GET /api/v1/clothes/1/filter?category=Üst Giyim&color=Siyah
    //Hiçbir filtre yok, hepsini getir: GET /api/v1/clothes/1/filter
    @GetMapping("/{userId}/filter")
    public ResponseEntity<java.util.List<ClothingItem>> filterWardrobe( // Normalde Spring, URL'de beklediği bir parametreyi bulamazsa "400 Bad Request" (Hatalı İstek) fırlatır. required = false diyerek "Eğer kullanıcı bu filtreyi yollamazsa hata verme, değişkenin içine null koy geç" diyoruz. Bu da Repository'deki o IS NULL sorguyla  uyum içinde çalışır.
            @PathVariable Long userId,// @PathVariable -> Kullanıcının kimliği (userId) mecburi olduğu için onu adrese gömdük
            @RequestParam(required = false) String category, // @RequestParam(required =false) Kullanıcı bu filtreyi seçmek zorunda değil | Query Parameter(Sorgu Parametresi)
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String color
    ){
        java.util.List<ClothingItem> filteredWardrobe = clothingItemService.filterClothes(userId,category,season,color);
        return ResponseEntity.ok(filteredWardrobe);
    }

}
