package com.MyWardrobe.backend.repository;

import com.MyWardrobe.backend.entity.ClothingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // Database elemanı
public interface ClothingItemRepository extends JpaRepository<ClothingItem, Long> {

    // JpaRepository kütüphanesi sayesinde SQL yazmadan .save(), .findALl(), . deleteById() komutlarla veritabanını yönetebiliriz.
    // İleride buraya AI algoritması için "Bu kullanıcının kışlık eşyalarını getir" tarzı komutlar yazacağız.
}
