package com.vestify.backend.domain.vton.controller;

import com.vestify.backend.domain.vton.service.VtonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/vton") // Projenin genel API standardına uyması için api/v1 ekledim
public class VtonController {

    @Autowired
    private VtonService vtonService;

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