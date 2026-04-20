package com.vestify.backend.domain.social.entity;

import com.vestify.backend.domain.user.entity.User;
import com.vestify.backend.domain.social.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// Sosyal ağ ve pazar yeri varsa, kullanıcıları uygulamaya geri çekecek bildirim sistemi şarttır. Bu tablo
// milyonlarca satıra çok hızlı ulaşır, o yüzden olabildiğince hafif tutulmalıdır.

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Bildirimin kime gideceği -> Alıcı
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    // Örn: NEW_FOLLOWER, OUTFIT_LIKED, ITEM_SOLD, AI_WEATHER_ALERT

    @Column(nullable = false)
    private String message; // "Kombinin 50 beğeni aldı!", "Yağmur yağacak, montunu al"

    @Column(name = "reference_id")
    private Long referenceId; // Tıklayınca nereye gidecek? (Satılan ürünün ID'si veya Kombin ID'si)

    @Column(name = "is_read")
    private Boolean isRead = false; // Okundu mu?

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}