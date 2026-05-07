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
}