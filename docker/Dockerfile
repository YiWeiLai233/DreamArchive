# 后端多阶段构建
# Stage 1: Maven 编译
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: 运行时
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/target/DreamArchive-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", \
  "-Xms512m", "-Xmx1024m", \
  "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", \
  "-jar", "app.jar", \
  "--spring.profiles.active=docker"]
