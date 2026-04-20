package com.vestify.backend.domain.outfit.service;

import com.vestify.backend.core.ai.service.AiIntegrationService;
import com.vestify.backend.domain.outfit.dto.OutfitFeedbackDto;
import com.vestify.backend.domain.outfit.entity.OutfitFeedbackLog;
import com.vestify.backend.domain.outfit.repository.OutfitFeedbackRepository;
import com.vestify.backend.domain.user.entity.User;
import com.vestify.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutfitFeedbackService {

    private final OutfitFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final AiIntegrationService aiIntegrationService; // YENİ KABLO!

    @Transactional
    public void saveFeedback(OutfitFeedbackDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        OutfitFeedbackLog logEntity = OutfitFeedbackLog.builder()
                .user(user) // İlişki sağlandı
                .outfitItemIds(dto.getOutfitItemIds())
                .feedbackType(dto.getFeedbackType())
                .reasonCode(dto.getReasonCode())
                .targetItemIds(dto.getTargetItemIds())
                .weatherContext(dto.getWeatherContext())
                .build();

        feedbackRepository.save(logEntity);
        log.info("✅ Geri Bildirim DB'ye Kaydedildi (Sebep: {})", dto.getReasonCode());

        // 🚀 ATEŞLE VE UNUT (KAFKA MİMARİSİNE HAZIRLIK)
        try {
            aiIntegrationService.sendFeedbackFireAndForget(dto);
        } catch (Exception e) {
            log.error("AI'a RLHF fırlatılırken kritik olmayan hata oluştu: {}", e.getMessage());
        }
    }
}