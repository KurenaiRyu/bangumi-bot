FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache bash

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'
RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# We make four distinct layers so if there are application changes the library layers can be re-used
COPY build/libs/lib /deployments/lib
COPY build/libs/*.jar /deployments/
COPY entrypoint.sh /deployments/

EXPOSE 8080

WORKDIR /deployments

ENTRYPOINT ["bash", "/deployments/entrypoint.sh"]
