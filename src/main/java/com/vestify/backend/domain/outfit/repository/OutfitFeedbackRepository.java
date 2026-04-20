package com.vestify.backend.domain.outfit.repository;

import com.vestify.backend.domain.outfit.entity.OutfitFeedbackLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutfitFeedbackRepository extends JpaRepository<OutfitFeedbackLog, Long> {
    // İleride Python AI servisi veya RLHF algoritmaları için eklenecek sorgular burada olacak.
}