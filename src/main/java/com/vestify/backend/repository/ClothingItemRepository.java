package com.vestify.backend.repository;

import com.vestify.backend.entity.ClothingItem;
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


    // 🚀 AKILLI VE ESNEK FİLTRELEME MOTORU V2
    @Query("SELECT c FROM ClothingItem c WHERE c.user.id = :userId " +
            "AND (:category IS NULL OR c.category = :category) " +
            // Renk ve Sezonlar virgülle eklendiği için "İçinde geçiyor mu?" (LIKE) diye bakıyoruz.
            "AND (:season IS NULL OR c.season LIKE CONCAT('%', :season, '%')) " +
            "AND (:color IS NULL OR c.color LIKE CONCAT('%', :color, '%')) " +
            // Yeni Eklenen Sütunların Filtreleri:
            "AND (:size IS NULL OR c.size = :size) " +
            "AND (:material IS NULL OR c.material = :material) " +
            "AND (:condition IS NULL OR c.condition = :condition)")
    // Sorgudaki :category gibi iki nokta üst üste ile başlayan dinamik değişkenlerin, aşağıdaki Java parametreleriyle (Örn: String category) eşleşmesini sağlar.
    List<ClothingItem> filterUserWardrobe(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("subCategory") String subCategory,
            @Param("season") String season,
            @Param("color") String color,
            @Param("size") String size,
            @Param("material") String material,
            @Param("condition") String condition
    );

    // --- ÇAPA (ANCHOR) MODELİ İÇİN KATEGORİ BAZLI ARAMA --- [Arama motoruna yeni filtre ayarı]
    // Kullanıcının dolabından belirli bir kategoriye (Örn: "Alt Giyim") ait tüm parçaları getirir.
    List<ClothingItem> findByUserIdAndCategory(Long userId, String category);

    // --- ANALİZ MOTORU İÇİN ---
    // Kullanıcının en çok giydiği (wearCount) kıyafeti bulur ve en yüksekten en düşüğe sıralayıp ilkini (Top 1) getirir.
    ClothingItem findFirstByUserIdOrderByWearCountDesc(Long userId);
}
