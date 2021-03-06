FROM openjdk:8-jdk-alpine as build-env
WORKDIR /building/
COPY ./ ./
RUN ./gradlew clean check build -x test

FROM openjdk:8-jre-alpine
RUN apk --no-cache add ca-certificates
RUN mkdir /root/.aws

ADD credentials /root/.aws/credentials

WORKDIR /running/
COPY --from=build-env /building/build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT java -jar app.jar