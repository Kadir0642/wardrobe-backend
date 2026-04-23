package com.vestify.backend.domain.wardrobe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vestify.backend.domain.user.entity.User;
import com.vestify.backend.domain.wardrobe.enums.ItemCondition;
import com.vestify.backend.domain.wardrobe.enums.ItemSeason;
import com.vestify.backend.domain.wardrobe.enums.ItemStatus;
import com.vestify.backend.domain.wardrobe.enums.ModerationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name= "clothing_items")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClothingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY (çağrıldığında yüklenmesini veri alımı sağlayan | gereksiz veri alımını önler )
    // EAGER (kullanım senaryosu gerektirsin veya gerektirmesin, ilişkili verilerin hepsini her zaman getirir.)
    // Performas farkları yaratırlar | Sadece ihtiyac oldugunda kullanıcı verisini getirir, performansi arttirir
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable=false) // "Clothing_items" tablosunda "user_id" değişkenin ForeignKey (FK) tutulacağını belirtir. | Veritabanına "Git ve bu FK sütununu oluştur" emridir.
    @JsonIgnore  // Dışarıya JSON gönderirken kullanıcı verisini sakla | userID,password bunları paketlerken gereksiz bir yükün altına giriyordu. Paketlemediği için artık sistem daha hızlı çalışıyor
    private User user;

    @Column(nullable=false)
    private String name;

    private String brand;

    @Column(name= "image_url", nullable = false)
    private String imageUrl; // Sadece dekupe edilmiş, sıkıştırılmış nihai görseli tutacağız.

    private String category;
    @Column(name= "sub_category")
    private String subCategory;
    private String formality; // Spor, Gündelik, İş/Şık vb.
    private String color;
    private String size;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition")
    private ItemCondition condition; // NEW_WITH_TAG, LIKE_NEW, USED vb.

    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.WARDROBE; // WARDROBE, ARCHIVED

    @Enumerated(EnumType.STRING)
    @Column(name = "season")
    private ItemSeason season;

    @Column(length = 100)
    private String description;

    @Column(name="purchase_price")
    private Double purchasePrice;

    @Column(name= "purchased_date")
    private LocalDate purchasedDate; // String tehlikesi LocalDate ile çözüldü!

    @Column(name= "wear_count")
    private Integer wearCount = 0;

    @Column(name ="love_factor")
    private Integer loveFactor;

    @Column(name="is_sharable")
    private Boolean isSharable = false;

    @Column(name="is_favorite")
    private Boolean isFavorite = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;
    // Yüklenen her fotoğraf varsayılan olarak "Beklemede" başlar.

    @Column(name="created_at", updatable=false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        this.createdAt=LocalDateTime.now();
    }

    // @Transient sayesinde bu veritabanında sütun olmaz, sadece istek atıldığında havada hesaplanır!
    // Derived Attribute | Hesaplanmış Özellik
    @Transient
    public Double getCostPerWear(){
        if(this.purchasePrice == null || this.purchasePrice <= 0.0) return 0.0;
        if(this.wearCount == null || this.wearCount == 0) return this.purchasePrice;
        return this.purchasePrice / this.wearCount;
    }
}