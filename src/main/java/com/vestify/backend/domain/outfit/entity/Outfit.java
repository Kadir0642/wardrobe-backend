package com.vestify.backend.domain.outfit.entity;

import com.vestify.backend.domain.user.entity.User;
import com.vestify.backend.domain.wardrobe.entity.ClothingItem;
import com.vestify.backend.domain.outfit.enums.ModerationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name="outfits")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Outfit {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String name;

    // Kullanıcı ile LAZY (Tembel) ilişki - N+1 sorunu çözüldü (EAGER) [Sadece lazım olan veriler geliyor artık]
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;
    // Yüklenen her fotoğraf varsayılan olarak "Beklemede" başlar.

    @ManyToMany
    @JoinTable(
            name="outfit_clothing_items",
            joinColumns = @JoinColumn(name= "outfit_id"),
            inverseJoinColumns= @JoinColumn(name="clothing_item_id")
    )
    private Set<ClothingItem> clothingItems; // Benzersiz verilerde set | tekrar eden verilerde list kullanılır.

    // AR (Sanal Deneme) veya kullanıcının ayna karşısında çektiği kombin fotoğrafı
    @Column(name = "outfit_image_url")
    private String outfitImageUrl;

    //  Kombinin türünü belirler (Örn: "LOOKBOOK", "AR_TRYON", "MANUAL")
    @Column(name = "outfit_type", length = 50)
    private String type;

    //  Canvas ekranındaki koordinatları tutan JSON verisi
    @Column(name = "canvas_data", columnDefinition = "TEXT")
    private String canvasData;

    @Column(name= "created_at", updatable=false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        this.createdAt=LocalDateTime.now();
    }
}