package com.MyWardrobe.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore; // Bir alanın, getter veya setter'ın üzerine yerleştirerek, bu verilerin API yanıtlarında görünmesini veya gelen JSON'dan okunmasını engellersiniz . Genellikle şifreleri gizlemek veya çift yönlü ilişkileri yönetmek gibi güvenlik amacıyla kullanılır
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name= "clothing_items") // Tablo adı
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
    @ManyToOne(fetch = FetchType.LAZY) // Birçok kıyafet, tek bir kullanıcıya aittir. (Kullanıcıdan Kıyafete (One to-Many) ilişkisinin tam tersi yönde)
    @JoinColumn(name = "user_id", nullable=false) // "Clothing_items" tablosunda "user_id" değişkenin ForeignKey (FK) tutulacağını belirtir. | Veritabanına "Git ve bu FK sütununu oluştur" emridir.
    @JsonIgnore // Dışarıya JSON gönderirken kullanıcı verisini sakla | userID,password bunları paketlerken gereksiz bir yükün altına giriyordu. Paketlemediği için artık sistem daha hızlı çalışıyor
    private User user;  // Sahipsiz kıyafet olamaz (nullable).

    @Column(nullable=false) // item adı, Örn:"Burgundy Corduroy Flare Jeans"
    private String name;

    @Column(name= "image_url") // Cloudinary'den gelecek 2D dekupe gorsel linki
    private String imageUrl; // Şimdilik 2D olacağı için kıyafet fotolarını veritabanında kaydetmek yerine (ki bu veritabanını şişirir ve çökertirdi)
                            // sadece Cloudinary gibi bir yerin linkini (String olarak) tutmamızı sağlar
    // Dolap filtrelemesi için hemde AI kullanıcının verilerinden öğreneceği için
    // Hangi parçaları giyiyor temel verileri olucak
    // AI modeli için gerçek dünyada kullanıcı isteklerine göre şekillenmesi için gereken veriler
    private String category; // Örn: "Pants" ,"Shirts" ,"Layers","Outfits" ,"Shoes","accessory"

    @Column(name= "sub_category")
    private String subCategory; // Tişört, Kazak, Kot Pantolon, Sneaker (Trendyol/Boyner API için onemli )

    private String color; // Ana renk (Zıt/Uyumlu renk eşleştirmesi için) Örn: "Burgundy" ,"Black"

    private String pattern; // Düz, Çizgili ,Kareli, Çiçekli (AI' desen karmaşası yapmasini engeller)

    private String formality; // Spor, Gündelik, İş/Şık , Gece (Kullanıcının 3 anahtar kelimesiyle eşleşecek)

    @Column(length=20)
    private String season; // Örn: "Summer","Winter","Fall","All" (Anlık hava durumuna göre filtreleme için)

    private String brand; // Örn: "BERSHKA", "Zara"

    // --- FINANSAL ANALITIK && OYUNLASTIRMA (GAMIFICATION) ---
    @Column(name="purchase_price") // kıyafet satın alma maliyeti
    private Double purchasePrice;

    @Column(name= "wear_count") // Giyim sayısı | Cost Per Wear feature in future / Giyilme Başına Maliyet algoritması için
    private Integer wearCount=0;

    // Kullanıcı bu kıyafeti ne kadar seviyor? (1-4 arası kalp)
    @Column(name ="love_factor") // kombin puanlama buna göre Collaborative filtering
    private Integer loveFactor; // AI bu puanı yüksek olanları merkez (Çapa) parça yapacak

    @Column(name="quality_rating") // 1-4 Yıldız: Kapsül dolap analizleri için
    private Integer qualityRating;

    @Column(name="created_at", updatable=false)
    private LocalDateTime createdAt;

    // Oluşturma tarihi, varsayılan değer atamaları veya doğrulama gibi işlemleri otomatikleştirerek kodun tekrarını önlemek için kullanılır
    @PrePersist // bir varlığın veri tabanına ilk kez kaydedilmesinden (persist/save) hemen önce otomatik olarak çalıştırılan bir yaşam anlatımı (lifecycle) anotasyonu
    protected void onCreate(){
        this.createdAt=LocalDateTime.now();
    } // Kayıt öncesi zamanı ayarlar

    // ---  Cost Per Wear  ---
    // "Bu ceketi almak mantıklı bir finansal karar mıydı?"
    // @Transient sayesinde bu veritabanında sütun olmaz, sadece istek atıldığında havada hesaplanır!
    @Transient // Derived Attribute | Hesaplanmış Özellik
    public Double getCostPerWear(){ // Veritabanını şişirmiyor, sadece mobil uygulama bu veriyi istediği an, o saniye mevcut fiyat ve giyilme sayısına bakarak sonucu havada üretiyoruz.

        // Eğer kıyafetin fiyatı girilmemişse veya bedavaysa maliyet 0'dır.
        if(this.purchasePrice==null || this.purchasePrice<=0.0){
            return 0.0;
        }

        // Eğer kıyafet henüz hiç giyilmemişse, maliyet kıyafetin tam fiyatıdır.
        if(this.wearCount==null || this.wearCount==0){
            return this.purchasePrice;
        }

        //Fiyatı giyilme sayısına böl (Virgül sonrasıyla uğraşmamak için şimdilik net bölelim)
        return this.purchasePrice/this.wearCount;
    }

}
