package com.MyWardrobe.backend.service;

import com.MyWardrobe.backend.dto.OutfitFeedbackDto;
import com.MyWardrobe.backend.entity.OutfitFeedbackLog;
import com.MyWardrobe.backend.repository.OutfitFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // @Slf4j // Bu annotation arka planda 'private static final Logger log...' satırını oluşturur
@Slf4j    // Sınıf için otomatik olarak statik final bir SLF4J günlükleyici örneği oluşturan bir Lombok ek açıklaması
public class OutfitFeedbackService { // Log, program çalışırken arkada neler yaptığını kaydeden bir olay defteri
 // Decoupling (Bağımsızlık): Kodunu loglama kütüphanesine esir etmezsin.
 //Standart Dil: Herkesin anlayacağı ortak bir "hata yazma" protokolü kullanırsın.
 //Performans: Log mesajlarını oluştururken ({} kullanarak) sadece gerekiyorsa işlem yapar, böylece gereksiz string birleştirmeleriyle sistemi yormazsın
    private final OutfitFeedbackRepository feedbackRepository;

    public void saveFeedback(OutfitFeedbackDto dto) {
        // DTO'dan gelen veriyi Entity builder ile inşa ediyoruz
        OutfitFeedbackLog logEntity = OutfitFeedbackLog.builder()
                .userId(dto.getUserId())
                .outfitItemIds(dto.getOutfitItemIds())
                .feedbackType(dto.getFeedbackType())
                .reasonCode(dto.getReasonCode())
                .targetItemIds(dto.getTargetItemIds())
                .weatherContext(dto.getWeatherContext())
                .build();

        feedbackRepository.save(logEntity);
        log.info("AI Geri Bildirimi Kaydedildi - Kullanıcı ID: {}, Tip: {}", dto.getUserId(), dto.getFeedbackType());
    }
}