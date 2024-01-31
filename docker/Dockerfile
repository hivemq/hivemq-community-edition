# Additional build image to unpack the zip file and change the permissions without retaining large layers just for those operations
FROM busybox:1.36.1@sha256:6d9ac9237a84afe1516540f40a0fafdc86859b2141954b4d643af7066d598b74 AS unpack

ARG HIVEMQ_VERSION

COPY hivemq-ce-${HIVEMQ_VERSION}.zip /tmp/hivemq-ce.zip
RUN unzip /tmp/hivemq-ce.zip -d /opt \
    && mv /opt/hivemq-ce-${HIVEMQ_VERSION} /opt/hivemq
COPY config.xml /opt/hivemq/conf/config.xml
RUN chmod -R 770 /opt/hivemq \
    && chmod +x /opt/hivemq/bin/run.sh


# Actual image
FROM eclipse-temurin:11.0.22_7-jre-jammy@sha256:985ec5f9ff61ef9e24b2298ef902d773a1e1cb36693d62d0ac4d37e289ff595b

ARG HIVEMQ_GID=10000
ARG HIVEMQ_UID=10000

# Additional JVM options, may be overwritten by user
ENV JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseNUMA"

# Default allow all extension, set this to false to disable it
ENV HIVEMQ_ALLOW_ALL_CLIENTS=true

# Set locale
ENV LANG=en_US.UTF-8

# HiveMQ setup
COPY docker-entrypoint.sh /opt/docker-entrypoint.sh
COPY --from=unpack /opt/hivemq /opt/hivemq
RUN chmod +x /opt/docker-entrypoint.sh \
    && groupadd --gid ${HIVEMQ_GID} hivemq \
    && useradd -g hivemq -d /opt/hivemq -s /bin/bash --uid ${HIVEMQ_UID} hivemq \
    && chmod 770 /opt/hivemq

# Make broker data persistent throughout stop/start cycles
VOLUME /opt/hivemq/data

# Persist log data
VOLUME /opt/hivemq/log

# MQTT TCP listener: 1883
# MQTT Websocket listener: 8000
EXPOSE 1883 8000

WORKDIR /opt/hivemq

ENTRYPOINT ["/opt/docker-entrypoint.sh"]
CMD ["/opt/hivemq/bin/run.sh"]
