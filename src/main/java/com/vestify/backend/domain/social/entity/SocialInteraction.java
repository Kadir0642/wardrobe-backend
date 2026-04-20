package com.vestify.backend.domain.social.entity;

import com.vestify.backend.domain.social.enums.InteractionType;
import com.vestify.backend.domain.social.enums.TargetType;
import com.vestify.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


// Mimari Not: Burada Polymorphic Design (Çok Biçimli Tasarım) kullanıyoruz.
// Yani tek bir tablo hem kıyafetlere (ClothingItem)
// hem de kombinlere (Outfit) atılan beğenileri tutacak

@Entity
@Table(
        name = "social_interactions",
        // Aynı kullanıcı bir gönderiyi iki kere beğenemez! Veritabanı seviyesinde kısıtlıyoruz (engelliyoruz) .
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "target_id", "target_type", "interaction_type"})
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Aksiyonu yapan kişi

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false)
    private InteractionType interactionType; // LIKE (Beğeni), SAVE (Kaydetme)

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType; // OUTFIT, CLOTHING_ITEM

    @Column(name = "target_id", nullable = false)
    private Long targetId; // Neyin beğenildiği (Outfit ID'si veya Item ID'si)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}