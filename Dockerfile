FROM eclipse-temurin:17-jre-focal

RUN apt update && apt install tzdata -y
ENV TZ="Asia/Shanghai"
ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'

COPY build/libs/lib /deployments/lib
COPY build/libs/*.jar /deployments/
COPY entrypoint.sh /deployments/

EXPOSE 8080

WORKDIR /deployments

ENTRYPOINT ["bash", "/deployments/entrypoint.sh"]
