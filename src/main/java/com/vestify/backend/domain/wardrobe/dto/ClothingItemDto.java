package com.vestify.backend.domain.wardrobe.dto;

import lombok.*;

@Getter @Setter
@Builder // Nesne oluşturmayı kolaylaştırır. Karmaşık ve çok sayıda parametresi olan nesneleri, okunabilir ve esnek bir Person.builder().name("..").build() yapısıyla oluşturmanızı sağlar
@NoArgsConstructor
@AllArgsConstructor
public class ClothingItemDto {// Entity sınıfların (veritabanı tabloları) çok ağırdır; içinde JPA bağlantıları (@ManyToOne), tarih bilgileri ve hassas veriler bulunur. DTO ise sadece mobil uygulamanın ekranda göstermeye ihtiyaç duyduğu "hafif ve temiz" verileri içeren bir kargo kutusudur.
    private Long id;
    private String name;
    private String brand; // Mobilde marka görmek önemlidir
    private String imageUrl; // Sadece temizlenmiş görsel (Cloudinary)
    private String category;
    private String season; // Hangi sezon? (Bunu mobilde badge/etiket olarak gösterebiliriz)
    private Double costPerWear;
    private Boolean isForSale; // Pazar yerinde mi? (Buna göre "Satılık" ibaresi koyulur)
}