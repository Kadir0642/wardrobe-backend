package com.MyWardrobe.backend.controller;

import com.MyWardrobe.backend.entity.User;
import com.MyWardrobe.backend.service.UserService;
import lombok.RequiredArgsConstructor; // Dependecy Injection
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Spring'e "Bu sınıf dış dünyaya açılan bir API kapısı" der.
@RestController // Bu sınıf artık REST API (sistemlere JSON cevap verir.)
@RequestMapping("/api/v1/users") // İnternetten bize ulaşacakları ana adres (v1= Versiyon 1)
@RequiredArgsConstructor // Adreslerin sonuna v1 (Versiyon 1) eklemek, İleride uygulamaya yeni bir kayıt sistemi (Örn: Google ile giriş) geldiğinde eski mobil uygulamayı güncellemeyen kullanıcıların v1 üzerinden sorunsuz çalışmaya devam etmesini sağlarsın.
public class UserController {
    // Controller sadece Service ile konuşur, veritabanına (repository) doğrudan erişememeli! (Mimari Kural!) Controller->Service->Repository->Service->Controller-> Kullanıcı

    private final UserService userService; // UserService bağımlılığını otomatik olarak (constructer injection ile) bu sınıfa bağlıyoruz

    // Mobil uygulamadan gelen "Kayıt Ol" formunu (JSON verisini) karşılayan metod
    // http://localhost:8080/api/v1/users/register | ResponseEntity<User> HTTP  yanıtını ve nesne türünü temsil eder
    @PostMapping("/register") // ENDPOINT -> Dış dünyaya açılan tuş gibi |Dışarıdan veri aldığımız,sunucuya veri gönderdiğimiz için POST
    public ResponseEntity<User> registerUser(@RequestBody User user){  // Gelen verileri user nesnesi içine doldurur.

        // Gelen veriyi (email, şifre, vs) ,işlem yapması için UserService'e yolluyoruz
        User savedUser=userService.registerUser(user);

        // İşlem başarılıysa mobil uygulamaya "200 OK" koduyla beraber kaydedilen veriyi gönderiyoruz.
        return ResponseEntity.ok(savedUser);
    }
}
