# ================================================================
# Stage 1: 빌드
# ================================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 의존성 캐시 (pom.xml 변경 시에만 재다운로드)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# 소스 빌드
COPY src ./src
RUN mvn package -DskipTests -q

# ================================================================
# Stage 2: 실행
# ================================================================
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8443

ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar"]
