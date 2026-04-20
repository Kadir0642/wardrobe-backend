package com.vestify.backend.service;

import java.util.Collections;
import com.vestify.backend.dto.OutfitFeedbackDto;
import com.vestify.backend.dto.AiScoreRequestDto;
import com.vestify.backend.dto.AiScoreResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceClient {

    //Backend Servisleri: Bir mikroservisin başka bir servise istek atması (Spring WebClient ile)
    private final WebClient aiWebClient;

    public AiScoreResponseDto scoreOutfit(AiScoreRequestDto requestDto) {
        log.info("Python AI servisine kombin skorlama isteği atılıyor... Kullanıcı: {}", requestDto.getUserId());

        try {
            // WebClient ile asenkron (non-blocking) POST isteği atıyoruz
            return aiWebClient.post()
                    .uri("/api/v1/ai/score-outfit")
                    .bodyValue(requestDto)
                    .retrieve()
                    .bodyToMono(AiScoreResponseDto.class)
                    .timeout(Duration.ofSeconds(3)) //  KRİTİK: Python 3 saniyede cevap vermezse bekleme, düşür
                    .block(); // Spring MVC içinde olduğumuz için sonucu senkrona çevirip bekliyoruz, ancak arka plandaki I/O asenkron çalışır.
        } catch (Exception e) {
            log.error("Python AI Servisi ile iletişim kurulamadı: {}", e.getMessage());
            // Eğer Python sunucusu çökerse sistemi kilitlememek için varsayılan bir onay dönüyoruz (Fallback Mechanism)
            AiScoreResponseDto fallbackResponse = new AiScoreResponseDto();  // fallbackResponse -> Yedek Yanıt
            fallbackResponse.setApproved(true);
            fallbackResponse.setScore(60.0);
            return fallbackResponse;
        }
    }

    public void sendFeedbackToAi(OutfitFeedbackDto feedbackDto) {
        log.info("RLHF Verisi Python'a fırlatılıyor...");

        try {
            // Python bizden bir liste (List<FeedbackLog>) bekliyor
            aiWebClient.post()
                    .uri("/api/v1/ai/train-rlhf")
                    .bodyValue(Collections.singletonList(feedbackDto)) // Tekli DTO'yu listeye sarıp gönderiyoruz
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(2))
                    .subscribe( // "Ateşle ve Unut" (Fire and Forget) mantığıyla çalışır. Java veriyi Python'a fırlatır ve cevabı beklemeden yoluna devam eder. Hızı asla yavaşlatmaz.
                            success -> log.info("Python RLHF Güncellemesi Başarılı: {}", success),
                            error -> log.error("Python RLHF Gönderim Hatası: {}", error.getMessage())
                    );
        } catch (Exception e) {
            log.error("RLHF Köprü Hatası: {}", e.getMessage());
        }
    }
}