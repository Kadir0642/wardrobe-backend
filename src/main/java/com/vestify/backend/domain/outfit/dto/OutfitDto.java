package com.vestify.backend.domain.outfit.dto;

import com.vestify.backend.domain.wardrobe.dto.ClothingItemDto;
import lombok.*;
import java.util.Set; // List yerine Set kullanıyoruz

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutfitDto { // Kombin içindeki kıyafetlerin listesidir.
    private Long id;
    private String name;
    private String outfitImageUrl; // Ekledik: AR ile oluşturulan sonuç görseli buraya gelecek!
    private Set<ClothingItemDto> clothes; // İçinde sadece temizlenmiş DTO kıyafetler var. | React tarafında veya farklı yer olsun fark etmez. Kombin kıyafetlerini clothes adında set liste ile yolluyoruz.
}