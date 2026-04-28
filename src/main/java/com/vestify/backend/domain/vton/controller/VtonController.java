package com.vestify.backend.domain.vton.controller;

import com.vestify.backend.domain.vton.dto.VtonTaskMessage;
import com.vestify.backend.domain.vton.dto.VtonTaskRequest;
import com.vestify.backend.domain.vton.service.VtonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/vton")
public class VtonController {

    @Autowired
    private VtonService vtonService;

    // ====================================================================
    // YENİ (ASYNC): Mobil Uygulamanın Kullanacağı Kuyruk Sistemi
    // ====================================================================
    @PostMapping("/async-try-on")
    public ResponseEntity<?> asyncTryOn(@RequestBody VtonTaskRequest request) {
        try {
            // Gelen isteği kuyruk mesajına (Message DTO) çeviriyoruz
            VtonTaskMessage message = new VtonTaskMessage();
            message.setUserId(request.getUserId());
            message.setPersonImageUrl(request.getPersonUrl());
            message.setGarmentImageUrls(request.getGarmentUrls());
            message.setTuckedIn(request.isTuckedIn());

            // Servise gönderip doğrudan takip numarasını alıyoruz (Beklemek yok!)
            String requestId = vtonService.sendTaskToQueue(message);

            // Kullanıcıya 202 Accepted dönüyoruz (İşlem alındı, arka planda sürüyor)
            return ResponseEntity.accepted().body("İşlem kuyruğa alındı. Takip No: " + requestId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Kuyruk Hatası: " + e.getMessage());
        }
    }

    // ======================================================================================
    // ESKİ (LEGACY): Doğrudan dosya yüklemeli senkron test ucu  <--> Şuan bunu kullanmıyoruz.
    // ======================================================================================
    @PostMapping("/try-on")
    public ResponseEntity<?> tryOnClothes(
            @RequestParam("person_image") MultipartFile personImage,
            @RequestParam("garment_image") MultipartFile garmentImage) {
        try {
            String result = vtonService.requestVirtualTryOn(personImage, garmentImage);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("VTON Köprü Hatası: " + e.getMessage());
        }
    }
}