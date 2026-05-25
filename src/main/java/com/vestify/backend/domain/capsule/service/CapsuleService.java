package com.vestify.backend.domain.capsule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vestify.backend.domain.capsule.dto.CapsuleRequest;
import com.vestify.backend.domain.capsule.dto.CapsuleResponse;
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

@Slf4j
@Service
public class CapsuleService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    public CapsuleService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public CapsuleResponse generateSmartCapsule(CapsuleRequest request) {
        log.info("🔮 [CAPSULE ENGINE] Akıllı Bavul Üretimi Başladı. Kullanıcı: {}, Hedef: {}", request.getUserId(), request.getTarget());

        String userWardrobeJson = getMockUserWardrobe(request.getUserId());
        String partnerCatalogJson = getMockPartnerCatalog();

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
            // 🚀 DÜZELTME: Silinmiş olan 1.5 modeli yerine, güncel gemini-2.5-flash modeline geçiyoruz
String geminiEndpoint = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

            Map response = webClient.post()
                    .uri(geminiEndpoint)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    // 🚀 NİHAİ DEDEKTİF: Google hata dönerse Retry yapma, doğrudan hatayı yut ve ekrana kus!
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("🚨 [GEMINI API ERROR] Google Bizi Reddetti: {}", errorBody);
                                        return Mono.error(new RuntimeException("Gemini Hatası: " + errorBody));
                                    })
                    )
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    // Retry'ı bilerek kapattım ki hatayı anında görelim.
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

    private String getMockUserWardrobe(String userId) {
        return "[ {\"id\": \"user_item_1\", \"category\": \"TOPS\", \"name\": \"Siyah Slim-Fit Tişört\"}," +
               "  {\"id\": \"user_item_2\", \"category\": \"BOTTOMS\", \"name\": \"Gri Kumaş Pantolon\"}," +
               "  {\"id\": \"user_item_3\", \"category\": \"OUTERWEAR\", \"name\": \"Deri Ceket\"} ]";
    }

    private String getMockPartnerCatalog() {
        return "[ {\"id\": \"partner_item_99\", \"brand\": \"Zara\", \"name\": \"Bej Trençkot\", \"price\": \"2499 TL\"}," +
               "  {\"id\": \"partner_item_100\", \"brand\": \"Massimo Dutti\", \"name\": \"İpek Şal\", \"price\": \"899 TL\"} ]";
    }
}