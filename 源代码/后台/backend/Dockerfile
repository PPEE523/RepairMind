FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests -Dspring-boot.repackage.skip=true package \
    && mvn -B dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /build/target/*.jar /app/app.jar
COPY --from=build /build/target/lib /app/lib
EXPOSE 8080
ENTRYPOINT ["java","-Xms512M","-Xmx1G","-cp","/app/app.jar:/app/lib/*","com.example.equipment.EquipmentRepairApplication"]