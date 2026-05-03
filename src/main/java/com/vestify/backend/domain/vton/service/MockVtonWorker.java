package com.vestify.backend.domain.vton.service;

import com.vestify.backend.core.config.RabbitMQConfig;
import com.vestify.backend.domain.vton.dto.VtonTaskMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class MockVtonWorker {

    // Kuyruğu dinleyen metod. RabbitMQ'ya mesaj düştüğü an burası otomatik tetiklenir!
    @RabbitListener(queues = RabbitMQConfig.VTON_QUEUE)
    public void processVtonTask(VtonTaskMessage message) {
        System.out.println("=====================================================");
        System.out.println("🚀 [RABBITMQ WORKER] YENI GOREV ALINDI!");
        System.out.println("Takip ID : " + message.getRequestId());
        System.out.println("Kullanici: " + message.getUserId());
        System.out.println("Kiyafet Sayisi: " + message.getGarmentImageUrls().size());
        System.out.println("Yapay Zeka Giydirme Islemi Baslatiliyor (Mock)...");
        System.out.println("=====================================================");

        try {
            // Fal.ai veya OOTDiffusion API'sini taklit ediyoruz (5 saniye bekletiyoruz)
            Thread.sleep(5000);

            // 5 Saniye sonra işlem başarıyla bitti kabul ediyoruz.
            System.out.println("✅ [RABBITMQ WORKER] ISLEM BASARIYLA TAMAMLANDI!");
            System.out.println("Takip ID : " + message.getRequestId() + " icin sonuc hazir.");
            System.out.println("=====================================================");

            // İLERİDE: Burada çıkan sonucu Supabase'e kaydedip telefona bildirim atacağız.

        } catch (InterruptedException e) {
            System.err.println("Worker islemi sirasinda hata: " + e.getMessage());
        }
    }
}