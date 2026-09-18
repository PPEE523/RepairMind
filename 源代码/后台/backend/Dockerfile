FROM eclipse-temurin:17-jre

# 安装 ffmpeg（视频截帧用），Docker 层缓存：只要基础镜像不变就不会重新下载
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*

WORKDIR /app
ENV TZ=Asia/Shanghai
EXPOSE 8080
# 只预留启动命令，jar靠外部挂载
ENTRYPOINT ["java","-Xms512M","-Xmx1G","-jar","/data/app.jar"]