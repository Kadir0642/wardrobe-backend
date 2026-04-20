package com.vestify.backend.domain.outfit.repository;

import com.vestify.backend.domain.outfit.entity.OutfitLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Page & Pageable -> Artık binlerce satır veri sunucuyu yoramayacak!

@Repository
public interface OutfitLogRepository extends JpaRepository<OutfitLog, Long> {
    // Kullanıcının giyme geçmişini (Takvimini) "En Yeniden -> En Eskiye" doğru sıralayarak getirir.
    // OrderByWornDateDesc: Getirilen sonuçları wornDate (Giyilme Tarihi) sütununa göre, en yeniden en eskiye doğru (Descending - Azalan) sırala demektir.
    Page<OutfitLog> findByUserIdOrderByWornDateDesc(Long userId, Pageable pageable);
}