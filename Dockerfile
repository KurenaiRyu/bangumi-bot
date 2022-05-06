FROM eclipse-temurin:17-jdk-alpine as actual-image
WORKDIR /app
COPY ./build/libs/*.jar ./bot.jar
# 修改为上海时区,不需要则删除
RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
ENTRYPOINT ["java", "-jar", "/app/bot.jar"]