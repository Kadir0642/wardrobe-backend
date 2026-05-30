package com.vestify.backend.core.ai.service;

import com.vestify.backend.domain.outfit.dto.AiScoreRequestDto;
import com.vestify.backend.domain.outfit.dto.AiScoreResponseDto;
import com.vestify.backend.domain.outfit.dto.OutfitFeedbackDto;
import com.vestify.backend.domain.wardrobe.entity.ClothingItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// --- MAIN PURPOSE ---
// Java backendi ile Python AI tarafı arasındaki bir köprüdür.
// Bloklamayan yapısı sayesinde yüksek trafik altında bile uygulamanın kasılmasını engeller.

// --- Spring WebFlux (Reactive Programming) ---
// Reactive Programming, verilerin ve olayların sürekli bir akış (stream) olarak ele alındığı,
// bu akışlarda gerçekleşen değişimlerin sisteme otomatik olarak yayıldığı bir yazılım geliştirme paradigmasıdır.

// Asenkron ve Bloklanmayan Yapı: İşlemler birbirinin tamamlanmasını beklemez; sistem bir görevi yürütürken diğer talepleri de kabul etmeye devam eder.
// Veri Akışları (Data Streams): Her şey (tıklamalar, veritabanı sorguları, sensör verileri) zaman içinde akan bir dizi olay olarak görülür.
// Deklaratif Yaklaşım: "Nasıl yapmalı" yerine "ne sonuç isteniyor" üzerine odaklanılır. Karmaşık döngüler yerine basit ifade zincirleri (operatörler) kullanılır.
// Gözlemci (Observer) Deseni: Bir "Observable" (gözlemlenebilir akış) veri üretirken, ona abone olan "Observer" (gözlemci) bu verileri anlık olarak yakalar ve işler.


@Service // Bu sınıfın bir "Business Logic" (iş mantığı) katmanı olduğunu Spring'e bildirir
@RequiredArgsConstructor // Lombok kütüphanesinin bir mucizesidir. final olarak tanımladığın aiWebClient değişkeni için otomatik bir constructor (yapıcı metod) oluşturur. Dependency Injection'ı sağlar
@Slf4j // Loglama yapmanı sağlar. Konsola yazdığın log.info veya log.error çıktıları buradan gelir.
public class AiIntegrationService {

    // bu client non-blocking (bloklamayan) çalışır.
    // Yani Python tarafındaki AI servisi yavaşsa, Java uygulaman o sırada başka işler yapmaya devam edebilir.
    private final WebClient aiWebClient; // Tek bir Asenkron Motor!

    // Docker ağı içindeki Python API servisinin merkezi adresi
    private final String PYTHON_BASE_URL = "http://python-ai-api:8000";

