package com.MyWardrobe.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final Cloudinary cloudinary; // Dependency Injection

    // Uygulamanın elindeki cep telefonu | Kendi veritabanına bağlanmak yerine, internetteki veya bilgisayardaki başka bir web sunucusuna (bizim durumumuzda 8000 portunda çalışan Python AI servisine) HTTP istekleri atıp oradan cevap almasını sağlar.
    private final RestTemplate restTemplate = new RestTemplate(); // Başka sunucularla (Python) konuşma motorumuz


    // Fotoğrafı alıp uçtan uca işleyen metod | Microservices
    public String uploadAndRemoveBackground(MultipartFile file) throws IOException {

        // --- 1.AŞAMA: PYTHON (AI) İLE KONUŞMA ---
        System.out.println("1.Fotoğraf python AI'a gönderiliyor...");
        String pythonAiUrl= "http://localhost:8080/api/v1/ai/remove-bg";

        HttpHeaders headers= new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 1.1. AI ile Görsel İşleme
        // App den gelen fotoyu (MultiparkFile), görsel işlerini yapan pythona yolluyoruz. (ByteArrayResource ile paketleme )
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        //Dosyayı byte olarak paketleyip kargoya (Python'a) veriyoruz.
        body.add("file",new ByteArrayResource(file.getBytes()){
            @Override
            public String getFilename(){
                return file.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Python'dan arka planı silinmiş PNG'yi (byte dizisi olarak) geri alıyoruz
        // Postacımız (RestTemplate) kargoyu alıp Python servisine (/ai/remove-bg) POST isteği atıyor.
        // Python modeli arka planı dekupe ediyor ve geriye yeni fotoğrafın piksellerini (byte[] yani byte dizisi olarak) döndürüyor.
        ResponseEntity<byte[]> response = restTemplate.postForEntity(pythonAiUrl, requestEntity, byte[].class);
        byte[] transparentImage = response.getBody();
        System.out.println("2. Şeffaf fotoğraf Python'dan başarıyla alındı!");

        // --- 2. AŞAMA: CLOUDINARY'E YÜKLEME ---
        // Elimizde artık arka planı temizlenmiş şeffaf bir PNG'nin pikselleri (transparentImage) var.
        // Bunu veritabanına kaydetmek sistemi anında çökerteceği için, daha önce yapılandırdığımız Cloudinary motorunu devreye sokuyoruz.
        // Cloudinary bu pikselleri alıp kendi bulutuna kaydediyor ve bize herkesin erişebileceği o güvenli linki (secureUrl) veriyor.
        // İşte bu link, ClothingItem tablosundaki o imageUrl sütununa kaydedeceğimiz asıl değer olacak.
        System.out.println("3. Şeffaf fotoğraf Cloudinary bulutuna yükleniyor...");
        Map uploadResult = cloudinary.uploader().upload(transparentImage, ObjectUtils.emptyMap());


        // Cloudinary'nin bize verdiği o kısa ve güvenli ".png" linkini alıyoruz
        String secureUrl = uploadResult.get("secure_url").toString();
        System.out.println("4. İşlem Tamam! Kıyafetin Linki: " + secureUrl);

        return secureUrl;
    }
}
