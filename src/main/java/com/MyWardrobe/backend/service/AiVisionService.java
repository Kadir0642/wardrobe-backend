package com.MyWardrobe.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiVisionService {

    // Uygulamanın elindeki cep telefonu | Kendi veritabanına bağlanmak yerine,
    // internetteki veya bilgisayardaki başka bir web sunucusuna (bizim durumumuzda 8000 portunda çalışan Python AI servisine) HTTP istekleri atıp oradan cevap almasını sağlar.
    private final RestTemplate restTemplate = new RestTemplate(); // Başka sunucularla (Python) konuşma motorumuz
    // Python AI Mikroservisimizin yeni Asenkron adresi
    private final String AI_SERVER_URL = "http://localhost:8000/api/v1/vision";
    /**
     * ADIM 1: Fotoğrafı Python'a yollar ve anında Task ID (Fiş) alır.
     */
    public String extractClothesAsync(MultipartFile file, String mode) throws IOException {
        System.out.println("1. Fotoğraf Python AI Asenkron Kuyruğuna Gönderiliyor...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
            }
        });
        body.add("mode", mode);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            // Python'a POST isteği atıyoruz
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    AI_SERVER_URL + "/extract-async",
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String taskId = (String) response.getBody().get("task_id");
                System.out.println("2. Fiş (Task ID) Alındı: " + taskId);
                return taskId;
            }
            throw new RuntimeException("AI Sunucusu Task ID dönemedi.");
        } catch (Exception e) {
            System.err.println("AI Sunucusuna bağlanılamadı: " + e.getMessage());
            throw new RuntimeException("Yapay Zeka servisi şu an ulaşılamaz durumda.");
        }
    }

    /**
     * ADIM 2: Alınan Task ID ile Python'a "Durum nedir?" diye sorar.
     * İşlem bitmişse (SUCCESS) Cloudinary linklerini ve Tag'leri içeren JSON'ı döndürür.
     */
    public Map<String, Object> checkTaskStatus(String taskId) {
        System.out.println("AI İşlem Durumu Sorgulanıyor... Task ID: " + taskId);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    AI_SERVER_URL + "/tasks/" + taskId,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody(); // İçinde "status" (PENDING/SUCCESS) ve "result" var
            }
            throw new RuntimeException("AI Görev durumu sorgulanamadı.");
        } catch (Exception e) {
            throw new RuntimeException("AI Sunucusuna bağlanılamadı: " + e.getMessage());
        }
    }
}