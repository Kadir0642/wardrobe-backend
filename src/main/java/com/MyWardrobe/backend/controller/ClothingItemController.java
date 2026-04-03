package com.MyWardrobe.backend.controller;

import com.MyWardrobe.backend.dto.WardrobeStatsDto;
import com.MyWardrobe.backend.entity.ClothingItem;
import com.MyWardrobe.backend.service.ClothingItemService;
import com.MyWardrobe.backend.service.AiVisionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// "Bean" (Spring'in yönettiği nesne)
@RestController // Bu sınıfın bir API hizmeti sunduğunu ve cevap olarak HTML sayfası değil, saf veri (JSON) döneceğini belirtir.
@RequestMapping("/api/v1/clothes") // Kıyafetlerle ilgili tüm işlemlerin ana adresi.
@RequiredArgsConstructor // dependency enjection
public class ClothingItemController {

    private final ClothingItemService clothingItemService;
    private final AiVisionService aiVisionService; // Pipeline
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON metnini Java Objesine çevirir

    // --- 🚀 YENİ: AI ASENKRON KOMBİN PARÇALAMA (AŞAMA 1) ---
    // URL: POST /api/v1/clothes/{userId}/ai-extract
    @PostMapping(value = "/{userId}/ai-extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> startAiExtraction(
            @PathVariable Long userId,
            @RequestPart("image") MultipartFile image) {
        try {
            // Fotoğrafı Python'a yolla ve sadece Fişi (Task ID) al
            String taskId = aiVisionService.extractClothesAsync(image, "flat_lay");
            return ResponseEntity.ok(Map.of("task_id", taskId, "message", "Yapay Zeka analizi başladı."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // --- 🚀 YENİ: AI DURUM SORGULAMA VE VERİTABANINA KAYDETME (AŞAMA 2) ---
    // URL: GET /api/v1/clothes/{userId}/ai-status/{taskId}
    @GetMapping("/{userId}/ai-status/{taskId}")
    public ResponseEntity<?> checkAiStatusAndSave(
            @PathVariable Long userId,
            @PathVariable String taskId) {
        try {
            Map<String, Object> aiResponse = aiVisionService.checkTaskStatus(taskId);

            // Eğer Python işlemi bitirmişse (SUCCESS), JSON'ı parçala ve DB'ye kaydet!
            if ("SUCCESS".equals(aiResponse.get("status"))) {
                Map<String, Object> result = (Map<String, Object>) aiResponse.get("result");
                List<Map<String, Object>> itemsData = (List<Map<String, Object>>) result.get("items");

                List<ClothingItem> savedItems = new ArrayList<>();

                for (Map<String, Object> itemData : itemsData) {
                    String url = (String) itemData.get("url");
                    Map<String, String> tags = (Map<String, String>) itemData.get("tags");

                    ClothingItem item = new ClothingItem();
                    item.setImageUrl(url);
                    item.setCategory(tags.get("category"));
                    item.setColor(tags.get("color"));
                    item.setPattern(tags.get("pattern"));
                    item.setSeason(tags.get("season"));
                    item.setFormality(tags.get("style"));

                    // İsim boş kalmasın diye AI verilerinden otomatik isim üretiyoruz
                    item.setName("AI: " + tags.get("color") + " " + tags.get("category"));

                    // Senin servisini çağırıp veritabanına kaydediyoruz
                    savedItems.add(clothingItemService.addClothingItem(userId, item));
                }

                // Kaydedilen gerçek veritabanı objelerini mobile dönüyoruz
                return ResponseEntity.ok(Map.of("status", "COMPLETED", "saved_items", savedItems));
            }

            // Eğer hala PENDING ise, mobile PENDING (Askıda işlem) döner
            return ResponseEntity.ok(aiResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // --- ESKİ: MANUEL KIYAFET EKLEME (Yedek olarak duruyor) --- [ value = "/{userId}/manual"  | addClothingItemManual]
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
                // Not: Manuel eklemede imageUrl uygulamanın kendisinden gelmeli

                // 2. Gelen kıyafet verisini ve URL'deki kullanıcı ID'sini alıp Service yolluyoruz.
                ClothingItem savedItem = clothingItemService.addClothingItem(userId, item);
                return ResponseEntity.ok(savedItem);
            }catch (Exception e) {
                throw new RuntimeException("Kayıt sırasında hata: " + e.getMessage());
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
    //  Var olan bir kaydın üzerindeki bir veriyi (burada giyilme sayısını) güncellediğimiz için " Update -> PUT (Kaydı komple ezip yenile	-> Kıyafetin tüm detaylarını (renk, marka, isim) düzenle)"
    //  Spesifik alan güncellemeleri  için "Update -> PATCH (Kısmi güncellemeler (Yama), Sadece belli bir alanı yama yap -> Sadece giyilme sayısını (+1) artır)"

    // Kıyafeti Giyme Endpoint'i (Kullanıcı kıyafeti giydikçe bu adrese istek atacak)
    @PutMapping("/{itemId}/wear") // Mevcut veriyi güncellerken kullanırız.
    public ResponseEntity<ClothingItem> wearItem(@PathVariable Long itemId){
        ClothingItem updatedItem = clothingItemService.wearClothingItem(itemId);
        return ResponseEntity.ok(updatedItem);
    }

    // Akıllı Filtreleme ENDPOINT
    // Örn: GET /api/v1/clothes/3/filter?category=Tops&season=WINTER&size=M
    // Mobil Uygulamanın Kullanım Senaryoları:
    //Sadece kışlıkları getir: GET /api/v1/clothes/1/filter?season=Kış
    //Siyah ve Üst Giyim getir: GET /api/v1/clothes/1/filter?category=Üst Giyim&color=Siyah
    //Hiçbir filtre yok, hepsini getir: GET /api/v1/clothes/1/filter
    @GetMapping("/{userId}/filter")
    public ResponseEntity<java.util.List<ClothingItem>> filterWardrobe( // Normalde Spring, URL'de beklediği bir parametreyi bulamazsa "400 Bad Request" (Hatalı İstek) fırlatır. required = false diyerek "Eğer kullanıcı bu filtreyi yollamazsa hata verme, değişkenin içine null koy geç" diyoruz. Bu da Repository'deki o IS NULL sorguyla  uyum içinde çalışır.
            @PathVariable Long userId, // @PathVariable -> Kullanıcının kimliği (userId) mecburi olduğu için onu adrese gömdük
            @RequestParam(required = false) String category, // @RequestParam(required =false) Kullanıcı bu filtreyi seçmek zorunda değil | Query Parameter(Sorgu Parametresi)
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String condition
    ){
        java.util.List<ClothingItem> filteredWardrobe = clothingItemService.filterClothes(
                userId, category, season, color, size, material, condition);
        return ResponseEntity.ok(filteredWardrobe);
    }

    // --- ANCHOR ALGROİTHM ENDPOINT ---
    // URL: GET /api/v1/clothes/{itemId}/generate-outfit
    // Kullanıcı bir kıyafete tıklayıp "Bununla kombin üret" dediğinde burası çalışır.
    @GetMapping("/{itemId}/generate-outfit")
    public ResponseEntity<java.util.List<ClothingItem>> generateOutfitWithAnchor(@PathVariable Long itemId) {
        java.util.List<ClothingItem> generatedOutfit = clothingItemService.generateOutfitFromAnchor(itemId);
        return ResponseEntity.ok(generatedOutfit);
    }
    // --- GARDIROP İSTATİSTİK ENDPOINT'İ ---
    // URL: GET /api/v1/clothes/{userId}/stats
    @GetMapping("/{userId}/stats")
    public ResponseEntity<WardrobeStatsDto> getWardrobeStats(@PathVariable Long userId) {
        WardrobeStatsDto stats = clothingItemService.getWardrobeStatistics(userId);
        return ResponseEntity.ok(stats);
    }

    // ---------------------------------------------------------------------------------------------------------
    // --- 🚀 YENİ: MOBİL UYGULAMA İÇİN DİREKT YÜKLEME KAPISI (KÖPRÜ) ---
    // URL: POST /api/v1/clothes/upload
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadItemImage(@RequestPart("file") MultipartFile file) {
        try {
            System.out.println("📱 Mobil uygulamadan yeni fotoğraf ulaştı! Boyut: " + (file.getSize() / 1024) + " KB");

            // NOT: MVP testini tamamlamak için şimdilik anında sahte bir (Mock) AI çıktısı URL'si dönüyoruz.
            // Sistemi test ettikten hemen sonra burayı senin o muazzam AiVisionService (Celery) yapına bağlayacağız.
            String mockProcessedImageUrl = "https://images.unsplash.com/photo-1515347619362-75fe80111eb9?w=400";

            // React Native'in beklediği "imageUrl" anahtarını dönüyoruz
            return ResponseEntity.ok(Map.of(
                    "message", "Yapay zeka işlemi başarılı!",
                    "imageUrl", mockProcessedImageUrl
            ));

        } catch (Exception e) {
            System.err.println("Yükleme sırasında hata: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Sunucu hatası: " + e.getMessage()));
        }
    }

    // URL: PUT /api/v1/clothes/{itemId}
    // Mobil uygulamadan gelen detayları kaydeder
    @PutMapping("/{itemId}")
    public ResponseEntity<ClothingItem> updateItemDetails(
            @PathVariable Long itemId,
            @RequestBody ClothingItem itemDetails) {

        ClothingItem updatedItem = clothingItemService.updateClothingItem(itemId, itemDetails);
        return ResponseEntity.ok(updatedItem);
    }

}
