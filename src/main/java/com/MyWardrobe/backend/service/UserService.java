package com.MyWardrobe.backend.service;

import com.MyWardrobe.backend.entity.User;
import com.MyWardrobe.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service // İş mantığını yöneten sınıf | Uygulama beyni
@RequiredArgsConstructor // Lombok  sayesinde Repository'yi otomatik bağlıyoruz.(Dependency Injection )
public class UserService {

    // @RequiredArgsConstructor ve private final: Bu ikili, mimaride meşhur Dependency Injection prensibinin en modern halidir.
    // UserService'in çalışmak için UserRepository'ye (köprüye) ihtiyacı var.
    // final anahtar kelimesi ve Lombok sayesinde, Spring bu köprüyü servisimize otomatik olarak güvenle bağlar.
    private final UserRepository userRepository;

    //Kullanıcı Kayıt İşlemi (Business Logic)
    public User registerUser(User user){

        // 1.KURAL: Aynı E-posta ile iki kişi kayıt olamaz!
        if(userRepository.findByEmail(user.getEmail()).isPresent()){ // Veritabanına bu Emailde biri varmı, Kullanıcı varmı?(isPresent)
            throw new RuntimeException("Bu e-posta adresi zaten kullanımda! Başka bir e-posta deneyin."); // Kullanıcı varsa uygulamayı çökertmeden RunTimeException fırlatarak işlemi durdurur.
        }

        // 2.KURAL: Şifre Hasleme (Güvenlik)
        // NOT: İLeride Spirng Security (BCrypt) kütüphanesi ekledğimizde burayı aktif edeceğiz.
        // user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 3.KURAL: Yeni kullanıcı ilk kayıt olduğunda dolabı gizli (private) başlasın
        user.setPublic(false);

        // Her şey kurallara uygunsa, Repository (köprü) "Bunu veritabanına kaydet" emri verir
        System.out.println("Yeni kullanıcı Supabase'e kaydediliyor: "+user.getEmail());
        return userRepository.save(user);
    }

}
