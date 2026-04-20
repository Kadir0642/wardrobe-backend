package com.vestify.backend.domain.wardrobe.controller;

import com.vestify.backend.domain.wardrobe.dto.WardrobeStatsDto;
import com.vestify.backend.domain.wardrobe.service.WardrobeDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Tüm bu karmaşık sistemin (kıyafetler, kombinler, giyilme sayıları, fiyatlar)
// en üstte kullanıcıya "özet" olarak sunulduğu vitrindir.

@RestController
@RequestMapping("/api/v1/dashboard") // İstatistikler için ayrılmış özel bir yol. Kullanıcı profilini açtığında veya ana sayfaya girdiğinde tetiklenecek olan yer burasıdır.
@RequiredArgsConstructor
public class WardrobeDashboardController {

    // Controller'ın içinde hiçbir mantık (logic) yok. Sadece servis katmanından (WardrobeDashboardService) gelen hazır veriyi paketleyip kullanıcıya sunuyor.
    // Bu, "Single Responsibility" (Tek Sorumluluk) prensibine tam uyumdur.

    private final WardrobeDashboardService dashboardService;

    @GetMapping("/{userId}") //Hangi kullanıcının verisini göstereceğimizi URL'den (örn: /api/v1/dashboard/5) şık bir şekilde alıyor
    public ResponseEntity<WardrobeStatsDto> getDashboardStats(@PathVariable Long userId){
        return ResponseEntity.ok(dashboardService.getUserDashboardStats(userId));
    }
}