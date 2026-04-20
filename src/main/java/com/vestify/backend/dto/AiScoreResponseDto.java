package com.vestify.backend.dto;

import lombok.Data;

@Data
public class AiScoreResponseDto {
    private Long user_id;
    private Double score;
    private Boolean approved;
    private String message;
}