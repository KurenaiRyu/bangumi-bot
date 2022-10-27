FROM eclipse-temurin:17.0.1_12-jdk-alpine

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'
RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# We make four distinct layers so if there are application changes the library layers can be re-used
COPY --chown=185 build/libs/lib/ /deployments/lib/
COPY --chown=185 build/libs/bangumi-bot.jar /deployments/

EXPOSE 8080
USER 185

WORKDIR /deployments

ENTRYPOINT ["java", "-jar", "-Dfile.encoding=UTF-8", "/deployments/bangumi-bot.jar"]