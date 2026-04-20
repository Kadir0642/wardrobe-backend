package com.vestify.backend.domain.outfit.entity;

import com.vestify.backend.domain.outfit.enums.FeedbackType;
import com.vestify.backend.domain.outfit.enums.FeedbackReason;
import com.vestify.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "outfit_feedback_logs")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutfitFeedbackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referans Bütünlüğü (Foreign Key) Sağlandı
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ElementCollection // ekstra bir tablo (küçük bir tablo) oluşturur.
    @CollectionTable(name = "feedback_outfit_items", joinColumns = @JoinColumn(name = "log_id"))
    @Column(name = "item_id")  //  Bu tablo, ana tablodaki log_id (Foreign Key) ile item_id listesini birbirine bağlar.
    private List<Long> outfitItemIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false)
    private FeedbackType feedbackType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code")
    private FeedbackReason reasonCode;

    @ElementCollection
    @CollectionTable(name = "feedback_target_items", joinColumns = @JoinColumn(name = "log_id"))
    @Column(name = "item_id")
    private List<Long> targetItemIds;

    @Column(name = "weather_context")
    private String weatherContext;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}