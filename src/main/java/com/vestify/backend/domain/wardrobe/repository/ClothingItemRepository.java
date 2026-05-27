package com.vestify.backend.domain.wardrobe.repository;

import com.vestify.backend.domain.wardrobe.entity.ClothingItem;
import com.vestify.backend.domain.wardrobe.enums.ItemCondition;
import com.vestify.backend.domain.wardrobe.enums.ItemSeason;
import com.vestify.backend.domain.wardrobe.enums.ItemStatus;
import com.vestify.backend.domain.wardrobe.enums.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClothingItemRepository extends JpaRepository<ClothingItem, Long> {

    // Instagram'daki "Sonsuz Kaydırma" (Infinite Scroll) mantığını kurmalıyız.
    // Repository'lerimiz List yerine Page (Sayfa) dönmeli ve içine Pageable parametresi almalıdır.
    // Böylece veriyi "20'şer 20'şer" çekeriz.

    // 1. DOLAP SORGUSU: Silinmemiş kıyafetleri sayfa sayfa getir.
    Page<ClothingItem> findByUserIdAndStatusNot(Long userId, ItemStatus status, Pageable pageable);

    // AI'ın tüm dolabı görebilmesi için (Sayfalama olmadan, sadece silinmemişleri getir)
    // List<ClothingItem> realWardrobeItems = clothingItemRepository.findByUserIdAndStatusNot(request.getUserId(), ItemStatus.DELETED);
    List<ClothingItem> findByUserIdAndStatusNot(Long userId, ItemStatus status);

    // AI'ın gardroptakileri sadece görmesi için
    // Sadece ve sadece kullanıcının şu an aktif olarak dolabında duran kıyafetleri getir!
    //List<ClothingItem> realWardrobeItems = clothingItemRepository.findByUserIdAndStatus(request.getUserId(), ItemStatus.WARDROBE);
    List<ClothingItem> findByUserIdAndStatus(Long userId, ItemStatus status);

    // 2. KATEGORİ SORGUSU: Çapa modeli için (Yine silinmemişleri ve sayfa sayfa)
    Page<ClothingItem> findByUserIdAndCategoryAndStatusNot(Long userId, String category, ItemStatus status, Pageable pageable);

    // 3. KEŞFET (FEED) SORGUSU: | (GÜVENLİ) HALİ:
    // Sosyal ağ için sadece paylaşılabilir ve silinmemiş olan ve AI tarafından ONAYLANMIŞ olanları getir!
    Page<ClothingItem> findByIsSharableTrueAndStatusNotAndModerationStatusOrderByCreatedAtDesc(
            ItemStatus status,
            ModerationStatus moderationStatus, // Parametre olarak ModerationStatus.APPROVED vereceğiz
            Pageable pageable
    );
    // 4. ANALİZ SORGUSU: En çok giyilen (Tek veri döneceği için sayfalama gerekmez)
    ClothingItem findFirstByUserIdAndStatusNotOrderByWearCountDesc(Long userId, ItemStatus status);

    // FİLTRELEME MOTORU
    // Kullanıcı uygulamada "Kışlık, Siyah, L beden ve durumu Yeni olan pantolonlarımı göster" dediğinde; bu filtreleme motoru tüm bu şartları birleştirir ve veritabanından sadece bu tanıma uyan parçaları cımbızla çeker gibi getirir.
    // Eğer herhangi bir filtreyi boş bırakırsa (örneğin renk seçmezse), sistem o kriteri görmezden gelerek diğerlerine göre sonuç vermeye devam eder
    // Enum'lar artık LIKE ile değil, doğrudan eşleşme (=) ile aranıyor.
    @Query("SELECT c FROM ClothingItem c WHERE c.user.id = :userId " +
            "AND c.status != 'DELETED' " + // Silinenleri asla filtrelemeye sokma!
            "AND (:category IS NULL OR c.category = :category) " +
            "AND (:subCategory IS NULL OR c.subCategory = :subCategory) " +
            "AND (:season IS NULL OR c.season = :season) " +
            "AND (:color IS NULL OR c.color LIKE CONCAT('%', :color, '%')) " +
            "AND (:size IS NULL OR c.size = :size) " +
            "AND (:condition IS NULL OR c.condition = :condition)") // başına ve sonuna % işareti eklenerek "içinde geçiyor mu?"
    Page<ClothingItem> filterUserWardrobe(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("subCategory") String subCategory,
            @Param("season") ItemSeason season,
            @Param("color") String color,
            @Param("size") String size,
            @Param("condition") ItemCondition condition,
            Pageable pageable // Akıllı filtreleme artık sayfa sayfa çalışıyor!
    );
}