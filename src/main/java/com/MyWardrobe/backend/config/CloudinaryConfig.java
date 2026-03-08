package com.MyWardrobe.backend.config;

import com.cloudinary.Cloudinary;
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

    // Inversion of Control (IoC - Kontrolün Tersine Çevrilmesi)
    //Normalde Java'da bir nesne lazım olduğunda her seferinde new Cloudinary() yazıp üretmek gerekir. Ancak @Bean dediğinde, bu nesneyi bir kez sen üretiyorsun ve Spring'in hafızasına (IoC Container) teslim ediyorsun.
    @Bean // "ImageUploadService" yazarken -> private final Cloudinary cloudinary; yazınca ve Spring bu Bean'i anında o servise bağlayacak (Dependency Injection).
    public Cloudinary cloudinary(){
        Map<String, String> config = new HashMap<>();
        // Gizli olan environment variables şifrelerini çekip Cloudinary motoruna veriyoruz.
        config.put("cloud_name", System.getenv("CLOUDINARY_CLOUD_NAME"));
        config.put("api_key", System.getenv("CLOUDINARY_API_KEY"));
        config.put("api_secret", System.getenv("CLOUDINARY_API_SECRET"));
        return new Cloudinary(config);
    }
}
