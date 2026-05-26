package com.vestify.backend.domain.capsule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vestify.backend.domain.capsule.dto.CapsuleRequest;
import com.vestify.backend.domain.capsule.dto.CapsuleResponse;

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
    private final ClothingItemRepository clothingItemRepository;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    public CapsuleService(ClothingItemRepository clothingItemRepository) {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
        this.clothingItemRepository = clothingItemRepository;
    }

    public CapsuleResponse generateSmartCapsule(CapsuleRequest request) {
        log.info("🔮 [VESTIFY AI] Motor Başladı. Mod: {}, Context: {}", request.getMode(), request.getMagicContext());

        String userWardrobeJson = getUserWardrobeFromDatabase(request.getUserId());
        String partnerCatalogJson = fetchAffiliateCatalog(); // Mimari olarak ayrılmış sahte servis

        int requestedOutfits = (request.getTotalOutfits() != null && request.getTotalOutfits() > 0)
                ? request.getTotalOutfits() : 3;

        // 🚀 DİNAMİK PROMPT MÜHENDİSLİĞİ: Mod'a göre Gemini'ye farklı emir veriyoruz
        String prompt = buildMasterStylistPrompt(
                request.getMode(),
                request.getMagicContext(),
                request.getWeatherContext(),
                requestedOutfits,
                userWardrobeJson,
                partnerCatalogJson
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.15); // Yaratıcılık düşük, keskin doğruluk yüksek
        requestBody.put("generationConfig", generationConfig);

        try {
            String geminiEndpoint = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

            Map response = webClient.post()
                    .uri(geminiEndpoint)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("🚨 [GEMINI API ERROR] Google Reddetti: {}", errorBody);
                                        return Mono.error(new RuntimeException("Gemini Hatası: " + errorBody));
                                    })
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(90))
                    .block();

            if (response != null && response.containsKey("candidates")) {
                List candidates = (List) response.get("candidates");
                Map firstCandidate = (Map) candidates.get(0);
                Map content = (Map) firstCandidate.get("content");
                List parts = (List) content.get("parts");
                String rawJsonOutput = (String) ((Map) parts.get(0)).get("text");

                log.info("✅ [VESTIFY AI] JSON Başarıyla Üretildi.");
                rawJsonOutput = rawJsonOutput.replace("```json", "").replace("```", "").trim();
                return objectMapper.readValue(rawJsonOutput, CapsuleResponse.class);
            }

            throw new RuntimeException("Gemini'den geçerli bir yanıt dönmedi.");

        } catch (Exception e) {
            log.error("🚨 [VESTIFY AI] Sistem Hatası: {}", e.getMessage());
            throw new RuntimeException("AI Üretim Hatası: " + e.getMessage());
        }
    }

    // --- PROMPT MÜHENDİSLİĞİ (MODE BAĞIMLI) ---
    private String buildMasterStylistPrompt(String mode, String context, String weatherContext, int outfitCount, String wardrobeJson, String partnerJson) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are 'Vestify AI', an elite personal fashion stylist and architect of luxury aesthetics.\n\n");
        sb.append("USER CONTEXT: ").append(context).append("\n");
        sb.append("WEATHER CONTEXT: ").append(weatherContext).append("\n\n");
        sb.append("USER'S WARDROBE:\n").append(wardrobeJson).append("\n\n");
        sb.append("PARTNER CATALOG:\n").append(partnerJson).append("\n\n");

        if ("EVENT".equalsIgnoreCase(mode)) {
            sb.append("TASK: The user is attending a specific event. Create EXACTLY 3 distinct outfit alternatives (e.g., 'Edgy', 'Classic', 'Modern') using ONLY items from the USER'S WARDROBE.\n");
        } else {
            sb.append("TASK: The user is traveling. Create a 'Core Capsule' wardrobe using realistic items from the USER'S WARDROBE. Then, create EXACTLY ").append(outfitCount).append(" distinct daily outfits using ONLY those core items.\n");
        }

        sb.append("UPSELL STRATEGY: For EACH outfit, select exactly ONE logical missing item from the PARTNER CATALOG to elevate the look.\n");
        sb.append("Write a 1-sentence 'stylistPitch' in Turkish explaining why this partner item makes the outfit perfect.\n\n");

        sb.append("JSON OUTPUT SCHEMA (STRICTLY ADHERE):\n");
        sb.append("{\n");
        sb.append("  \"capsuleTitle\": \"string\",\n");
        sb.append("  \"coreCapsuleItemIds\": [\"string\"],\n");
        sb.append("  \"outfits\": [\n");
        sb.append("    {\n");
        sb.append("      \"outfitName\": \"string\",\n");
        sb.append("      \"userItems\": [\"string\"],\n");
        sb.append("      \"partnerUpsellItem\": \"string\",\n");
        sb.append("      \"stylistPitch\": \"string\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}");

        return sb.toString();
    }

    // --- VERİ HAZIRLAMA METOTLARI ---
    private String getUserWardrobeFromDatabase(Long userId) {
        try {
            List<ClothingItem> items = clothingItemRepository.findByUserIdAndStatusNot(userId, ItemStatus.DELETED);

            List<Map<String, Object>> optimizedList = items.stream().map(item -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", item.getId().toString());
                map.put("category", item.getCategory());
                map.put("subCategory", item.getSubCategory());
                map.put("color", item.getColor());
                map.put("formality", item.getFormality());
                return map;
            }).collect(Collectors.toList());

            return objectMapper.writeValueAsString(optimizedList);
        } catch (Exception e) {
            log.error("🚨 Dolap verisi JSON'a dönüştürülemedi: {}", e.getMessage());
            return "[]";
        }
    }

    // 🚀 MİMARİ VİZYON: Affiliate (Satış Ortaklığı) Simülasyon Servisi
    // Hoca veya yatırımcı sorduğunda: "Şu an Affiliate Integration Service katmanımız mock çalışıyor,
    // yarın Trendyol API key'i aldığımızda sadece bu metodu bir REST Call ile değiştireceğiz" diyeceksin.
    private String fetchAffiliateCatalog() {
        List<Map<String, String>> mockExternalApiData = List.of(
                Map.of("id", "pt_99", "brand", "Burberry", "name", "Klasik Bej Trençkot", "category", "OUTERWEAR"),
                Map.of("id", "pt_100", "brand", "Prada", "name", "Siyah Deri Loafer", "category", "FOOTWEAR"),
                Map.of("id", "pt_101", "brand", "Rolex", "name", "Submariner Çelik Saat", "category", "ACCESSORIES"),
                Map.of("id", "pt_102", "brand", "Massimo Dutti", "name", "İpek Şal", "category", "ACCESSORIES"),
                Map.of("id", "pt_103", "brand", "Hugo Boss", "name", "Lacivert Blazer", "category", "OUTERWEAR")
        );

        try {
            return objectMapper.writeValueAsString(mockExternalApiData);
        } catch (Exception e) {
            return "[]";
        }
    }
}