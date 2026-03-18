package com.MyWardrobe.backend.repository;

import com.MyWardrobe.backend.entity.OutfitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OutfitLogRepository extends JpaRepository<OutfitLog,Long> {

    // Kullanıcının giyme geçmişini (Takvimini) "En Yeniden -> En Eskiye" doğru sıralayarak getirir.
    List<OutfitLog> findByUserIdOrderByWornDateDesc(Long userId);
}
