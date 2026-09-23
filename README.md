# FPFS-backend · ShopEasy (punto de partida de la clase 25)

Backend de ShopEasy: Spring Boot 3.3, **Java 21**, MySQL. Se compila con **Gradle 8.14 (Kotlin DSL)**
usando el wrapper incluido: no hace falta instalar Gradle.

Basado en https://github.com/villavant-vant/FPFS-backend, con:

- `build.gradle.kts` y `settings.gradle.kts` en lugar de `pom.xml` (mismas dependencias).
- Las 98 pruebas en `src/test/` (JUnit 5, Mockito y H2 en memoria: no necesitan MySQL ni Docker).
- El `Dockerfile` de la clase 24.

## Requisitos

- JDK 21 (`java -version`).
- Docker Desktop, para la base de datos y la imagen.

## Compilar y probar

```bash
./gradlew build             # compila, corre las 98 pruebas, cobertura y .jar
./gradlew test              # solo las pruebas (informe: build/reports/tests/test/index.html)
./gradlew unitTest          # 24 pruebas @Tag("unit")
./gradlew integrationTest   # 74 pruebas @Tag("integration")
./gradlew bootJar           # solo el .jar ejecutable → build/libs/shopeasy-backend-0.0.1-SNAPSHOT.jar
```

En PowerShell: `.\gradlew.bat build`, etc. La cobertura queda en
`build/reports/jacoco/test/html/index.html`.

## Correr con Docker

```bash
docker compose up -d                  # MySQL (servicio "mysql", contenedor shopeasy-db)
./gradlew bootJar
docker build -t shopeasy .
docker run -d --name shopeasy -p 8080:8080 --network fpfs-backend-clase25_default \
  -e "SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/shopeasy?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
  shopeasy
```

La red se llama como la carpeta del proyecto más `_default`; si no coincide, revísala con
`docker network ls`. La API queda en http://localhost:8080/docs.
