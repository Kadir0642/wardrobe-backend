package com.vestify.backend.domain.vton.service;

import com.vestify.backend.core.config.RabbitMQConfig;
import com.vestify.backend.domain.vton.dto.VtonTaskMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VtonWorker {

    private final VtonTaskTracker taskTracker;
    private final WebClient webClient;

    @Value("${fal.ai.api-key}")
    private String falAiApiKey;

    public VtonWorker(VtonTaskTracker taskTracker, @Value("${fal.ai.endpoint}") String falAiEndpoint) {
        this.taskTracker = taskTracker;
        this.webClient = WebClient.builder()
                .baseUrl(falAiEndpoint)
                .build();
    }

    @RabbitListener(queues = RabbitMQConfig.VTON_QUEUE)
    public void processVtonTask(VtonTaskMessage message) {
        String taskId = message.getRequestId();

        System.out.println("=====================================================");
        System.out.println("🚀 [MULTI-GARMENT PIPELINE] İŞLEM BAŞLADI: " + taskId);

        // Orijinal Mankeni (Person) Başlangıç Olarak Alıyoruz
        String currentPersonImage = message.getPersonImageUrl();
        List<VtonTaskMessage.GarmentItemMessage> garments = message.getGarments();

        try {
            //  STRATEJİ: Kıyafetleri Doğru Sırayla Giydirmek!
            // --- Önce Alt, Sonra Üst (veya Elbise), En Son Ceket. ---
            // 1. Önce Alt Giyim (BOTTOMS)
            // 2. Sonra Üst Giyim (TOPS veya FULL BODY)
            // 3. En son Dış Giyim (OUTERWEAR - Ceket vs.)

            // Akıllı Atlamalar: Eğer kullanıcı sadece Pantolon seçtiyse,
            // Üst ve Ceket adımları otomatik atlanır (garmentToWear == null),
            // AI'a boşuna para ve saniye ödemeyiz!

            // 1. AŞAMA: BOTTOMS (Alt Giyim)
            currentPersonImage = processGarmentCategory(garments, "BOTTOMS", currentPersonImage, "lower_body", "A pair of pants or skirt");

            // 2. AŞAMA: FULL BODY (Elbise vs. varsa alt ve üstü ezer)
            currentPersonImage = processGarmentCategory(garments, "FULL BODY", currentPersonImage, "dresses", "A full body dress");

            // 3. AŞAMA: TOPS (Üst Giyim - Eğer Full Body yoksa)
            boolean hasFullBody = garments.stream().anyMatch(g -> "FULL BODY".equalsIgnoreCase(g.getCategory()));
            if (!hasFullBody) {
                currentPersonImage = processGarmentCategory(garments, "TOPS", currentPersonImage, "upper_body", "A stylish top, shirt or t-shirt");
            }

            // 4. AŞAMA: OUTERWEAR (Ceket/Mont - Katmanlama)
            //  Ceket giydirirken prompta "open jacket" yazıyoruz ki içindekini silmesin!
            currentPersonImage = processGarmentCategory(garments, "OUTERWEAR", currentPersonImage, "upper_body", "An open jacket or coat worn over the clothes");

            //  TÜM ZİNCİRLEME BİTTİ! FİNAL RESMİ TELEFONA YOLLA
            taskTracker.completeTask(taskId, currentPersonImage);
            System.out.println("✅ [PIPELINE BİTTİ] FİNAL SONUÇ: " + currentPersonImage);

        } catch (WebClientResponseException e) {
            System.err.println("🚨 Fal.ai Hatası: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            taskTracker.completeTask(taskId, "HATA");
        } catch (Exception e) {
            System.err.println("🚨 Pipeline Çöktü: " + e.getMessage());
            taskTracker.completeTask(taskId, "HATA");
        }
        System.out.println("=====================================================");
    }

    /**
     * 🚀 Helper Metot: Belirli bir kategorideki kıyafeti bulur, yoksa mevcut resmi geri döner.
     * Varsa AI'a gönderir ve çıkan *yeni* resim URL'sini döner.
     */
    private String processGarmentCategory(List<VtonTaskMessage.GarmentItemMessage> garments, String categoryName, String currentPersonImage, String aiCategory, String prompt) {

        // Bu kategoride giyilecek bir kıyafet var mı diye bakıyoruz
        VtonTaskMessage.GarmentItemMessage garmentToWear = garments.stream()
                .filter(g -> categoryName.equalsIgnoreCase(g.getCategory()))
                .findFirst()
                .orElse(null);

        // Eğer kullanıcı bu kategoriden bir şey seçmediyse, mankenin mevcut halini bozmadan aynen geri yolla
        if (garmentToWear == null) {
            return currentPersonImage;
        }

        System.out.println("⏳ " + categoryName + " Giydiriliyor... (" + garmentToWear.getUrl() + ")");

        // Fal.ai API İsteği Hazırlığı
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("human_image_url", currentPersonImage); // Mankenin o anki hali
        requestBody.put("garment_image_url", garmentToWear.getUrl()); // Giydirilecek kıyafet
        requestBody.put("category", aiCategory);
        requestBody.put("description", prompt); // AI'ı yönlendiren sihirli ipucu
        // 🚀 FİNOPS: Turbo modellere geçene kadar şimdilik 25 step ile kalite/hız dengesi kuralım
        requestBody.put("num_inference_steps", 25);

        // API İsteği
        Map response = webClient.post()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Key " + falAiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(165)) // Her bir kıyafet için bekleme süresi
                .block();

        if (response != null && response.containsKey("image")) {
            Map<String, Object> imageObj = (Map<String, Object>) response.get("image");
            String resultImageUrl = (String) imageObj.get("url");
            System.out.println("✨ " + categoryName + " başarıyla giydirildi: " + resultImageUrl);
            return resultImageUrl; // Çıkan yeni resmi (Mankenin yeni halini) geri dön
        }

        throw new RuntimeException(categoryName + " giydirilirken AI'dan geçerli bir resim dönmedi.");
    }
}