package com.vestify.backend.domain.user.controller;

import com.vestify.backend.domain.user.entity.User;
import com.vestify.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

//Bu controller, kullanıcı kayıt işlemini güvenli, versiyonlanmış ve dış dünyaya şifre sızdırmayacak şekilde yönetiyor.

// Bu sınıf artık REST API (sistemlere JSON cevap verir.)
@RestController // Bu sınıfın bir API olduğunu ve gelen verileri JSON formatında alıp/göndereceğini belirtir.
@RequestMapping("/api/v1/users") // İnternetten bize ulaşacakları ana adres (v1= Versiyon 1)
@RequiredArgsConstructor // Adreslerin sonuna v1 (Versiyon 1) eklemek, İleride uygulamaya yeni bir kayıt sistemi (Örn: Google ile giriş) geldiğinde eski mobil uygulamayı güncellemeyen kullanıcıların v1 üzerinden sorunsuz çalışmaya devam etmesini sağlarsın.
public class UserController {

    // @RequestMapping -> Bu controller içindeki tüm metodların ana adresini belirler.
    // Yani bu metodlara ulaşmak için .../api/v1/users yolunu kullanmalısın.
    // Versiyonlama (v1) eklemek -> ileride API'da büyük değişiklikler yaparsan v2 diyerek eskiyi bozmadan devam edebilirsin.
    private final UserService userService;

    // Kullanıcı oluşturma gibi "yeni veri ekleme" işlemleri için HTTP POST metodunu kullanıyoruz.
    @PostMapping("/register") // Tam adresi: POST /api/v1/users/register
    public ResponseEntity<?> registerUser(@RequestBody User user){ // Gelen JSON paketini Java'daki User nesnesine otomatik olarak dönüştürür.
        User savedUser = userService.registerUser(user);

        // MİMARİ KURAL: Dışarıya User nesnesi (ve içindeki şifre) verilmez!
        // Sadece başarılı olduğunu belirten bir mesaj ve güvenli ID dönülür.
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(  // Map.of(...) --> kullanarak sadece ihtiyacımız olan alanları (mesaj, id, email) seçip dönüyorsun. Şifreyi asla dışarı sızdırmıyorsun.
                "message", "Kayıt başarılı!",
                "userId", savedUser.getId(),
                "email", savedUser.getEmail()
        )); // Artık şifreyi dışarı sızdırmıyoruz ve oluşturma işleminde 201 Created durum kodunu dönüyoruz.
    }
}