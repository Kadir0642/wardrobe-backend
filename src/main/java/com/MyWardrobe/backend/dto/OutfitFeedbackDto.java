package com.MyWardrobe.backend.dto;

import com.MyWardrobe.backend.enums.FeedbackReason;
import com.MyWardrobe.backend.enums.FeedbackType;
import lombok.Data;

import java.util.List;

@Data
public class OutfitFeedbackDto {
    private Long userId;
    private List<Long> outfitItemIds; // O an ekranda olan kombindeki tüm eşyalar
    private FeedbackType feedbackType; // LIKE, DISLIKE, WORE_IT
    private FeedbackReason reasonCode; // Neden beğenmedi?
    private List<Long> targetItemIds; // Sadece şikayet ettiği spesifik eşyalar (varsa)
    private String weatherContext; // "17°C, Bilecik Merkez"
}