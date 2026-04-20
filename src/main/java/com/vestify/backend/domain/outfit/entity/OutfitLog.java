package com.vestify.backend.domain.outfit.entity;

import com.vestify.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name= "outfit_logs")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutfitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "user_id", nullable=false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outfit_id", nullable = false)
    private Outfit outfit;

    @Column(name="worn_date", nullable=false)
    private LocalDate wornDate;

    // AI Eğitimi İçin Kritik
    @Column(name = "weather_condition")
    private String weatherCondition;

    @Column(name = "temperature")
    private Integer temperature;
}