package com.vestify.backend.domain.outfit.dto;

import com.vestify.backend.domain.outfit.enums.FeedbackReason;
import com.vestify.backend.domain.outfit.enums.FeedbackType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;


// Jackson : JSON --> Java Nesnesine dönüşümünü sağlayan kütüphanedir
// Java dünyasında JSON dendiğinde akla gelen ilk isimdir. Jackson, bir "Tercüman" (Library) gibidir.
//Görevi: Mobil uygulamadan gelen o süslü parantezli karmaşık JSON metnini alır, senin Java'da yazdığın @Data eklediğin sınıfa (Class) otomatik olarak yerleştirir.
//Neden @NoArgsConstructor Şart? Jackson çok "gelenekçidir". Bir nesne oluştururken önce kapıyı çalar ve "İçeride kimse olmayan boş bir oda var mı?" diye sorar.
// İşte o boş oda senin NoArgsConstructor'ındır. O olmazsa içeri giremez ve hata verir

@Getter @Setter
@NoArgsConstructor // JSON'ın bu sınıfı okuyabilmesi için | Jackson, önce boş bir nesne oluşturur, sonra setter metodları veya reflection ile alanları doldurur. Bu constructor olmazsa "Cannot construct instance" hatasıyla karşılaşırsın
@AllArgsConstructor // Testlerde veya manuel nesne üretiminde hızlıca tüm alanları doldurmanı sağlar.
public class OutfitFeedbackDto {
    // Özetle: Şu an mobil uygulamanın anlayacağı dilden (JSON) Java'nın anlayacağı dile (POJO) bir köprü kuruyorsun.
    // Spring Boot ve Jackson da bu işin hamallığını yapıyor

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("outfit_item_ids")
    private List<Long> outfitItemIds; // O an ekranda olan kombindeki tüm eşyalar

    @JsonProperty("feedback_type")
    private FeedbackType feedbackType; // LIKE, DISLIKE, WORE_IT

    @JsonProperty("reason_code")
    private FeedbackReason reasonCode; // Neden beğenmedi?

    @JsonProperty("target_item_ids")
    private List<Long> targetItemIds;  // Sadece şikayet ettiği spesifik eşyalar (varsa)

    @JsonProperty("weather_context")
    private String weatherContext;  // "42°C, Konya Selçuklu"
}

// API Request (Payload): Mobilden sunucuya gönderilen "sipariş" paketidir.
//API Response: Sunucunun mobile döndüğü "yanıt" paketidir.
//DTO (Data Transfer Object): Bu sınıfların genel adıdır. Veritabanındaki tablolarını (Entity) doğrudan dışarı açmak yerine,
// sadece ihtiyacın olan veriyi taşımak için bu "DTO" yani POJO (Sade Eski Java Nesnesi) sınıflarını kullanırsın