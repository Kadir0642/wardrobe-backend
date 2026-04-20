package com.vestify.backend.repository;

import com.vestify.backend.entity.OutfitFeedbackLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutfitFeedbackRepository extends JpaRepository<OutfitFeedbackLog, Long> {
    // İleride Python AI servisi için "find by userId" gibi özel sorgular buraya gelecek
}