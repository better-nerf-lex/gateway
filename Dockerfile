FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-al2023-headless AS builder
WORKDIR /app
RUN yum update -y && yum install -y tar gzip
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-al2023-headless
WORKDIR /app
RUN yum update -y && \
    yum install -y shadow-utils && \
    groupadd -r appuser && \
    useradd -r -g appuser appuser && \
    yum clean all

COPY --from=builder /app/target/gateway-*.jar ./app.jar
RUN chown appuser:appuser /app/app.jar
USER appuser
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE=docker
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
