FROM public.ecr.aws/amazoncorretto/amazoncorretto:21.0.3-al2023-headless AS builder
WORKDIR /app

# Install tar and gzip required for mvnw
RUN yum update -y && yum install -y tar gzip

# Copy project files and run Maven
COPY src ./src
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw clean package -DskipTests

#Runtime
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21.0.3-al2023-headless
ENV JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
WORKDIR /app
COPY --from=builder /app/target/market-intelligence-service-*.jar ./app.jar
ENV SPRING_PROFILES_ACTIVE=cloud
CMD ["java", "-jar", "app.jar"]


