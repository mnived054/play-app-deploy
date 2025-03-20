# Start with an official Java image (OpenJDK)
FROM openjdk:11-jdk-slim AS builder

# Install sbt (Play’s build tool)
RUN apt-get update && apt-get install -y curl && \
    curl -L https://github.com/sbt/sbt/releases/download/v1.9.7/sbt-1.9.7.tgz | tar xz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/bin/sbt

# Set the working directory
WORKDIR /app

# Copy your project files
COPY . .

# Build and stage the app
RUN sbt stage

# Final runtime image
FROM openjdk:11-jre-slim

# Set working directory
WORKDIR /app

# Copy the staged app from the builder
COPY --from=builder /app/target/universal/stage /app

# Expose the port (Render will override this with $PORT)
EXPOSE 9000

# Start the app, using Render’s PORT variable
CMD ["sh", "-c", "bin/play-app-deploy -Dhttp.port=$PORT"]