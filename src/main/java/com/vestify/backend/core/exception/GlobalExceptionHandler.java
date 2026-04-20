package com.vestify.backend.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

// @RestControllerAdvice anotasyonu, sistemdeki tüm Controller'ların üzerine görünmez bir ağ (radar) atar.
// Nerede bir throw new RuntimeException(...) çalışırsa, bu radar onu havada yakalar ve bizim standart kutumuza koyup yollar.

@RestControllerAdvice //  Tüm Controller'ları dinleyen global bir gözcü.
@Slf4j
public class GlobalExceptionHandler {

    // 1. İŞ MANTIĞI HATALARI (Bizim fırlattığımız RuntimeException'lar)
    // Örn: "Bu email zaten kullanımda", "Kombin bulunamadı" vs.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorDetails> handleBusinessExceptions(RuntimeException ex, HttpServletRequest request) {
        log.warn("İşlem reddedildi (İş Mantığı): {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value()) // 400 - Kullanıcı Hatası
                .error("Bad Request")
                .message(ex.getMessage()) // "Kullanıcı bulunamadı!" mesajı buraya girer
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    // 2. KÜRESEL ÇÖKMELER (Beklenmeyen Sunucu Hataları)
    // Örn: Veritabanı bağlantısı koptu, NullPointerException oluştu vs.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalExceptions(Exception ex, HttpServletRequest request) {
        // Bu hatalar kritiktir, konsola kırmızı (ERROR) basılır ve detayı yazdırılır.
        log.error("BEKLENMEYEN SİSTEM HATASI! Yol: {}", request.getRequestURI(), ex);

        ErrorDetails errorDetails = ErrorDetails.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500 - Sunucu Hatası
                .error("Internal Server Error")
                // MİMARİ KURAL: Hackerlara ipucu vermemek için gerçek hatayı gizliyor, jenerik mesaj dönüyoruz.
                .message("Sistemde beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin.")
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}