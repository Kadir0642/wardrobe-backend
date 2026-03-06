package com.MyWardrobe.backend.entity; // Sınıfın projedeki konumu

import jakarta.persistence.*;  // JPA (Karmaşık SQL yerine sınıflar ve yöntemlerle minimum kod değişikliğiyle farklı ilişkisel veritabanları arasında geçiş imkanı) Veritabani tablolarini ve sutunlarini belirleyen anotasyonlar burada
import lombok.*; // getter/setter/constructer otomatik yazan Kod temizliği sağlar
import java.time.LocalDateTime;

@Entity  // Hibernate bunun veritabanı tablosu olduğunu biliyor
@Table(name="users") // Tablo adını "users" yaptık
@Getter // Sınıftaki tüm değişkenler için otomatik get
@Setter // ve set üretmeke
@NoArgsConstructor // Parametresiz (boş) yapıcı metot( constrcuter) oluşturur. JPA'nı arka plan da çalışması için zorunlu
@AllArgsConstructor // Tüm değişkenleri içineren dolu bir yapıcı metot oluşturur
@Builder // Nesne oluştururken kodun daha okunabilir olmasını sağlayan Builder tasarım deseni uygular.
public class User{

    @Id // Bu alanın Primary Key olduğunu belirtir.
    @GeneratedValue(strategy=GenerationType.IDENTITY) // ID'nin veritabanı tarafından otomatik olarak (1,2,3..) artırılmasını sağlar.
    private Long id; // baslangic degeri NULL

    /*Spring Boot'ta bir kaydın yeni olup olmadığını ID'sine bakarak anlarız.
        Eğer ID null ise "Bu yeni bir kayıt, kaydedeyim" der.
        (long-0) Eğer 0 olsaydı, "Acaba 0 numaralı kayıt mı, yoksa yeni mi?" diye karışıklık olur.*/

    @Column(nullable=false, unique=true) // Email alanı boş bırakılamaz (nullable)  veritabanında aynı email'den sadece bir tane olabilir (unique)
    private String email;

    @Column(nullable=false) // İlerleyen aşamalarda şifreleri veritabanına kaydetmeden önce "BCrypt (Salted Hash)" ile şifreleyeceğiz.
    private String password;

    // --- FARK YARATAN YER ---

    //Kullanıcının başta seçtiği tarzı
    @Column(name = "style_keywords") // AI modelin gerçek kullanıcıdan veri toplayıp öğreneceği yer
    private String stylekeywords;   // Kullanıcı zevkini anahtar kelimelerden ("Minimalist","Vintage","Spor") başlangıç verisi toplanabilecek

    // Sosyal Ağ özelliği: Kullanıcı dolabını diğerlerine açtı mı?
    @Column(name= "is_public") // Kullanıcı hesaplarının kendi aralarında etkileşime girmesini sağlayan özellik
    private boolean isPublic=false;  // Default gizli olması kullanıcı gizliliği için isteğe baglı acilabilir

    @Column(name="created_at" ,updatable = false) // Hesap oluşturma tarihi ve sonradan güncellenemez.
    private LocalDateTime createdAt;

    @PrePersist // Hibernate veriyi veritabanına ilk defa kaydetmeden önce bu metodu çalıştırır
    protected void onCreate(){ // O anki tarihi değişkene atar.
        this.createdAt=LocalDateTime.now();
    }


}

