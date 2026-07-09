FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

COPY src ./src
# Maven Central가 CI 대량 트래픽에 간헐적 403(레이트리밋)을 반환 → 백오프 재시도.
# 같은 RUN 레이어라 앞선 시도에서 받은 의존성은 ~/.gradle에 남아 재시도는 증분(빠름).
RUN ./gradlew bootJar --no-daemon \
 || (echo "retry #1 (20s)" && sleep 20 && ./gradlew bootJar --no-daemon) \
 || (echo "retry #2 (45s)" && sleep 45 && ./gradlew bootJar --no-daemon)

FROM eclipse-temurin:25-jre
WORKDIR /app

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
