package com.MyWardrobe.backend.repository;

import com.MyWardrobe.backend.entity.OutfitFeedbackLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutfitFeedbackRepository extends JpaRepository<OutfitFeedbackLog, Long> {
    // İleride Python AI servisi için "find by userId" gibi özel sorgular buraya gelecek
}