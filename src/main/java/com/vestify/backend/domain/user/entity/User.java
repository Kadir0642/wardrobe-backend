package com.vestify.backend.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vestify.backend.domain.user.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity // Hibernate bunun veritabanı tablosu olduğunu biliyor
@Table(name="users") // Tablo adını "users" yaptık
@Getter @Setter // Sınıftaki tüm değişkenler için otomatik get ve set üretir
@NoArgsConstructor // Parametresiz (boş) yapıcı metot( constrcuter) oluşturur. JPA'nın arka plan da çalışması için zorunlu
@AllArgsConstructor // Tüm değişkenleri içineren dolu bir yapıcı metot oluşturur
@Builder // Nesne oluştururken kodun daha okunabilir olmasını sağlayan Builder tasarım deseni uygular.
public class User {

    @Id // Bu alanın Primary Key olduğunu belirtir.
    @GeneratedValue(strategy=GenerationType.IDENTITY) // ID'nin veritabanı tarafından otomatik olarak (1,2,3..) artırılmasını sağlar.
    private Long id; // baslangic degeri NULL

    /*Spring Boot'ta bir kaydın yeni olup olmadığını ID'sine bakarak anlarız.
        Eğer ID null ise "Bu yeni bir kayıt, kaydedeyim" der.
        (long-0) Eğer 0 olsaydı, "Acaba 0 numaralı kayıt mı, yoksa yeni mi?" diye karışıklık olur.*/

    @Column(nullable=false, unique=true) // Email alanı boş bırakılamaz (nullable)  veritabanında aynı email'den sadece bir tane olabilir (unique)
    private String email;



    @Column(nullable=false) // İlerleyen aşamalarda şifreleri veritabanına kaydetmeden önce "BCrypt (Salted Hash)" ile şifreleyeceğiz.
    @JsonProperty(access=JsonProperty.Access.WRITE_ONLY)// Sadece yazmaya izin ver, okumayı (dışarı sızmayı) engelle!
    private String password;// Şifreyi sadece dışarı veri gönderirken gizle, ama içeri yeni biri kayıt olurken şifresini al!

    @Column(name = "user_name", length = 50, unique = true) // Kullanıcı adı
    private String userName;

    @Column(name = "profile_image_url") // Profile footage
    private String profileImageUrl; // Cloudinary Linki

    @Enumerated(EnumType.STRING) // Uygulama içerisindeki kullanıcı rolleri
    @Column(nullable = false)
    private Role role = Role.USER; // Varsayılan rol USER

    // Virgüllü string yerine, arama yapılabilen temiz liste formatı
    @ElementCollection
    @CollectionTable(name = "user_style_keywords", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "keyword")
    private List<String> styleKeywords;  // Kullanıcı zevkini anahtar kelimelerden ("Minimalist","Vintage","Spor") başlangıç verisi toplanabilecek

    @Column(name= "is_public") // Sosyal Ağ özelliği: Kullanıcı dolabını diğerlerine açtı mı?
    private Boolean isPublic = false;

    @Column(name= "is_verified")
    private Boolean isVerified = false; // Mavi tik / Onaylı hesap

    @Column(name = "is_active")
    private Boolean isActive = true; // Soft delete bayrağı

    @Column(name = "last_login_at") // Kullanıcın son aktiflik tarihi
    private LocalDateTime lastLoginAt;

    @Column(name="created_at", updatable = false) // Hesap oluşturma tarihi ve sonradan güncellenemez.
    private LocalDateTime createdAt;

    @Column(name="deleted_at")
    private LocalDateTime deletedAt; // Hesap silinirse veri tabanından uçurmak yerine bu tarihi atacağız

    @PrePersist // Hibernate veriyi veritabanına ilk defa kaydetmeden önce bu metodu çalıştırır
    protected void onCreate(){
        this.createdAt=LocalDateTime.now();
    }
}