package com.vestify.backend.domain.outfit.repository;

import com.vestify.backend.domain.outfit.entity.Outfit;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;


@Repository
public interface OutfitRepository extends JpaRepository<Outfit, Long> {

    // N+1 Problemi Çözümü: Outfit çekilirken içindeki clothingItems'ları da tek sorguda getir!
    @EntityGraph(attributePaths = {"clothingItems"}) // Kaç kombin olursa olsun veritabanına sadece 1 kez gidilir.
    Page<Outfit> findByUserId(Long userId, Pageable pageable); // SELECT * FROM outfits WHERE user_id = ?

    //Mimarın Notu: @EntityGraph ekledim. Bu sayede bir kullanıcının kombinini çektiğinde, o kombinin içindeki kıyafetleri tek tek (N+1 problemi) çekmek yerine,
    // tek bir SQL JOIN sorgusuyla hepsini tek seferde getirir. İnanılmaz bir performans artışı sağlar.

    // 1. Sorgu: Kullanıcının tüm kombinlerini getirir. (SELECT * FROM outfits WHERE user_id = ...)
    // N. Sorgu (N=10): Her bir kombin için, o kombine ait kıyafetleri veritabanından tek tek çeker. (1. kombin için SELECT..., 2. kombin için SELECT...)
    // Sonuç: 1 (ana sorgu) + 10 (alt detaylar) = 11 sorgu.
    //Eğer kullanıcının 100 kombini olsaydı, veritabanına tam 101 kez gitmek zorunda kalacaktın.
    // Bu da uygulamanın yavaşlamasına ve veritabanının yorulmasına sebep olur. İşte buna N+1 Problemi diyoruz


    //Tek bir JOIN sorgusuyla "Kullanıcı 5'in tüm kombinlerini ve o kombinlere ait tüm kıyafetleri paket yap getir" diyor
    //Performans: Veritabanı trafiği azalır.
    //Hız: Uygulama çok daha hızlı cevap verir.
    //Kaynak Yönetimi: Sunucun gereksiz yere SQL üretmekle uğraşmaz.

    // Birebir eşleşen tipi getir (Örn: Sadece LOOKBOOK'lar)
    Page<Outfit> findByUserIdAndType(Long userId, String type, Pageable pageable);

    //  Belirtilen tipte OLMAYANLARI veya tipi NULL olanları getir (Eski verileri kaybetmemek için)
    @Query("SELECT o FROM Outfit o WHERE o.user.id = :userId AND (o.type IS NULL OR o.type != :type)")
    Page<Outfit> findByUserIdAndTypeNotOrTypeIsNull(@Param("userId") Long userId, @Param("type") String type, Pageable pageable);

}