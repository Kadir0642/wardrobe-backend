package com.vestify.backend.domain.vton.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.vestify.backend.core.config.RabbitMQConfig;
import com.vestify.backend.domain.vton.dto.VtonTaskMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

@Service
@RequiredArgsConstructor
public class MockVtonWorker {

    private final VtonTaskTracker taskTracker;

    // Kuyruğu dinleyen metod. RabbitMQ'ya mesaj düştüğü an burası otomatik tetiklenir!
    @RabbitListener(queues = RabbitMQConfig.VTON_QUEUE)
    public void processVtonTask(VtonTaskMessage message) {

        // 🚀 İŞTE DÜZELTİLEN SATIR: getTaskId() yerine getRequestId() kullanıyoruz!
        String taskId = message.getRequestId();

        System.out.println("=====================================================");
        System.out.println("🚀 [RABBITMQ WORKER] YENI GOREV ALINDI!");
        System.out.println("Task ID: " + taskId);
        System.out.println("Yapay Zeka Giydirme Islemi Baslatiliyor (Mock)...");
        System.out.println("=====================================================");

        try {
            // Fal.ai veya OOTDiffusion API'sini taklit ediyoruz (5 saniye bekletiyoruz)
            Thread.sleep(5000);

            // 🚀 İŞLEM BİTTİ! Sonucu takip merkezine şık bir sahte görselle kaydediyoruz
            String mockResultImage = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=600";

            taskTracker.completeTask(taskId, mockResultImage);

            System.out.println("✅ [RABBITMQ WORKER] ISLEM BASARIYLA TAMAMLANDI!");
            System.out.println("=====================================================");

            // İLERİDE: Burada çıkan sonucu Supabase'e kaydedip telefona bildirim atacağız.

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}