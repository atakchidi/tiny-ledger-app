FROM eclipse-temurin:21-jdk AS build

WORKDIR /build

COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
RUN ./gradlew --no-daemon --console=plain dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon --console=plain installDist

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /build/build/install/ledger ./

ENV PORT=80
ENV TZ=Europe/Riga
EXPOSE 80

ENTRYPOINT ["bin/ledger"]
