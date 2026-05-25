package com.vestify.backend.domain.capsule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vestify.backend.domain.capsule.dto.CapsuleRequest;
import com.vestify.backend.domain.capsule.dto.CapsuleResponse;

//Gerçek dolap entity ve reposunu içeri aktarıyoruz
import com.vestify.backend.domain.wardrobe.entity.ClothingItem;
import com.vestify.backend.domain.wardrobe.enums.ItemStatus;
import com.vestify.backend.domain.wardrobe.repository.ClothingItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CapsuleService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Gerçek veritabanı bağlantımız
    private final ClothingItemRepository clothingItemRepository;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    // Dependency Injection ile Repository'i içeri alıyoruz
    public CapsuleService(ClothingItemRepository clothingItemRepository) {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
        this.clothingItemRepository = clothingItemRepository;
    }

    public CapsuleResponse generateSmartCapsule(CapsuleRequest request) {
        log.info("🔮 [CAPSULE ENGINE] Akıllı Bavul Üretimi Başladı. Kullanıcı: {}, Hedef: {}", request.getUserId(), request.getTarget());

        // Verileri hazırlıyoruz (Dolap canlı, Mağaza mock)
        // String olarak userId geliyor, Long'a çevirmemiz gerek
        Long userIdAsLong = Long.parseLong(request.getUserId());
        String userWardrobeJson = getUserWardrobeFromDatabase(userIdAsLong);
        String partnerCatalogJson = getPartnerCatalogMock();

        String prompt = String.format(
                "You are 'Vestify AI', an elite personal stylist and smart commerce assistant. " +
                        "Your goal is to curate a travel or event capsule wardrobe.\n\n" +
                        "CONTEXT:\n" +
                        "Mode: %s, Target: %s, Date: %s, Purpose: %s, Weather: %s.\n\n" +
                        "USER'S WARDROBE (Available items):\n%s\n\n" +
                        "PARTNER CATALOG (Upsell items):\n%s\n\n" +
                        "RULES:\n" +
                        "1. Create exactly 3 distinct outfits for this context.\n" +
                        "2. For each outfit, select 2 to 4 item IDs strictly from the USER'S WARDROBE.\n" +
                        "3. For EVERY outfit, select EXACTLY ONE matching item ID from the PARTNER CATALOG.\n" +
                        "4. Write a 1-sentence 'stylistPitch' in Turkish explaining why this partner item completes the look.\n" +
                        "5. OUTPUT ONLY VALID JSON. Do not use Markdown formatting. Just the raw JSON object matching this schema:\n" +
                        "{\n  \"capsuleTitle\": \"string\",\n  \"outfits\": [\n    {\n      \"outfitName\": \"string\",\n      \"userItems\": [\"string\"],\n      \"partnerUpsellItem\": \"string\",\n      \"stylistPitch\": \"string\"\n    }\n  ]\n}",
                request.getMode(), request.getTarget(), request.getDate(), request.getTripPurpose(), request.getTemperature(), userWardrobeJson, partnerCatalogJson
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
        ));

        try {

            // En stabil sürüm ve model tanımı
            // gemini-2.5-flash modeline geçiyoruz
            String geminiEndpoint = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

            Map response = webClient.post()
                    .uri(geminiEndpoint)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    // NİHAİ DEDEKTİF: Google hata dönerse Retry yapma, doğrudan hatayı yut ve ekrana kus!
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("🚨 [GEMINI API ERROR] Google Bizi Reddetti: {}", errorBody);
                                        return Mono.error(new RuntimeException("Gemini Hatası: " + errorBody));
                                    })
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response != null && response.containsKey("candidates")) {
                List candidates = (List) response.get("candidates");
                Map firstCandidate = (Map) candidates.get(0);
                Map content = (Map) firstCandidate.get("content");
                List parts = (List) content.get("parts");
                String rawJsonOutput = (String) ((Map) parts.get(0)).get("text");

                log.info("✅ [CAPSULE ENGINE - GEMINI] Yapay Zeka Başarıyla JSON Üretti.");
                rawJsonOutput = rawJsonOutput.replace("```json", "").replace("```", "").trim();
                return objectMapper.readValue(rawJsonOutput, CapsuleResponse.class);
            }

            throw new RuntimeException("Gemini'den geçerli bir yanıt dönmedi.");

        } catch (Exception e) {
            log.error("🚨 [CAPSULE ENGINE - GEMINI] Sistem Hatası: {}", e.getMessage());
            throw new RuntimeException("Kapsül oluşturulamadı: " + e.getMessage());
        }
    }

    // --- VERİ HAZIRLAMA METOTLARI ---

    // Gerçek Repository Bağlantısı
    private String getUserWardrobeFromDatabase(Long userId) {
        try {
            // Repodan silinmemiş (ARCHIVED veya DELETED olmayan), aktif kıyafetleri çekiyoruz
            // Şuan içinde soft Delete attıklarımızı çekmiyoruz
            List<ClothingItem> items = clothingItemRepository.findByUserIdAndStatusNot(userId, ItemStatus.DELETED); // Veya DELETED, enum yapına göre

            // Performans Optimizasyonu: Yapay zekanın sadece bilmesi gereken alanları seçiyoruz.
            // Bütün entity'i versek JSON şişer ve API token sınırı aşılır.
            // Bu, hem token maliyetini sıfıra yaklaştırır hem de hızı artırır.
            List<Map<String, Object>> optimizedList = items.stream().map(item -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", item.getId().toString()); // LLM'in anlaması için String'e çevirdik
                map.put("category", item.getCategory());
                map.put("subCategory", item.getSubCategory());
                map.put("name", item.getName());
                map.put("color", item.getColor());
                map.put("formality", item.getFormality());
                return map;
            }).collect(Collectors.toList());

            return objectMapper.writeValueAsString(optimizedList);
        } catch (Exception e) {
            log.error("🚨 Dolap verisi JSON'a dönüştürülemedi: {}", e.getMessage());
            return "[]"; // Hata durumunda boş dolap dön
        }
    }

    // Affiliate ( Satış Ortaklığı )
    // Bu, "Satış komisyonu (Upsell) işini böyle yapacağız"
    // 🚀 GÜNCELLEME : Yatırımcı Sunumu İçin Premium Mock Katalog
    private String getPartnerCatalogMock() {
        // İleride PartnerItemRepository'ye bağlayacağımız yer.
        // Şimdilik yatırımcıyı etkileyecek gerçek markalar ve ürünler ekliyoruz.
        return "[" +
                "{\"id\": \"partner_item_99\", \"brand\": \"Burberry\", \"name\": \"Klasik Bej Trençkot\", \"price\": \"28.499 TL\", \"category\": \"OUTERWEAR\"}," +
                "{\"id\": \"partner_item_100\", \"brand\": \"Prada\", \"name\": \"Siyah Deri Çapraz Çanta\", \"price\": \"35.999 TL\", \"category\": \"ACCESSORIES\"}," +
                "{\"id\": \"partner_item_101\", \"brand\": \"Nike\", \"name\": \"Air Force 1 '07 Beyaz\", \"price\": \"4.299 TL\", \"category\": \"SHOES\"}," +
                "{\"id\": \"partner_item_102\", \"brand\": \"Massimo Dutti\", \"name\": \"Kaşmir V Yaka Kazak\", \"price\": \"4.899 TL\", \"category\": \"TOPS\"}," +
                "{\"id\": \"partner_item_103\", \"brand\": \"Zara\", \"name\": \"Su İtici Şişme Mont\", \"price\": \"2.499 TL\", \"category\": \"OUTERWEAR\"}" +
                "]";
    }
}