package com.vestify.backend.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 1. ŞİFRELEME MOTORU: Tüm sisteme BCrypt'i tanıtıyoruz.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. GÜMRÜK KAPISI: Şimdilik geliştirme aşamasında olduğumuz için gelen tüm isteklere izin veriyoruz.
    // İleride Login/Register bitince burayı "Sadece yetkili kişiler girebilir" diye güncelleyeceğiz.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // API testleri (Postman vb.) için CSRF'i kapatıyoruz
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}