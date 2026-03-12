package com.MyWardrobe.backend.controller;

import com.MyWardrobe.backend.dto.WardrobeStatsDto;
import com.MyWardrobe.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService; // Dependency Injection

    // Kullanıcının Ana Ekran İstatistiklerini Getir.
    @GetMapping("/{userId}")
    public ResponseEntity<WardrobeStatsDto> getDashboardStats(@PathVariable Long userId){
        return ResponseEntity.ok(dashboardService.getUserDashboardStats(userId));
    }
}