    // 1. ASENKRON KOMBİN SKORLAMA (Stil DNA Kontrolü)
    public Mono<AiScoreResponseDto> scoreOutfitAsync(AiScoreRequestDto requestDto) {
        log.info("Python AI: Kombin skorlama isteği (Kullanıcı: {})", requestDto.getUserId());

        // Pydantic uyuşmazlığı riski için güvenli dönüştürme yapıyoruz
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", requestDto.getUserId());
        payload.put("weather_context", requestDto.getWeatherContext());
        payload.put("items", requestDto.getItems());

        WebClient directClient = WebClient.create(PYTHON_BASE_URL);

        return directClient.post()
                .uri("/api/v1/ai/score-outfit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(AiScoreResponseDto.class)
                .timeout(Duration.ofSeconds(5)) // Ağ gecikmelerine karşı 5 saniyeye esnetildi
                .onErrorResume(e -> { // "Fallback" (Yedek Plan) kısmıdır. AI servisi çökerse veya gecikirse sistem hata vermez; kullanıcıya hissettirmeden varsayılan (60.0 puan gibi) bir sonuç döner.
                    log.error("AI Sunucusu Çöktü/Gecikti. Fallback (Yedek) devreye giriyor. Hata: {}", e.getMessage());
                    return Mono.just(new AiScoreResponseDto(requestDto.getUserId(), 60.0, true, "Fallback Onayı"));
                });
    }

    // 2. ATEŞLE VE UNUT RLHF (Stil DNA'sını Eğiten Katman)
    public void sendFeedbackFireAndForget(OutfitFeedbackDto feedbackDto) {
        log.info("Python'a RLHF Verisi fırlatılıyor...");

        // 🚀 CRITICAL FIX: Java camelCase alanlarını Python'un beklediği snake_case formatına eşliyoruz
        Map<String, Object> logPayload = new HashMap<>();
        logPayload.put("user_id", feedbackDto.getUserId());
        logPayload.put("outfit_item_ids", feedbackDto.getOutfitItemIds());
        logPayload.put("feedback_type", feedbackDto.getFeedbackType());
        logPayload.put("reason_code", feedbackDto.getReasonCode() != null ? feedbackDto.getReasonCode() : "NONE");
        logPayload.put("target_item_ids", feedbackDto.getTargetItemIds());

        // Python List[FeedbackLog] beklediği için toplu liste formatına sarmalıyoruz
        List<Map<String, Object>> requestBody = Collections.singletonList(logPayload);

        WebClient directClient = WebClient.create(PYTHON_BASE_URL);

        directClient.post()
                .uri("/api/v1/ai/train-rlhf")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3))
                .subscribe( // Yani Java isteği gönderir ve cevabı beklemeden yoluna devam eder. Cevap geldiğinde (başarılı veya hatalı) sadece log atar.
                        success -> log.info("🧬 [RLHF SUCCESS] Geri bildirim Python DNA motoruna başarıyla işlendi."),
                        error -> log.error("🚨 [RLHF ERROR] Geri bildirim fırlatılırken AI sunucusu hata döndü: {}", error.getMessage())
                );
    }

    // 3. YENİ KATMAN: ASENKRON KIYAFET VEKTÖRLEŞTİRME (Kıyafet Gardıroba Girerken Tetiklenir)
    public void vectorizeItemAsync(Long userId, ClothingItem item) {
        log.info("Kıyafet ID {} için 384 boyutlu vektör üretimi başlatılıyor...", item.getId());

        // Python'daki ItemVectorRequest modeline milimetrik uyumlu snake_case harita
        Map<String, Object> payload = new HashMap<>();
        payload.put("item_id", item.getId());
        payload.put("user_id", userId);

        Map<String, String> tags = new HashMap<>();
        tags.put("category", item.getCategory());
        tags.put("sub_category", item.getSubCategory());
        tags.put("color", item.getColor());
        tags.put("formality", item.getFormality());
        tags.put("season", item.getSeason() != null ? item.getSeason().name().toLowerCase() : "unknown");

        payload.put("tags", tags);

        WebClient directClient = WebClient.create(PYTHON_BASE_URL);

        directClient.post()
                .uri("/api/v1/ai/vectorize-item")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(5))
                .subscribe(
                        success -> log.info("✅ [VEKTÖR SUCCESS] Kıyafet ID {} başarıyla Supabase pgvector hafızasına nakşedildi.", item.getId()),
                        error -> log.error("🚨 [VEKTÖR ERROR] Kıyafet ID {} vektörleştirilirken Python hatası: {}", item.getId(), error.getMessage())
                );
    }

    // 4. ASENKRON GÖRSEL İŞLEME (Vision - Jilet Kesim Motoru)
    public Mono<String> extractClothesAsync(MultipartFile file, String mode) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() { return file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg"; }
        });
        body.add("mode", mode);

        WebClient directClient = WebClient.create(PYTHON_BASE_URL);

        return directClient.post()
                .uri("/api/v1/vision/extract-async")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(Map.class)
                .map(res -> (String) res.get("task_id"));
    }

    // 5. GÖRSEL İŞLEME DURUM SORGULAMA (Task Status Kontrolü)
    public Mono<Map<String, Object>> getAiExtractionStatus(String taskId) {
        log.info("Python AI: Task durumu ve sonuçları sorgulanıyor... TaskID: {}", taskId);

        WebClient directClient = WebClient.create(PYTHON_BASE_URL);

        return directClient.get()
                .uri("/api/v1/vision/status/{taskId}", taskId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response);
    }

    // 6. AI İÇERİK DENETİMİ (NSFW / Güvenlik Filtresi)
    public Mono<Boolean> checkContentModerationAsync(String imageUrl) {
        log.info("AI İçerik Denetimi başlatıldı. URL: {}", imageUrl);
        return aiWebClient.post()
                .uri("/api/v1/ai/moderate-content")
                .bodyValue(Collections.singletonMap("image_url", imageUrl))
                .retrieve()
                .bodyToMono(Boolean.class);
    }
}