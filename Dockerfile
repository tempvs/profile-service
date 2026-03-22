FROM openjdk:25-ea-slim
COPY ./build/libs/*.jar /usr/app/
WORKDIR /usr/app
EXPOSE 8087
CMD exec java -Xms128m -Xmx512m -jar tempvs-profile.jar
