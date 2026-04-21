# 1. AŞAMA: Kodları Derleme (Mutfak)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Testleri atlayıp projeyi direkt .jar dosyasına çeviriyoruz
RUN mvn clean package -DskipTests

# 2. AŞAMA: Çalıştırma (Sunum)
# Java 21'in en hafif Alpine sürümünü kullanıyoruz
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
# İlk aşamada üretilen .jar dosyasını buraya kopyala
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]