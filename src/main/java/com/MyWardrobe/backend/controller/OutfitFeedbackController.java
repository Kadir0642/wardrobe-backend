package com.MyWardrobe.backend.controller;

import com.MyWardrobe.backend.dto.OutfitFeedbackDto;
import com.MyWardrobe.backend.service.OutfitFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class OutfitFeedbackController {

    private final OutfitFeedbackService feedbackService;

    @PostMapping // Otomatik Dönüştürme (RequestBody): Spring, gelen JSON metnini alır ve sizin belirttiğiniz Java sınıfının alanlarıyla eşleştirerek bir nesne oluşturur.
    public ResponseEntity<String> submitFeedback(@RequestBody OutfitFeedbackDto dto) {
        try {
            feedbackService.saveFeedback(dto);
            return ResponseEntity.ok("Feedback başarıyla kaydedildi. AI modeli eğitilecek.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Feedback kaydedilirken hata oluştu: " + e.getMessage());
        }
    }
}