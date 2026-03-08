package com.MyWardrobe.backend.repository;

import com.MyWardrobe.backend.entity.ClothingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository // Database elemanı
public interface ClothingItemRepository extends JpaRepository<ClothingItem, Long> {

    // Bu ID ye sahip kullanıcının dolabındaki tüm parçaları getir
    List<ClothingItem> findByUserId(Long userId); // AI ve Uygulama için sorgu
    // JpaRepository kütüphanesi sayesinde SQL yazmadan .save(), .findALl(), . deleteById() komutlarla veritabanını yönetebiliriz.
    // İleride buraya AI algoritması için "Bu kullanıcının kışlık eşyalarını getir" tarzı komutlar yazacağız.
}
