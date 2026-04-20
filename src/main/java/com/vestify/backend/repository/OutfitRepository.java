package com.vestify.backend.repository;

import com.vestify.backend.entity.Outfit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutfitRepository extends JpaRepository<Outfit, Long> {

    //Kullanıcının tüm kombinlerini getirecek metod
    List<Outfit> findByUserId(Long userId);  // SELECT * FROM outfits WHERE user_id = ?
}
