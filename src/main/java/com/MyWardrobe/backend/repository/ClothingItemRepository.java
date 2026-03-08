package com.MyWardrobe.backend.repository;

import com.MyWardrobe.backend.entity.ClothingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository // Database elemanı
public interface ClothingItemRepository extends JpaRepository<ClothingItem, Long> {

    // List<ClothingItem> veritabanında o kullanıcıya ait  birden fazla(n) kıyafet olacağı için n elemanlı liste döndürür yoksa boş liste ([]) döner. Sistem çökmez
    // Bu ID ye sahip kullanıcının dolabındaki tüm parçaları getir | AI ve Uygulama için sorgu
    List<ClothingItem> findByUserId(Long userId); // SELECT * FROM clothing_items WHERE user_id=?


    // JpaRepository kütüphanesi sayesinde SQL yazmadan .save(), .findALl(), . deleteById() komutlarla veritabanını yönetebiliriz.
    // İleride buraya AI algoritması için "Bu kullanıcının kışlık eşyalarını getir" tarzı komutlar yazacağız.
}
