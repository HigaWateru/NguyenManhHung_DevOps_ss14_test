# TODO 1: Chọn image nền phù hợp với Java 21 của dự án
FROM eclipse-temurin:21-jdk-alpine

# TODO 2: Khai báo thư mục làm việc trong container
WORKDIR /app

# TODO 3: Sao chép file JAR đã build vào image
COPY build/libs/*.jar app.jar

# TODO 4: Khai báo cổng ứng dụng Spring Boot
EXPOSE 8080

# TODO 5: Khởi chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
