package com.MyWardrobe.backend.repository;

import com.MyWardrobe.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//Veriye erişim katmanı - Data Access Object
@Repository // Veritabanı işlemleriyle ilgilenir..  | Springe hangi tabloyla çalışacağını ve o tablonun Primary Key(@Id)tipini verir
public interface UserRepository extends JpaRepository<User, Long>{ // Interface'de tanımlanan sorgular,JPA tarafından runtime sırasında implementasyonu arkaplanda otomatik üretecek

    // Derived QueryMethods (Türetilmiş sorgu methodları) | find(Bul), By(Şuna göre), Email(Email sütunu)
    // <Optional<User> Boş değer hatası riskini yok eder. Optional aslında bir kutudur ve veriyi bulursa dolu gelir, yoksa boş gelir ama sistem hata verip çökmez.
    java.util.Optional<User> findByEmail(String email); // SELECT * FROM users WHERE email =?

    // JpaRepository mirası ile doğrudan:
    //save(user): Yeni kullanıcı kaydeder veya var olanı günceller.
    //findAll(): Veritabanındaki tüm kayıtları liste olarak getirir.
    //findById(1L): ID'si 1 olan kaydı bulur.
    //delete(clothingItem): Verilen kıyafeti veritabanından siler.


}
