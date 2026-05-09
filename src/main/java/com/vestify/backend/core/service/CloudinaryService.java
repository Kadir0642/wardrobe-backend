package com.vestify.backend.core.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // 🚀 REST API'den gelen görseli alır, Cloudinary'ye fırlatır ve URL'yi döner
    public String uploadImage(MultipartFile file) throws IOException {
        String uniqueFilename = "vton_person_" + UUID.randomUUID().toString();

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", "vestify_users/" + uniqueFilename
        ));

        return uploadResult.get("secure_url").toString();
    }

    // 🚀 FINOPS: Geçici olarak yüklenen fotoğrafı Cloudinary'den siler
    // Bunu nasıl daha az maliyetli ve optimize hale getirebilirim -> "FinOps" (Financial Operations)
    public void deleteImageByUrl(String imageUrl) {
        try {
            // URL'den public_id'yi (Klasör + Dosya Adı) çekip çıkarma algoritması
            String[] parts = imageUrl.split("/");
            String folderAndFile = parts[parts.length - 2] + "/" + parts[parts.length - 1];

            // Cloudinary'den bir resmi silmek için o resmin public_id (kimlik) bilgisine ihtiyacımız var.
            // Yüklediğimiz resimlerin URL'si genellikle şöyledir:
            //.../upload/v177.../vestify_users/vton_person_3395...jpg
            //Buradaki public_id kısmı klasör adıyla beraber: vestify_users/vton_person_3395... şeklindedir.

            // Uzantıyı (.jpg, .png) kesip atıyoruz
            String publicId = folderAndFile.substring(0, folderAndFile.lastIndexOf('.'));

            // Cloudinary'e imha emrini gönder
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            System.out.println("🗑️ FinOps: Geçici fotoğraf Cloudinary'den silindi -> " + publicId);

        } catch (Exception e) {
            System.err.println("🚨 Cloudinary silme hatası: " + e.getMessage());
        }
    }
}