package com.vestify.backend.core.ai.service;

import com.vestify.backend.domain.outfit.dto.AiScoreRequestDto;
import com.vestify.backend.domain.outfit.dto.AiScoreResponseDto;
import com.vestify.backend.domain.outfit.dto.OutfitFeedbackDto;
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
@RequiredArgsConstructor  // Lombok kütüphanesinin bir mucizesidir. final olarak tanımladığın aiWebClient değişkeni için otomatik bir constructor (yapıcı metod) oluşturur. Dependency Injection'ı sağlar
@Slf4j // Loglama yapmanı sağlar. Konsola yazdığın log.info veya log.error çıktıları buradan gelir.
public class AiIntegrationService {

    // bu client non-blocking (bloklamayan) çalışır.
    // Yani Python tarafındaki AI servisi yavaşsa, Java uygulaman o sırada başka işler yapmaya devam edebilir.
    private final WebClient aiWebClient; // Tek bir Asenkron Motor!

    // 1. TAMAMEN ASENKRON SKORLAMA (Mono Dönüyor, sistemi kitlemiyor!)
    // WebClient ile istek atarken kullandığımız standart "zincirleme" -> Parametre yapısı.
    // retrieve() dediğin an Java, Python tarafındaki AI servisine kapıyı çalar ve "Cevabı (response) yakala ve bana getir" emridir
    // bodyValue  -> Paketin içine bu veriyi koy
    // bodyValue ile paketi hazırlarsın, retrieve ile gönderirsin, bodyToMono ile gelen cevabı okursun.
    public Mono<AiScoreResponseDto> scoreOutfitAsync(AiScoreRequestDto requestDto) { // sıfır veya bir sonuç dönecek olan asenkron bir kutudur.
        log.info("Python AI: Kombin skorlama isteği (Kullanıcı: {})", requestDto.getUserId()); // uri -> İsteğin gideceği endpoint |
        return aiWebClient.post()
                .uri("/api/v1/ai/score-outfit")
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(AiScoreResponseDto.class)
                .timeout(Duration.ofSeconds(3))  // AI servisi 3 saniye içinde cevap vermezse "vaktim doldu" der.
                .onErrorResume(e -> { // "Fallback" (Yedek Plan) kısmıdır. AI servisi çökerse veya gecikirse sistem hata vermez; kullanıcıya hissettirmeden varsayılan (60.0 puan gibi) bir sonuç döner.
                    log.error("AI Sunucusu Çöktü/Gecikti. Fallback (Yedek) devreye giriyor. Hata: {}", e.getMessage());
                    return Mono.just(new AiScoreResponseDto(requestDto.getUserId(), 60.0, true, "Fallback Onayı"));
                });
    }

    // 2. KAFKA'YA HAZIRLIK: Ateşle ve Unut RLHF
    // İleride bu kodu silip template.send("rlhf-topic", feedbackDto) yazacağız!
    public void sendFeedbackFireAndForget(OutfitFeedbackDto feedbackDto) {
        log.info("Python'a RLHF Verisi fırlatılıyor...");
        aiWebClient.post()
                .uri("/api/v1/ai/train-rlhf")
                .bodyValue(Collections.singletonList(feedbackDto))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(2))
                .subscribe( // Yani Java isteği gönderir ve cevabı beklemeden yoluna devam eder. Cevap geldiğinde (başarılı veya hatalı) sadece log atar.
                        success -> log.info("RLHF Başarılı."),
                        error -> log.error("RLHF Hatası: {}", error.getMessage())
                );
    }

    // 3. ASENKRON GÖRSEL İŞLEME (Vision)
    // Bir görsel içinden kıyafetleri ayıklayan (segmentation) kısımdır.
    public Mono<String> extractClothesAsync(MultipartFile file, String mode) throws IOException { // MultipartFile: Kullanıcının yüklediği ham dosya.
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) { // ByteArrayResource: Dosyayı ağ üzerinden gönderilebilecek bir veri paketine dönüştürür.
            @Override // getFilename kısmını override ederek dosya isminin kaybolmamasını sağlar.
            public String getFilename() { return file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg"; }
        });
        body.add("mode", mode);

        return aiWebClient.post()
                .uri("http://localhost:8000/api/v1/vision/extract-async")
                .contentType(MediaType.MULTIPART_FORM_DATA) // MULTIPART_FORM_DATA -> Standart bir JSON değil, içinde dosya olan bir form gönderdiğimizi belirtir.
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(Map.class)
                .map(res -> (String) res.get("task_id"));// task_id: Bu metod doğrudan sonucu değil, bir Task ID döner.
                                                              // Çünkü görsel işleme uzun sürer. Java "ben işi AI'ya verdim, takip numaran bu" der
    }

    // AI'dan uygunsuz içerik (uygunsuz içerik / NSFW) taraması yapma kısmıdır.
    public Mono<Boolean> checkContentModerationAsync(String imageUrl) {
        log.info("AI İçerik Denetimi başlatıldı. URL: {}", imageUrl);
        return aiWebClient.post()
                .uri("/api/v1/ai/moderate-content")
                .bodyValue(Collections.singletonMap("image_url", imageUrl))
                .retrieve()
                .bodyToMono(Boolean.class); // Boolean.class: AI'dan " true = temiz, false = uygunsuz " yanıtını bekler.
    // bodyValue -> Tek bir alanı olan hızlıca bir JSON oluşturur:
    }
}