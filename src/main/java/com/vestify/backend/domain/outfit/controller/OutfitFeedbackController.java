package com.vestify.backend.domain.outfit.controller;

import com.vestify.backend.domain.outfit.dto.OutfitFeedbackDto;
import com.vestify.backend.domain.outfit.service.OutfitFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// "Kullanıcı Deneyimi ve Sürekli Öğrenme" kapısıdır.
// Kullanıcının kombinler hakkındaki fikirlerini toplar.
// Buradaki en kritik, standart bir "kaydedildi" cevabı yerine asenkron süreci vurgulayan bir yapı kullanılmış olmasıdır.

@RestController
@RequestMapping("/api/v1/feedback") // Geri bildirimler için özel, temiz bir yol
@RequiredArgsConstructor
public class OutfitFeedbackController {

    private final OutfitFeedbackService feedbackService;

    // yani direk  " POST /api/v1/feedback " kullanır
    @PostMapping // URL'nin sonuna ek bir takı almadan (direkt ana yola) POST isteği atılmasını sağlar.
    public ResponseEntity<?> submitFeedback(@RequestBody OutfitFeedbackDto dto) {
        feedbackService.saveFeedback(dto);

        // 202 ACCEPTED: İşlemi kuyruğa aldık mesajı.  |  Kullanıcıya (mobil uygulamaya) anında cevap dönerek arayüzün donmasını engeller.
        return ResponseEntity.accepted().body(Map.of(
                "status", "Accepted",
                "message", "Geri bildirim işleme alındı. AI modeli asenkron olarak eğitiliyor."
        ));
    }
}