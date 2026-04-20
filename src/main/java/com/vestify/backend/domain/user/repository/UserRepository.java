package com.vestify.backend.domain.user.repository;

import com.vestify.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

//Veriye erişim katmanı - Data Access Object
@Repository  // Veritabanı işlemleriyle ilgilenir..  | Springe hangi tabloyla çalışacağını ve o tablonun Primary Key(@Id)tipini verir
public interface UserRepository extends JpaRepository<User, Long> {

    // Derived QueryMethods (Türetilmiş sorgu methodları) | find(Bul), By(Şuna göre), Email(Email sütunu)
    // <Optional<User> Boş değer hatası riskini yok eder. Optional aslında bir kutudur.
    // Veriyi bulursa dolu gelir, yoksa boş gelir ama sistem hata verip çökmez.

    // Mimarın Notu: Soft Delete (is_active) eklediğimiz için, silinmiş (kapatılmış) hesapları getirmemek adına sorgulara AndIsActiveTrue ekledik.
    // Sadece aktif (silinmemiş) kullanıcıyı email ile bul
    Optional<User> findByEmailAndIsActiveTrue(String email); // SELECT * FROM users WHERE email =? && IsActıveTrue=True


    // Sosyal ağ profilleri için kullanıcı adıyla bulma
    Optional<User> findByUserNameAndIsActiveTrue(String userName);

}

// JpaRepository mirası ile doğrudan:
//save(user): Yeni kullanıcı kaydeder veya var olanı günceller.
//findAll(): Veritabanındaki tüm kayıtları liste olarak getirir.
//findById(1L): ID'si 1 olan kaydı bulur.
//delete(clothingItem): Verilen kıyafeti veritabanından siler.