package com.vestify.backend.domain.social.repository;

import com.vestify.backend.domain.social.entity.SocialInteraction;
import com.vestify.backend.domain.social.enums.InteractionType;
import com.vestify.backend.domain.social.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialInteractionRepository extends JpaRepository<SocialInteraction, Long> {

    // Bir kombinin kaç beğeni aldığını çok hızlı saymak için
    long countByTargetIdAndTargetTypeAndInteractionType(Long targetId, TargetType targetType, InteractionType interactionType);

    // Kullanıcı bu kombini daha önce beğenmiş mi? (Çift beğenmeyi engellemek için)
    boolean existsByUserIdAndTargetIdAndTargetTypeAndInteractionType(Long userId, Long targetId, TargetType targetType, InteractionType interactionType);
    // Bir kullanıcının aynı fotoğrafı 10 kere beğenmesini engellemek için kullanılır. Eğer sonuç true gelirse, kullanıcıya "Zaten beğendiniz" diyebilirsin.
    // Veritabanında o kaydın var olup olmadığını (EXISTS) kontrol eder ve true veya false döner
}