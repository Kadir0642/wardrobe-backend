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

    // DİNAMİK YÖNLENDİRME (ROUTING) İÇİN ENDPOINT'LER
    private final String ENDPOINT_IDM_VTON = "https://fal.run/fal-ai/idm-vton";
    private final String ENDPOINT_FASHN = "https://fal.run/fal-ai/fashn/tryon/v1.5"; // Fashn v1.5 API Endpoint'i

    public VtonWorker(VtonTaskTracker taskTracker) {
        this.taskTracker = taskTracker;
        // BaseUrl'i burada tanımlamıyoruz, çünkü her istekte (kategoriye göre) Endpoint değişecek!
        this.webClient = WebClient.builder().build();
    }

    @RabbitListener(queues = RabbitMQConfig.VTON_QUEUE)
    public void processVtonTask(VtonTaskMessage message) {
        String taskId = message.getRequestId();
        
        System.out.println("=====================================================");
        System.out.println(" [MULTI-GARMENT PIPELINE w/ ROUTING] İŞLEM BAŞLADI: " + taskId);

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

            // 1. AŞAMA: TOPS (Üst Giyim -> IDM-VTON)
            // STRATEJİ DEĞİŞİKLİĞİ: Önce TOPS giydirilir ki, BOTTOMS (Pantolon) onun üstüne/altına daha iyi otursun!
            boolean hasFullBody = garments.stream().anyMatch(g -> "FULL BODY".equalsIgnoreCase(g.getCategory()));
            if (!hasFullBody) {
                currentPersonImage = processGarmentCategory(garments, "TOPS", currentPersonImage, "upper_body",
                        "The exact top shown in the reference garment image. Strictly preserve the original sleeve length and neckline. Do not add sleeves if the garment is sleeveless.",
                        "long sleeves, extra fabric, disfigured arms, bad skin"); // NEGATİF PROMPT);
            }

            // 2. AŞAMA: BOTTOMS (Alt Giyim -> IDM-VTON)
            currentPersonImage = processGarmentCategory(garments, "BOTTOMS", currentPersonImage, "lower_body", 
                "The exact bottoms shown in the reference image. Strictly preserve the original length, fit, and design. Do not alter the style.",
                       "changing the shirt, naked top, bad anatomy"); //  NEGATİF PROMPT
            
            // 3. AŞAMA: FULL BODY (Elbise -> FASHN)
            //"dresses" kategorisi için fashn modelinde kategori "one-pieces" olmalı!
            currentPersonImage = processGarmentCategory(garments, "FULL BODY", currentPersonImage, "one-pieces", 
                "The exact full body dress shown in the reference garment image. Strictly preserve the original sleeve length (if sleeveless, keep it sleeveless) and neckline. Completely cover and replace any existing pants or trousers on the person's legs. Perfectly render bare skin where fabric is missing.",
                       "long sleeves, extra fabric on arms, pants, trousers, bad skin generation, disfigured anatomy");
            
            // 4. AŞAMA: OUTERWEAR (Dış Giyim -> IDM-VTON)
            //  Ceket giydirirken prompta "open jacket" yazıyoruz ki içindekini silmesin!
            currentPersonImage = processGarmentCategory(garments, "OUTERWEAR", currentPersonImage, "upper_body", 
                "An open jacket or coat worn over the existing clothes. Preserve the exact design, collar, and sleeve length of the reference outerwear. Preserve the clothes underneath exactly.",
                    "closed jacket, erasing undergarment, merged fabrics");

            // TÜM ZİNCİRLEME BİTTİ! FİNAL RESMİ TELEFONA YOLLA
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
     *  Helper Metot: Belirli bir kategorideki kıyafeti bulur, modeli seçer ve şemasını (schema) ayarlar.
     */
    private String processGarmentCategory(List<VtonTaskMessage.GarmentItemMessage> garments, String categoryName, String currentPersonImage, String aiCategory, String prompt, String negativePrompt) {

        // Bu kategoride giyilecek bir kıyafet var mı diye bakıyoruz
        VtonTaskMessage.GarmentItemMessage garmentToWear = garments.stream()
                .filter(g -> categoryName.equalsIgnoreCase(g.getCategory()))
                .findFirst()
                .orElse(null);

        if (garmentToWear == null) {
            return currentPersonImage; // // Bu kategoriden bir şey giyilmeyecekse pas geç (eski fotoyu koru)
        }

        // DİNAMİK MODEL SEÇİMİ (DYNAMIC ROUTING)
        String targetEndpoint = categoryName.equals("FULL BODY") ? ENDPOINT_FASHN : ENDPOINT_IDM_VTON;

        System.out.println("⏳ " + categoryName + " Giydiriliyor... Model: " + (categoryName.equals("FULL BODY") ? "FASHN" : "IDM-VTON"));

        // Fal.ai API İsteği Hazırlığı (JSON Body)
        Map<String, Object> requestBody = new HashMap<>();
        
        // MODEL BAZLI ŞEMA AYARLAMASI (Büyük Sorunu Çözen Kısım)
        // Fashn'in istediği özel model_image formatını ayırlandı ve IDM-VTON'un description formatını bozmadan ikisini tek bir metotta erittik
        if (targetEndpoint.equals(ENDPOINT_FASHN)) {
             // FASHN Modeli Parametreleri
             requestBody.put("model_image", currentPersonImage);  // Mankenin o anki hali
             requestBody.put("garment_image", garmentToWear.getUrl());  // Giydirilecek kıyafet
            requestBody.put("negative_prompt", negativePrompt); // FASHN için Negatif
             requestBody.put("category", aiCategory); // "one-pieces" olarak gelecek
             requestBody.put("nsfw_filter", false); // Bazen dekolteleri engellememesi için
        } else {
             // IDM-VTON Modeli Parametreleri
             requestBody.put("human_image_url", currentPersonImage); 
             requestBody.put("garment_image_url", garmentToWear.getUrl()); 
             requestBody.put("category", aiCategory);
             requestBody.put("description", prompt);
             requestBody.put("negative_prompt", negativePrompt); // IDM-VTON için Negatif
             requestBody.put("num_inference_steps", 25);  //FİNOPS: Turbo modellere geçene kadar şimdilik 25 step ile kalite/hız dengesi kuralım
        }

        // API İsteği
        Map response = webClient.post()
                .uri(targetEndpoint) // Her kategori için seçilen modele gider
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Key " + falAiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(165))  // Her bir kıyafet için bekleme süresi
                .block(); 

        if (response != null && (response.containsKey("image") || response.containsKey("images"))) {
            // FASHN modeli bazen "images" isimli bir liste (array) dönebilir. Bunu da kontrol altına alıyoruz.
            String resultImageUrl = "";
            if (response.containsKey("images")) {
                List<Map<String, Object>> imagesList = (List<Map<String, Object>>) response.get("images");
                resultImageUrl = (String) imagesList.get(0).get("url");
            } else if (response.containsKey("image")) {
                Map<String, Object> imageObj = (Map<String, Object>) response.get("image");
                resultImageUrl = (String) imageObj.get("url");
            }
            
            System.out.println("✨ " + categoryName + " başarıyla giydirildi: " + resultImageUrl);
            return resultImageUrl;  // Çıkan yeni resmi (Mankenin yeni halini) geri dön
        }

        throw new RuntimeException(categoryName + " giydirilirken AI'dan geçerli bir resim dönmedi.");
    }
}