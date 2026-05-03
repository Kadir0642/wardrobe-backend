package com.vestify.backend.domain.vton.service;

import com.vestify.backend.core.config.RabbitMQConfig;
import com.vestify.backend.domain.vton.dto.VtonTaskMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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

// teknik sistemlerde (veritabanı, yazılım, ağ) her veriye "dünyada sadece bir tane" olduğunu garanti eden dijital bir parmak izi atama yöntemidir.
import java.util.UUID; // UUID -> Universal Unique Identifier (Evrensel benzersiz tanımlayıcı)

@Service
public class VtonService {

    //  DİKKAT: Java Docker içinde olduğu için dışarıdaki Windows'a (Python'a) bu adresle çıkar!
    private final String PYTHON_API_URL = "http://host.docker.internal:8001/vton/try-on";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // ==============================================================================
    // YENİ SİSTEM: Asenkron (Kuyruk) Metodu - Silikon Vadisi Standartı
    // ==============================================================================
    public String sendTaskToQueue(VtonTaskMessage task) {
        String requestId = UUID.randomUUID().toString();
        task.setRequestId(requestId);

        // Görevi RabbitMQ kuyruğuna fırlatır ve anında döner
        rabbitTemplate.convertAndSend(RabbitMQConfig.VTON_QUEUE, task);

        return requestId;
    }

    // ==============================================================================
    // ESKİ SİSTEM: Senkron (Doğrudan İstek) Metodu - Test ve Hızlı Denemeler İçin
    // ==============================================================================
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

        // İsteği Python'a fırlatıyoruz ve bitene kadar (20 dakika da sürse) bekliyoruz
        ResponseEntity<String> response = restTemplate.postForEntity(PYTHON_API_URL, requestEntity, String.class);

        return response.getBody();
    }
}