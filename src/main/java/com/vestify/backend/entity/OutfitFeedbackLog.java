package com.vestify.backend.entity;

import com.vestify.backend.enums.FeedbackType;
import com.vestify.backend.enums.FeedbackReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "outfit_feedback_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutfitFeedbackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hangi kullanıcı bu geri bildirimi yaptı?
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ElementCollection
    @CollectionTable(name = "feedback_outfit_items", joinColumns = @JoinColumn(name = "log_id"))
    @Column(name = "item_id")
    private List<Long> outfitItemIds;
    // O an ekranda olan tüm kombinin eşya ID'leri (Örn: [12, 45, 8] - Tişört, Pantolon, Ayakkabı)

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false)
    private FeedbackType feedbackType;
    // Aksiyonun Tipi (Beğendi mi, Reddetti mi?)

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code")
    private FeedbackReason reasonCode;
    // Referans olarak çıkardığımız spesifik ret nedenleri

    @ElementCollection
    @CollectionTable(name = "feedback_target_items", joinColumns = @JoinColumn(name = "log_id"))
    @Column(name = "item_id")
    private List<Long> targetItemIds;
    // ÇOK KRİTİK: Eğer kullanıcı "Bu ikisini eşleştirme" veya "Bu kaban çok sıcak" derse,
    // faturayı tüm kombine değil, sadece seçtiği sorunlu eşyalara kesiyoruz.

    @Column(name = "weather_context")
    private String weatherContext;
    // Kullanıcının o anki hava durumu (Modelin mevsimselliği öğrenmesi için şart)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Veritabanına ilk kaydedildiğinde zamanı otomatik atar
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}