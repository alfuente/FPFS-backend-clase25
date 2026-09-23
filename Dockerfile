# =====================================================================
# Clase 24 - SOLUCION - Dockerfile de ShopEasy
#   ./gradlew bootJar            (PowerShell: .\gradlew.bat bootJar)
#   docker build -t shopeasy .
#   docker run -d --name shopeasy -p 8080:8080 --network fpfs-backend_default \
#     -e "SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/shopeasy?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
#     shopeasy
# =====================================================================

# Parte de un computador que ya tiene Java 21 (JRE: solo ejecuta, no compila)
FROM eclipse-temurin:21-jre

# Mete adentro tu programa, con un nombre corto
COPY build/libs/shopeasy-backend-0.0.1-SNAPSHOT.jar app.jar

# Va a atender por la puerta 8080
EXPOSE 8080

# Cuando arranque, ejecuta esto
ENTRYPOINT ["java", "-jar", "app.jar"]
