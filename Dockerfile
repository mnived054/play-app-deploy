FROM apache/beam_java17_sdk AS builder
#openjdk:11
ARG SBT_VERSION=1.9.9

RUN mkdir /working/ && \
    cd /working/ && \
    curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" >> /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add - && \
    apt-get update && \
    apt-get install -y sbt=$SBT_VERSION && \
    rm sbt-$SBT_VERSION.deb && \
    cd && \
    rm -r /working/ && \
    sbt sbtVersion


WORKDIR /app

COPY . /app

RUN sbt clean stage

FROM openjdk:17-slim
WORKDIR /app

COPY --from=builder /app/target/universal/stage /app

EXPOSE 9000

# CMD sbt run #klk
CMD ["/bin/sh","-c", "bin/play-app-deploy -Dhttp.port=${PORT:-9000}"]