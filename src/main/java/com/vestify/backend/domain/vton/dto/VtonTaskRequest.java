package com.vestify.backend.domain.vton.dto;

import lombok.Data;
import java.util.List;

@Data  // içinde requestId yoktur. Çünkü kullanıcı veya mobil uygulama kendi takip numarasını üretemez; bu güvenliğe aykırıdır.
public class VtonTaskRequest { //(DIŞ KAPI)-> Mobil Uygulamanın (React Native) bizim Java sunucumuza gönderdiği pakettir.
    private String userId;
    private String personUrl; // Cloudinary'ye yüklenmiş insan fotoğrafı linki
    private List<String> garmentUrls; // Sırayla giyilecek kıyafet linkleri
    private boolean tuckedIn; // Ceket içine sokulsun mu? (Aşama 2 için)
}