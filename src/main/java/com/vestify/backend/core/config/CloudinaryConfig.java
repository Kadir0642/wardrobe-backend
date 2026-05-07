package com.vestify.backend.core.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

//Spring tarafından yönetilen bir bean olarak yapılandırmak ve kaydetmek için kullanılır . Genellikle güvenli dosya yükleme, dönüştürme ve yönetimi sağlamak için uygulama özelliklerinden kimlik bilgilerini ( , , ) Cloudinary alır
@Configuration  //Backend ayağa kalkarken, Controller veya Service sınıflarına geçmeden önce ilk iş bu dosyayı bul ve içindeki ayarları sisteme yükle." Böylece Cloudinary motoru, en başından itibaren çalışmaya hazır hale gelir.
public class CloudinaryConfig {
    //Bağımlılık Enjeksiyonu: Bunu bir bağımlılık enjeksiyonu olarak tanımlayarak , dosya yüklemelerini işleyen servislere veya denetleyicilere nesneyi enjekte @Beanedebilirsiniz .@AutowireCloudinary
    //Güvenli Kimlik Doğrulama: API gizli anahtarı ve veri seti, sabit kod yerine güvenli ortam değişkenlerinden veya uygulama özelliklerinden yüklenmelidir.
    //İşlevsellik: Bu bean, resimler, videolar ve ham dosyaları yönetmek için Cloudinary API'si ile arka uçtan kimlik doğrulamalı etkileşime olanak tanır

    // 🚀 DÜZELTME: System.getenv yerine Spring'in @Value anotasyonunu kullanıyoruz.
    // Bu değerler application.yaml üzerinden güvenle gelecek.
    @Value("${cloudinary.cloud-name")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    // System.getenv doğrudan işletim sistemine (veya Docker konteynerine) bağlanıp şifreyi çeker.
    // Bu çalışır, evet. Ancak Spring Boot ekosisteminde "Best Practice" bu değildir.
    // Çünkü yarın bir gün test yazmak istediğinde veya "Dev", "Test", "Prod" gibi farklı ortamlar kurduğunda
    // System.getenv esnemez ve sistemi kilitler.

    // Inversion of Control (IoC - Kontrolün Tersine Çevrilmesi)
    //Normalde Java'da bir nesne lazım olduğunda her seferinde new Cloudinary() yazıp üretmek gerekir. Ancak @Bean dediğinde, bu nesneyi bir kez sen üretiyorsun ve Spring'in hafızasına (IoC Container) teslim ediyorsun.
    @Bean // "ImageUploadService" yazarken -> private final Cloudinary cloudinary; yazınca ve Spring bu Bean'i anında o servise bağlayacak (Dependency Injection).
    public Cloudinary cloudinary(){
        Map<String, String> config = new HashMap<>(); // Kayıtların şifrelerin güvenli bir şekilde saklanması, veri bütünlüğünün kontrolünün yapılması veya blockchain teknolojilerindeki verilerin saklanması bağlanmak için kullanılır
        // Gizli olan environment variables şifrelerini çekip Cloudinary motoruna veriyoruz.
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        return new Cloudinary(config);
    }
}
