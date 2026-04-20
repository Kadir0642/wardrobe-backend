package com.vestify.backend.domain.social.repository;

import com.vestify.backend.domain.social.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Kullanıcının bildirimlerini en yeniden en eskiye doğru sayfa sayfa getir
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    // Okunmamış bildirim sayısını getir (Uygulama ikonundaki kırmızı balon)
    long countByRecipientIdAndIsReadFalse(Long recipientId);
}