package com.vestify.backend.domain.user.service;

import com.vestify.backend.domain.user.entity.User;
import com.vestify.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional // MİMAR KURALI: Veritabanına yazma yapılan her işlem Transactional olmalıdır.
    public User registerUser(User user) {  // @Transactional -> Bu "Ya hep ya hiç" (All or Nothing) kuralıdır. İşlem yarıda kesilirse veritabanı eski haline döner (Rollback).
        log.info("Yeni kayıt denemesi: {}", user.getEmail());

        // Kullanıcı aktif/pasif fark etmeksizin email benzersiz olmalı
        if (userRepository.findByEmailAndIsActiveTrue(user.getEmail()).isPresent()) {
            log.warn("Kayıt reddedildi: Email zaten kullanımda ({})", user.getEmail());
            throw new RuntimeException("Bu e-posta adresi zaten kullanımda!");
        }

        user.setIsPublic(false);
        user.setIsActive(true); // Soft delete bayrağı varsayılan olarak açık

        log.info("Kullanıcı başarıyla oluşturuldu: {}", user.getEmail());
        return userRepository.save(user);
    }
}