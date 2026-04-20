package com.vestify.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

//AI'ı eğitecek ve "En çok giyilenler" istatistiğini tutacak olan kayıt tablosu burasıdır.
@Entity
@Table(name= "outfit_logs") // Giyme Geçmişi / Takvim Tablosu
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutfitLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EAGER -> N+1 Sorgu Problemi | sadece 5 satırlık bir "Giyme Geçmişi" göstermek için veritabanındaki tüm tabloları RAM'e indirebilir
    @ManyToOne(fetch = FetchType.LAZY) // LAZY -> Sadece istenen tabloyu (LOG) getirir
    @JoinColumn(name= "user_id", nullable=false)
    private User user;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name = "outfit_id",nullable = false)
    private Outfit outfit; // Hangi kombin giyildi ?

    @Column(name="worn_date",nullable=false)
    private LocalDate wornDate; // Hangi TARİHTE giyildi ? (Takvim entegrasyonu için)

    // --- AI EĞİTİM VERİLERİ (Dataset) ---
    @Column(name = "weather_conditon")
    private String weatherCondition; // Örn: "Güneşli", "Yağmurlu" (Mobilden hava durumunu çekip buraya yazacağız)

    @Column(name = "temperature")
    private Integer temperature; // Örn: 18 (Derece)

}
