package com.MyWardrobe.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

// Python'un beklediği veri yapısıyla Java'nın göndereceği yapının aynı dilde (json formatında) konuşması gerekir.
@Data
@Builder // iç içe geçmiş sınıf (Nested Class)
@NoArgsConstructor
@AllArgsConstructor
public class AiScoreRequestDto {

    @JsonProperty("user_id") // Python "user_id" bekler, biz Java'da "userId" kullanırız
    private Long userId;

    @JsonProperty("weather_context")
    private String weatherContext;

    private List<ItemFeatureDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemFeatureDto { // Sınıfın içinde private List<AiItemFeature> items; diyerek bir liste oluşturulmuş. Bu listenin içindeki her bir elemanın hangi özelliklere (id, renk, stil vb.) sahip olacağını ise hemen altındaki o "iç sınıf" belirliyor.
        private Long id;              // Bu yapı sayesinde kodun daha düzenli durur ve bu yardımcı sınıfın sadece bu isteğe özel olduğu açıkça belli olur
        private String category;
        private String color;
        private String style;
        private String pattern;
    }
}

// Buradaki temel amaç gruplamaktır. AiItemFeature (eşya özellikleri), tek başına bir anlam ifade etmekten ziyade,
// AiScoreRequest (AI puanlama isteği) paketinin bir parçasıdır.
//Örnek: Bir alışveriş listesi düşün. "Liste" ana sınıftır,
// listenin içindeki her bir "Ürün" ise iç sınıftır. Ürün, liste olmadan havada kalır.