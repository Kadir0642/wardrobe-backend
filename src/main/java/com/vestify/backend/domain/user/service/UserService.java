package com.vestify.backend.domain.user.service;

import com.vestify.backend.domain.user.entity.User;
import com.vestify.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder; // YENİ IMPORT
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Bu servis, uygulamanın "Kullanıcı Kabul ve Kayıt" kapısıdır.

@Service
@RequiredArgsConstructor
@Slf4j // "Yeni kayıt denemesi" gibi kritik bilgileri konsola yazmanı sağlar. Bu, hata ayıklarken (debugging) hayat kurtarır.
public class UserService {

    // Kullanıcı bilgilerini (email, şifre vb.) veritabanında sorgulamak ve saklamak için kullanılan köprüdür.
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Şifreleme motoru

    @Transactional // MİMAR KURALI: Veritabanına yazma yapılan her işlem Transactional olmalıdır.
    public User registerUser(User user) {  // @Transactional -> Bu "Ya hep ya hiç" (All or Nothing) kuralıdır. İşlem yarıda kesilirse veritabanı eski haline döner (Rollback).
        log.info("Yeni kayıt denemesi: {}", user.getEmail());

        // Kullanıcı aktif/pasif fark etmeksizin email benzersiz olmalı
        if (userRepository.findByEmailAndIsActiveTrue(user.getEmail()).isPresent()) {
            log.warn("Kayıt reddedildi: Email zaten kullanımda ({})", user.getEmail());
            throw new RuntimeException("Bu e-posta adresi zaten kullanımda!");
        }

        // Şifreyi veritabanına gitmeden hemen önce BCrypt ile maskele!
        // Örn: "123456" -> "$2a$10$xyz123abc456..." gibi geri döndürülemez bir metne dönüşür.
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        user.setIsPublic(false); // Kullanıcı profili varsayılan olarak gizli başlar.
        user.setIsActive(true); // Kayıt anında true yapılır. | Soft delete bayrağı varsayılan olarak açık

        // Kayıt olan herkese varsayılan "USER" rolünü veriyoruz!
        user.setRole(com.vestify.backend.domain.user.enums.Role.USER);

        log.info("Kullanıcı başarıyla oluşturuldu: {}", user.getEmail());
        return userRepository.save(user);
    }
}