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


// Bu servis, projenin "Öğrenen Mekanizma" merkezidir.
// Kullanıcının bir kombini beğenip beğenmediğini hem veritabanına kaydeder
// hem de bu veriyi AI modelini eğitmek (RLHF) üzere Python tarafına gönderir.

//  1) Kullanıcıyı bul.
// 2) Geri bildirimi veritabanına işle (Bu kısım öncelikli ve zorunlu).
// 3) AI'yı eğitmek için veriyi asenkron olarak "fırlat" (Bu kısım ikincil ve hata olsa da işlem devam eder).

@Service
@RequiredArgsConstructor
@Slf4j // Simple Logging Facade for Java (Java için Basit Loglama Cephesi) | loglama nesnesini elle tanımlama zahmetinden kurtaran bir anotasyondur.
public class OutfitFeedbackService {

    private final OutfitFeedbackRepository feedbackRepository;  // Geri bildirimleri veritabanına (PostgreSQL/MySQL vb.) kalıcı olarak yazmak için kullanılır.
    private final UserRepository userRepository; // Geri bildirimi veren kullanıcının gerçekten sistemde olup olmadığını kontrol etmek için çağrılır.
    private final AiIntegrationService aiIntegrationService; // Veriyi asenkron olarak Python'a "fırlatmak" için kullanılır.

    @Transactional //"Ya hep ya hiç!" demektir. Eğer metodun içinde veritabanı ile ilgili bir hata oluşursa (örneğin kayıt sırasında elektrik kesildi veya DB çöktü), yapılan tüm işlemler geri alınır (Rollback). Böylece veritabanında yarım yamalak veya tutarsız veri kalmaz
    public void saveFeedback(OutfitFeedbackDto dto) { // Dışarıdan gelen dto içindeki userId ile veritabanına bakılır.
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        // Builder Pattern ile Log Kaydı Oluşturma
        // Builder -> Uzun constructor (yapıcı metod) listeleri yerine,
        // alanları tek tek isimlendirerek setlememizi sağlayan okunabilir bir tasarım desenidir.
        OutfitFeedbackLog logEntity = OutfitFeedbackLog.builder() // DTO'dan (gelen veri paketi) DB'ye kaydedilecek olan Entity (tablo karşılığı) nesnesine dönüştürüyoruz.
                .user(user) // İlişki sağlandı
                .outfitItemIds(dto.getOutfitItemIds())
                .feedbackType(dto.getFeedbackType())
                .reasonCode(dto.getReasonCode())
                .targetItemIds(dto.getTargetItemIds())
                .weatherContext(dto.getWeatherContext())
                .build();

        // Hazırlanan geri bildirim nesnesi veritabanına yazılır.
        // Kullanıcının hangi hava durumunda (weatherContext), hangi parçalar için (outfitItemIds)
        // ne tür bir geri bildirim verdiği artık kalıcı hale gelmiş olur
        feedbackRepository.save(logEntity);
        log.info("✅ Geri Bildirim DB'ye Kaydedildi (Sebep: {})", dto.getReasonCode());

        //  --- ATEŞLE VE UNUT (KAFKA MİMARİSİNE HAZIRLIK) ---
        // Python tarafındaki AI servisi o an kapalıysa veya bir hata verirse, kullanıcının işlemi yarıda kalsın istemiyoruz.
        // Bu yüzden AI kısmını "kritik olmayan hata" olarak ele alıyoruz. DB'ye kayıt başarılıysa kullanıcıya "Başarılı" döneriz
        // AI tarafındaki hatayı sadece loglara yazarız.
        //sendFeedbackFireAndForget zaten asenkron olduğu için bu işlem sistemini yavaşlatmaz.
        try {
            aiIntegrationService.sendFeedbackFireAndForget(dto);
        } catch (Exception e) {
            log.error("AI'a RLHF fırlatılırken kritik olmayan hata oluştu: {}", e.getMessage());
        }
    }
}