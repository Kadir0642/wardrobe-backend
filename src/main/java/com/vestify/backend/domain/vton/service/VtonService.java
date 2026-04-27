package com.vestify.backend.domain.vton.service;

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

@Service
public class VtonService {

    // 🚀 DİKKAT: Java Docker içinde olduğu için dışarıdaki Windows'a (Python'a) bu adresle çıkar!
    private final String PYTHON_API_URL = "http://host.docker.internal:8001/vton/try-on";

    public String requestVirtualTryOn(MultipartFile personImage, MultipartFile garmentImage) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // Kişinin fotoğrafı
        body.add("person_image", new ByteArrayResource(personImage.getBytes()) {
            @Override
            public String getFilename() {
                return personImage.getOriginalFilename() != null ? personImage.getOriginalFilename() : "person.jpg";
            }
        });

        // Kıyafet fotoğrafı
        body.add("garment_image", new ByteArrayResource(garmentImage.getBytes()) {
            @Override
            public String getFilename() {
                return garmentImage.getOriginalFilename() != null ? garmentImage.getOriginalFilename() : "garment.jpg";
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // İsteği Python'a fırlatıyoruz
        ResponseEntity<String> response = restTemplate.postForEntity(PYTHON_API_URL, requestEntity, String.class);

        return response.getBody();
    }
}