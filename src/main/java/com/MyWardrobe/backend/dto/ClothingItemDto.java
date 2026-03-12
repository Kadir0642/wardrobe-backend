package com.MyWardrobe.backend.dto;

import lombok.Builder;
import lombok.Data;

// Lombok anotasyonları
@Data // Getter - (final olmayan)Setter metotları oluşturur.
@Builder // Nesne oluşturmayı kolaylaştırır. Karmaşık ve çok sayıda parametresi olan nesneleri, okunabilir ve esnek bir Person.builder().name("..").build() yapısıyla oluşturmanızı sağlar
public class ClothingItemDto { // Entity sınıfların (veritabanı tabloları) çok ağırdır; içinde JPA bağlantıları (@ManyToOne), tarih bilgileri ve hassas veriler bulunur. DTO ise sadece mobil uygulamanın ekranda göstermeye ihtiyaç duyduğu "hafif ve temiz" verileri içeren bir kargo kutusudur.
    // Sadece mobil uygulamanın ekranda göstereceği "vitrin" bilgileri!
    // Bu tekil bir "şablon" veya "vitrin"dir.
    private Long id;
    private String name;
    private String imageUrl;
    private String category;
    private Double costPerWear; // Hesaplanan giyme başına maliyet
}
