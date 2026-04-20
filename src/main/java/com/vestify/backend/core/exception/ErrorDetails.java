package com.vestify.backend.core.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ErrorDetails { // Standart Hata Kutusu (ErrorDetails DTO)
    private LocalDateTime timestamp; // Hata ne zaman oldu?
    private int status;              // HTTP Kodu (Örn: 400, 404, 500)
    private String error;            // Hatanın kısa adı (Örn: "Bad Request")
    private String message;          // Mobil ekranda kullanıcıya gösterilecek mesaj
    private String path;             // Hatanın yaşandığı API adresi
}