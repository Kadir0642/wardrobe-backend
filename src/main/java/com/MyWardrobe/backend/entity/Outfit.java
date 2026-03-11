package com.MyWardrobe.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity@Table(name="outfits")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Outfit {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String name; // Her kombinin mutlaka bir ismi olacak | "Kişlık Ofis Kombini", "Spor Salonu"


    // 1.İLİŞKİ: Bu kombin kime ait?
    @ManyToOne
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    // 2.İLİŞKİ: Bu kombinin içinde hangi kıyafetler var? (En önemli yer)
    @ManyToMany  //  Çoka Çok (N:M)" ilişkisi için araya üçüncü bir "Köprü Tablo" (Junction Table)
    @JoinTable( // Spring Boot ayağa kalkarken outfit_clothing_items adında üçüncü, bir tablo oluşacak.
            //Bu tablo sadece-> 3 numaralı kombin, 5 ve 8 numaralı kıyafetlerden oluşur." Bu sayede siyah tişörtünü istediğin kadar farklı kombine ekleyebileceksin!
            name="outfit_clothing_items", // Spring Boot bu isimde gizli bir köprü tablosu kuracak
            joinColumns = @JoinColumn(name= "outfit_id"),
            inverseJoinColumns= @JoinColumn(name="clothing_item_id")
    )
    private List<ClothingItem> clothingItems;

    private LocalDateTime createdAt; // Kıyafetin ne zaman oluşturulduğunu bize söyler

    @PrePersist
    protected void onCreate(){
        this.createdAt=LocalDateTime.now();
    }

}
