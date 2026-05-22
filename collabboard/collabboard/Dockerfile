# Stage 1: Use an official OpenJDK 17 image to build the application
FROM eclipse-temurin:17-jdk-jammy AS build

# Set the working directory
WORKDIR /app

# Copy the Maven wrapper files
COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .

# Add executable permissions to the Maven wrapper
RUN chmod +x mvnw

# Download dependencies to leverage Docker layer caching
RUN ./mvnw dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Build the application, creating the executable JAR file.
# This will use ServerApplication as the main class, as configured in pom.xml.
RUN ./mvnw clean package -DskipTests


# Stage 2: Create the final, smaller runtime image
FROM eclipse-temurin:17-jre-jammy

# THIS IS THE FIX: Install the missing graphics libraries required by JavaFX.
# These are needed even if a UI is not displayed.
RUN apt-get update && apt-get install -y libx11-6 libxtst6 libxi6 libxrender1 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built JAR file from the 'build' stage
COPY --from=build /app/target/collabboard-0.0.1-SNAPSHOT.jar app.jar

# Expose the port the application will run on
EXPOSE 8080

# The command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]

