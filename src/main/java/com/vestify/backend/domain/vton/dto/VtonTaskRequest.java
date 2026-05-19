package com.vestify.backend.domain.vton.dto;

import lombok.Data;
import java.util.List;

//  DIŞ KAPI - Telefondan Gelen

@Data  // içinde requestId yoktur. Çünkü kullanıcı veya mobil uygulama kendi takip numarasını üretemez; bu güvenliğe aykırıdır.
public class VtonTaskRequest { //(DIŞ KAPI)-> Mobil Uygulamanın (React Native) bizim Java sunucumuza gönderdiği pakettir.
    private String userId;
    private String personUrl; // Cloudinary'ye yüklenmiş insan fotoğrafı linki

    private List<GarmentItem> garments; // Sırayla giyilecek kıyafetlerin bilgileri ( url + category )
    private boolean tuckedIn; // Ceket içine sokulsun mu? (Aşama 2 için)

    // İç sınıf:  Kıyafetin URL'si ve Kategorisi birlikte geliyor.
    @Data
    public static class GarmentItem{
        private String url;
        private String category; // "TOPS", "BOTTOMS", "OUTERWEAR"
    }
}

// GARMENT ITEM İÇİN --  [ DIŞ KAPI - Telefondan Gelen  ]
// // Telefondan Java'ya gelecek olan yeni paket şekli:
//  {
//  "userId": "1",
//  "personUrl": "http...",
//  "garments": [
//      { "url": "http...", "category": "TOPS" },
//      { "url": "http...", "category": "BOTTOMS" }
//      ]
//   }