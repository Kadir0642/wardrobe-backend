package com.vestify.backend.domain.vton.controller;

import com.vestify.backend.domain.vton.dto.VtonTaskMessage;
import com.vestify.backend.domain.vton.dto.VtonTaskRequest;
import com.vestify.backend.domain.vton.service.VtonService;
import com.vestify.backend.domain.vton.service.VtonTaskTracker; // 🚀 YENİ: Takip Sınıfımız
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map; // 🚀 YENİ: JSON dönmek için eklendi

@RestController
@RequestMapping("/api/v1/vton")
public class VtonController {

    @Autowired
    private VtonService vtonService;

    @Autowired
    private VtonTaskTracker taskTracker; // 🚀 YENİ: Takip Merkezini içeri aldık

    // ====================================================================
    // YENİ (ASYNC): Mobil Uygulamanın Kullanacağı Kuyruk Sistemi
    // ====================================================================
    @PostMapping("/async-try-on")
    public ResponseEntity<?> asyncTryOn(@RequestBody VtonTaskRequest request) {
        try {
            // 1. Gelen isteği kuyruk mesajına çeviriyoruz (Senin orjinal kodun)
            VtonTaskMessage message = new VtonTaskMessage();
            message.setUserId(request.getUserId());
            message.setPersonImageUrl(request.getPersonUrl());
            message.setGarmentImageUrls(request.getGarmentUrls());
            message.setTuckedIn(request.isTuckedIn());

            // 2. Servise gönderip doğrudan takip numarasını alıyoruz (Senin orjinal kodun)
            String requestId = vtonService.sendTaskToQueue(message);

            // 🚀 3. YENİ: RabbitMQ'ya giden bu işin numarasını Takip Merkezine "PENDING" olarak kaydediyoruz
            taskTracker.startTask(requestId);

            // 🚀 4. DÜZELTME: Telefona düz metin yerine JSON (Map) dönüyoruz. 
            // Çünkü telefondaki kodumuz "response.data.taskId" diyerek bu numarayı okumaya çalışıyor.
            return ResponseEntity.accepted().body(Map.of( // Map.of() kullanarak bir JSON objesi yolladık
                    "message", "İşlem kuyruğa alındı.",
                    "taskId", requestId
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Kuyruk Hatası: " + e.getMessage()));
        }
    }

    // ====================================================================
    // 🚀 YENİ: Telefonun Akıllı Polling (3-6-9 sn) ile soracağı kapı [ ENDPOINT ]
    // ====================================================================
    @GetMapping("/result/{taskId}")
    public ResponseEntity<?> getVtonResult(@PathVariable String taskId) {
        // Takip merkezinden işin durumunu soruyoruz
        VtonTaskTracker.TaskResult result = taskTracker.getTask(taskId);

        if (result == null) {
            return ResponseEntity.notFound().build(); // Sistemde böyle bir işlem yok
        }

        // İşlemin durumunu (PENDING veya COMPLETED) telefona gönder
        return ResponseEntity.ok(result);
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