package com.MyWardrobe.backend.entity;

import jakarta.persistence.*;
import lombok.*;
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
    // Performas farkları yaratırlar
    @ManyToOne(fetch = FetchType.LAZY) // Birçok kıyafet, tek bir kullanıcıya aittir. (Kullanıcıdan Kıyafete (One to-Many) ilişkisinin tam tersi yönde)
    @JoinColumn(name = "user_id", nullable=false) // "Clothing_items" tablosunda "user_id" değişkenin ForeignKey (FK) tutulacağını belirtir. | Veritabanına "Git ve bu FK sütununu oluştur" emridir.
    private User user;  // Sahipsiz kıyafet olamaz (nullable).

    @Column(nullable=false) // item adı, Örn:"Burgundy Corduroy Flare Jeans"
    private String name;

    @Column(name= "image_url") // Cloudinary'den gelecek fotoğraf linkini burada tutacağız.
    private String imageUrl; // Şimdilik 2D olacağı için kıyafet fotolarını veritabanında kaydetmek yerine (ki bu veritabanını şişirir ve çökertirdi)
                            // sadece Cloudinary gibi bir yerin linkini (String olarak) tutmamızı sağlar
    // Dolap filtrelemesi için hemde AI kullanıcının verilerinden öğreneceği için
    // Hangi parçaları giyiyor temel verileri olucak
    // AI modeli için gerçek dünyada kullanıcı isteklerine göre şekillenmesi için gereken veriler
    private String category; // Örn: "Pants" ,"Shirts" ,"Layers"
    private String color; // Örn: "Burgundy" ,"Black"
    private String brand; // Örn: "BERSHKA"

    @Column(name="purchase_price") // kıyafet satın alma fiyatı
    private Double purchasePrice;

    @Column(name= "wear_count") // Giyim sayısı | Cost Per Wear feature in future / Giyilme Maliyeti hesabı için
    private Integer wearCount=0;

    // Kullanıcı bu kıyafeti ne kadar seviyor? (1-4 arası kalp)
    @Column(name ="love_factor") // kombin puanlama buna göre Collaborative filtering
    private Integer loveFactor; // Sevdiği parçalar merkeze alınacak

    @Column(name="quality_rating") // Kalite derecelendirme (1-4 yıldız)
    private Integer qualityRating;

    @Column(name="created_at", updatable=false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        this.createdAt=LocalDateTime.now();
    }
}
