FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ENV TZ=Asia/Shanghai
EXPOSE 8080
# 只预留启动命令，jar靠外部挂载
ENTRYPOINT ["java","-Xms512M","-Xmx1G","-jar","/data/app.jar"]