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
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CapsuleService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Gemini API Anahtarını okuyoruz
    // Gemini 1.5 Flash Modeli -> (dakikada 15 istek, günde 1500 istek) yeterli MVP için
    // application.yaml üzerinden .env'den gelen API anahtarını alıyoruz
    @Value("${gemini.api-key}")
    private String geminiApiKey;

    public CapsuleService() { // Kendi object mapper nesnemizi oluşturuyoruz.
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper(); 
    }

    public CapsuleResponse generateSmartCapsule(CapsuleRequest request) {
        log.info("🔮 [CAPSULE ENGINE] Akıllı Bavul Üretimi Başladı. Kullanıcı: {}, Hedef: {}", request.getUserId(), request.getTarget());

        // 1. Veritabanından verileri topla (Şimdilik Mock/Simülasyon Verisi)
        String userWardrobeJson = getMockUserWardrobe(request.getUserId());
        String partnerCatalogJson = getMockPartnerCatalog();

        // 2. Gemini İçin Zenginleştirilmiş Tekil Prompt
        // Sistem Promptunu Hazırla (Katı kurallarla LLM'i bağlıyoruz)
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

        // 3. Gemini İstek Gövdesini Hazırla (JSON Mode Açık)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
        ));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("response_mime_type", "application/json"); // Gemini'yi JSON dönmeye zorla
        requestBody.put("generationConfig", generationConfig);

        try {
            String geminiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

            // API Çağrısı (Retry Mekanizmalı)
            Map response = webClient.post()
                    .uri(geminiEndpoint)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)))
                    .block();

            if (response != null && response.containsKey("candidates")) {
                List candidates = (List) response.get("candidates");
                Map firstCandidate = (Map) candidates.get(0);
                Map content = (Map) firstCandidate.get("content");
                List parts = (List) content.get("parts");
                String rawJsonOutput = (String) ((Map) parts.get(0)).get("text");

                log.info("✅ [CAPSULE ENGINE - GEMINI] Yapay Zeka Başarıyla JSON Üretti.");

                // Güvenlik: Gemini Markdown etiketleri koyarsa temizle
                rawJsonOutput = rawJsonOutput.replace("```json", "").replace("```", "").trim();

                return objectMapper.readValue(rawJsonOutput, CapsuleResponse.class);
            }

            throw new RuntimeException("Gemini'den geçerli bir yanıt dönmedi.");

        } catch (Exception e) {
            log.error("🚨 [CAPSULE ENGINE - GEMINI] Üretim Sırasında Hata Oluştu: {}", e.getMessage());
            throw new RuntimeException("Kapsül oluşturulamadı: " + e.getMessage());
        }
    }

    // --- MOCK DATA GENERATORS ---
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