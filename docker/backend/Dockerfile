# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-22 AS build
WORKDIR /build

COPY singleton/pom.xml singleton/pom.xml
COPY singleton/application/pom.xml singleton/application/pom.xml
COPY singleton/common/pom.xml singleton/common/pom.xml
COPY singleton/auth/pom.xml singleton/auth/pom.xml
COPY singleton/comfyui/pom.xml singleton/comfyui/pom.xml
COPY singleton/llm/pom.xml singleton/llm/pom.xml
COPY singleton/notice/pom.xml singleton/notice/pom.xml
COPY singleton/oss/pom.xml singleton/oss/pom.xml
COPY singleton/system/pom.xml singleton/system/pom.xml

RUN mvn -f singleton/pom.xml dependency:go-offline -B -Pprod

COPY singleton/ singleton/
RUN mvn -f singleton/pom.xml -pl application -am clean package -DskipTests -Pprod

FROM eclipse-temurin:22-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app app
COPY --from=build /build/singleton/application/target/application-*.jar /app/app.jar
RUN chown -R app:app /app
USER app

EXPOSE 9000

ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
