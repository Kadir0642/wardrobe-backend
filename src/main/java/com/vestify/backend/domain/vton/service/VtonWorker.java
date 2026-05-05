package com.vestify.backend.domain.vton.service;

import com.vestify.backend.core.config.RabbitMQConfig;
import com.vestify.backend.domain.vton.dto.VtonTaskMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class VtonWorker {

    private final VtonTaskTracker taskTracker;
    private final WebClient webClient;

    @Value("${fal.ai.api-key}")
    private String falAiApiKey;

    // 🚀 MİMARİ DOKUNUŞ: WebClient'ı performanslı çalışması için Constructor'da bir kez inşa ediyoruz (Build)
    public VtonWorker(VtonTaskTracker taskTracker, WebClient.Builder webClientBuilder, @Value("${fal.ai.endpoint}") String falAiEndpoint) {
        this.taskTracker = taskTracker;
        this.webClient = webClientBuilder
                .baseUrl(falAiEndpoint)
                .build();
    }

    // RabbitMQ mesaj kuyruğunu 7/24 dinler
    @RabbitListener(queues = RabbitMQConfig.VTON_QUEUE)
    public void processVtonTask(VtonTaskMessage message) {
        String taskId = message.getRequestId();

        System.out.println("=====================================================");
        System.out.println("🚀 [FAL.AI WEBCLIENT WORKER] YENİ GÖREV ALINDI!");
        System.out.println("Task ID: " + taskId);

        try {
            // 1. Fal.ai Paketini Hazırla  | burası sanırım kullanıcının isteğine göre giydirme yapacağımız alan seçeneklerle alakalı giyim şekli yaptırılabilir.
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("human_image_url", message.getPersonImageUrl());
            requestBody.put("garment_image_url", message.getGarmentImageUrls().get(0));
            requestBody.put("category", "upper_body");
            requestBody.put("num_inference_steps", 30);

            System.out.println("⏳ Fal.ai GPU'ları kıyafeti giydiriyor (Non-blocking istek atılıyor)...");

            // 2. WEBCLIENT İLE ASENKRON İSTEK (REACTIVE PROGRAMMING)
            Map response = webClient.post()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Key " + falAiApiKey)
                    .bodyValue(requestBody)
                    .retrieve() // Cevabı getir
                    .bodyToMono(Map.class) // Gelen JSON'u Map'e dönüştür
                    .timeout(Duration.ofSeconds(60)) // AI 60 saniyede cevap vermezse işlemi iptal et
                    .block(); // ⚠️ DİKKAT: Neden block() kullandığımızı aşağıda açıkladım!

            // WebClient tamamen asenkrondur. Eğer .block() yerine reaktif dünyanın kuralı olan .subscribe() kullansaydık, kod hiç beklemeden anında biterdi.
            //Fakat burada bir "Tuzak" var: Biz bu kodu RabbitMQ Listener'ının içinde çalıştırıyoruz.
            // Eğer kod anında biterse, RabbitMQ işlem bitti sanıp mesajı kuyruktan siler.
            // Eğer o sırada Fal.ai'den hala cevap geliyorsa ve senin sunucun yeniden başlarsa, o mesaj sonsuza kadar kaybolur!
            // Bu yüzden, mesajı güvene almak (Message Durability) adına RabbitMQ'ya "Fal.ai'den cevap gelene kadar
            // bu mesajı kuyrukta güvende tut" demek için burada block() kullanmak bir sektör standardıdır (Manuel Ack sistemi kurmadığımız sürece).
            // Yani WebClient'in gücünü kullanıyoruz ama mesajlarımızı da koruma altına alıyoruz.

            // RabbitMQ'da Manuel Ack (Acknowledgement - Onaylama) Sistemi, bir tüketicinin (consumer - Java Spring Boot uygulamanız)
            // kuyruktan aldığı bir mesajı başarılı bir şekilde işledikten sonra, RabbitMQ sunucusuna "bu mesajı başarıyla işledim,
            // kuyruktan silebilirsin" bilgisini manuel olarak gönderdiği bir güven mekanizmasıdır.

            //Bu sistem, mesajların işlenirken kaybolmasını önler ve veri tutarlılığını sağlar.
            //Neden Manuel Ack Kullanmalıyız?
            //Güvenilir İşleme: Otomatik Ack sisteminde mesaj tüketiciye ulaştığı anda kuyruktan silinir.
            // Eğer tüketici mesajı işlerken hata verirse (crash, exception), mesaj kaybolur.
            // Hata Yönetimi: Manuel Ack ile mesajın işlenmesi başarısız olursa, mesajı reddedip (Nack/Reject)
            // tekrar kuyruğa girmesini (requeue) veya ölü mektup kuyruğuna (Dead Letter Exchange - DLX) yönlendirebilirsiniz.
            //İşlem Kontrolü: Mesajın ne zaman "tamamlandı" sayılacağına uygulama karar verir


            // 3. BAŞARILI SONUCU İŞLE
            if (response != null && response.containsKey("image")) {
                Map<String, Object> imageObj = (Map<String, Object>) response.get("image");
                String resultImageUrl = (String) imageObj.get("url");

                taskTracker.completeTask(taskId, resultImageUrl);

                System.out.println("✅ [WEBCLIENT] İŞLEM HARİKA BİR ŞEKİLDE BİTTİ!");
                System.out.println("📸 Sonuç URL: " + resultImageUrl);
            }

        } catch (Exception e) {
            System.err.println("🚨 WebClient Kritik Hata (Timeout veya API Çökmesi): " + e.getMessage());
            taskTracker.completeTask(taskId, "HATA"); // Telefona hata fırlat
        }
        System.out.println("=====================================================");
    }
}