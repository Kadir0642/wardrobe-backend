package com.vestify.backend.domain.outfit.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor // Boş parametreli yapıcı metot (JSON dönüşümleri için şart)
@AllArgsConstructor // 4 parametreyi aynı anda alan yapıcı metot
public class AiScoreResponseDto {
    private Long user_id;
    private Double score;
    private Boolean approved;
    private String message;
}