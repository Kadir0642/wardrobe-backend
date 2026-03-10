package com.MyWardrobe.backend.repository;

import com.MyWardrobe.backend.entity.ClothingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository // Database elemanı
public interface ClothingItemRepository extends JpaRepository<ClothingItem, Long> {

    // JpaRepository kütüphanesi sayesinde SQL yazmadan .save(), .findALl(), . deleteById() komutlarla veritabanını yönetebiliriz.

    // List<ClothingItem> veritabanında o kullanıcıya ait  birden fazla(n) kıyafet olacağı için n elemanlı liste döndürür yoksa boş liste ([]) döner. Sistem çökmez
    // Bu ID ye sahip kullanıcının dolabındaki tüm parçaları getir | AI ve Uygulama için sorgu
    List<ClothingItem> findByUserId(Long userId); // SELECT * FROM clothing_items WHERE user_id=?


    // Esnek Filtreleme Motoru
    // Eğer parametre boş (NULL) gelirse o şartı atlar,dolu gelirse eşleştirir.
    // JPQL (Java Persistence Query Language)
    @Query("SELECT c FROM ClothingItem c WHERE c.user.id = :userId " +
            "AND (:category IS NULL OR c.category = :category) " +
            "AND (:season IS NULL OR c.season = :season) " +
            "AND (:color IS NULL OR c.color = :color)") //rengi ne olursa olsun diğer şartlara uyan tüm kıyafetleri getirir. | boş değilse istenen renkli olanları getirir.
    List<ClothingItem> filterUserWardrobe( // Sorgudaki :category gibi iki nokta üst üste ile başlayan dinamik değişkenlerin, aşağıdaki Java parametreleriyle (Örn: String category) eşleşmesini sağlar.
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("season") String season,
            @Param("color") String color
    );

}
