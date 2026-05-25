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

        // Güvenlik önlemi (Eğer Frontend'den days gelmezse veya 0 gelirse varsayılan 3 yap)
        int outfitCount = (request.getDays() != null && request.getDays() > 0) ? request.getDays() : 3;

        String prompt = String.format(
                "You are 'Vestify AI', an elite personal stylist. " +
                        "The user is going on a trip.\n\n" +
                        "CONTEXT:\n" +
                        "Mode: %s, Target: %s, Dates: %s, Duration: %d days, Purpose: %s, Weather: %s.\n\n" +
                        "USER'S WARDROBE:\n%s\n\n" +
                        "PARTNER CATALOG:\n%s\n\n" +
                        "CRITICAL INSTRUCTIONS (READ CAREFULLY):\n" +
                        "Phase 1 - The Core Capsule:\n" +
                        "First, analyze the trip duration (%d days). Create a 'Core Capsule' by selecting a realistic number of items from the USER'S WARDROBE. Do NOT invent IDs.\n\n" +
                        "Phase 2 - The Outfits & Upsell:\n" +
                        "Create EXACTLY %d distinct outfits (one for each day). IMPORTANT: You must ONLY use items selected in Phase 1 for these outfits.\n" +
                        "For EACH outfit, add exactly ONE matching item from the PARTNER CATALOG to fill a logical gap.\n" +
                        "Write a 1-sentence 'stylistPitch' in Turkish explaining why this partner item elevates the look.\n\n" +
                        "JSON OUTPUT SCHEMA (STRICTLY ADHERE):\n" +
                        "{\n" +
                        "  \"capsuleTitle\": \"string\",\n" +
                        "  \"coreCapsuleItemIds\": [\"string\"],\n" +
                        "  \"outfits\": [\n" +
                        "    {\n" +
                        "      \"outfitName\": \"string\",\n" +
                        "      \"userItems\": [\"string\"],\n" +
                        "      \"partnerUpsellItem\": \"string\",\n" +
                        "      \"stylistPitch\": \"string\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                // outfitCount parametresini formata ekledik
                request.getMode(), request.getTarget(), request.getDate(), outfitCount, request.getTripPurpose(), request.getTemperature(), userWardrobeJson, partnerCatalogJson, outfitCount, outfitCount
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
        ));

        // AI'ın yaratıcılığını kapatıyoruz ki kendi kendine ID uydurmasın (Halüsinasyon engeli)
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
        requestBody.put("generationConfig", generationConfig);

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