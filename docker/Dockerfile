# We use multi-stage here to unzip in an initial layer so we don't have to COPY and then RUN unzip (two layers). ADD can lead to larger layers as well.
FROM busybox:1.35.0@sha256:02289a9972c5024cd2f083221f6903786e7f4cb4a9a9696f665d20dd6892e5d6 AS unpack

ARG HIVEMQ_VERSION

COPY hivemq-ce-${HIVEMQ_VERSION}.zip /tmp/hivemq-ce.zip
RUN unzip /tmp/hivemq-ce.zip -d /opt \
    && chgrp -R 0 /opt \
    && chmod -R 770 /opt

FROM eclipse-temurin:11.0.22_7-jre-jammy@sha256:985ec5f9ff61ef9e24b2298ef902d773a1e1cb36693d62d0ac4d37e289ff595b

ARG HIVEMQ_VERSION

ARG HIVEMQ_GID=10000
ARG HIVEMQ_UID=10000

# Additional JVM options, may be overwritten by user
ENV JAVA_OPTS "-XX:+UnlockExperimentalVMOptions -XX:+UseNUMA"

# Default allow all extension, set this to false to disable it
ENV HIVEMQ_ALLOW_ALL_CLIENTS "true"

# Set locale
ENV LANG=en_US.UTF-8

# HiveMQ setup
COPY --from=unpack /opt/hivemq-ce-${HIVEMQ_VERSION} /opt/hivemq-ce-${HIVEMQ_VERSION}
COPY config.xml /opt/hivemq-ce-${HIVEMQ_VERSION}/conf/config.xml
COPY docker-entrypoint.sh /opt/docker-entrypoint.sh
RUN ln -s /opt/hivemq-ce-${HIVEMQ_VERSION} /opt/hivemq \
    && groupadd --gid ${HIVEMQ_GID} hivemq \
    && useradd -g hivemq -d /opt/hivemq -s /bin/bash --uid ${HIVEMQ_UID} hivemq \
    && chgrp 0 /opt/hivemq-ce-${HIVEMQ_VERSION}/conf/config.xml \
    && chmod 770 /opt/hivemq-ce-${HIVEMQ_VERSION}/conf/config.xml \
    && chgrp 0 /opt/hivemq \
    && chmod 770 /opt/hivemq \
    && chmod +x /opt/hivemq/bin/run.sh /opt/docker-entrypoint.sh

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
