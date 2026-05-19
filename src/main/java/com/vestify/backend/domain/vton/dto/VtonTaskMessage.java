package com.vestify.backend.domain.vton.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

// İÇ HABERLEŞME - RabbitMQ'ya Giden

@Data
@AllArgsConstructor // Ayrıca ağ (network) üzerinden kuyruğa yazılabilmesi için implements Serializable olmak zorundadır.
@NoArgsConstructor  // İçinde Java'nın otomatik ürettiği requestId vardır. Python motoru bu paketi aldığında, işi bitince "Ben şu requestId'li işi bitirdim" diyebilmesi için bu numaraya ihtiyacı vardır.
public class VtonTaskMessage implements Serializable { // (İç Haberleşme)-> Java'nın kendi içinde işleyip RabbitMQ (Python) tarafına fırlattığı pakettir.
    private String requestId; // İstek ID
    private String userId;    // kime ait
    private String personImageUrl; // Cloudinary'deki AR ile üretilen link

    private List<GarmentItemMessage> garments; // Sırasıyla giydirilecek kıyafetler listesi - Kişiyi baştan aşağı giydireceğiz.
    private boolean isTuckedIn;  // Planlanan kişi kıyafeti farklı şekillerde giymek isterse (kıyafeti salaş veya alt giyim içine sıkıştırmak istemese)

    @Data
    @AllArgsConstructor
    @NoArgsConstructor // RabbitMQ'dan geçebilmesi için bu da Serializable olmalı
    public static class GarmentItemMessage implements Serializable{
        private String url;
        private String category;
    }
}