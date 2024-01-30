ARG BASE_IMAGE_TAG=zulu-openjdk-17.0.3-alpine-2

FROM com2mcore.azurecr.io/iot-base-image:$BASE_IMAGE_TAG

ARG HIVEMQ_VERSION=2024.1
ENV HIVEMQ_GID=10000
ENV HIVEMQ_UID=10000

# Additional JVM options, may be overwritten by user
ENV JAVA_OPTS "-XX:+UnlockExperimentalVMOptions -XX:+UseNUMA"

# Default allow all extension, set this to false to disable it
ENV HIVEMQ_ALLOW_ALL_CLIENTS "false"

# Set locale
ENV LANG=en_US.UTF-8

RUN set -x \
	&& apk update \
	&& apk add --no-cache tini

COPY config.xml /opt/config.xml
COPY docker-entrypoint.sh /opt/docker-entrypoint.sh

# HiveMQ setup
COPY build/distributions/hivemq-ce-${HIVEMQ_VERSION} /opt/hivemq-ce-${HIVEMQ_VERSION}
RUN ln -s /opt/hivemq-ce-${HIVEMQ_VERSION} /opt/hivemq

RUN ls -la /opt
RUN ls -la /opt/hivemq-ce-${HIVEMQ_VERSION}

WORKDIR /opt/hivemq

# Configure user and group for HiveMQ
RUN addgroup -g ${HIVEMQ_GID} hivemq \
    && adduser -D -G hivemq -h /opt/hivemq-ce-${HIVEMQ_VERSION} --u ${HIVEMQ_UID} hivemq \
    && chown -R hivemq:hivemq /opt/hivemq-ce-${HIVEMQ_VERSION} \
    && chmod -R 777 /opt \
    && chmod +x /opt/hivemq/bin/run.sh /opt/docker-entrypoint.sh

# Substitute eval for exec and replace OOM flag if necessary (for older releases). This is necessary for proper signal propagation
RUN sed -i -e 's|eval \\"java\\" "$HOME_OPT" "$JAVA_OPTS" -jar "$JAR_PATH"|exec "java" $HOME_OPT $JAVA_OPTS -jar "$JAR_PATH"|' /opt/hivemq/bin/run.sh && \
    sed -i -e "s|-XX:OnOutOfMemoryError='sleep 5; kill -9 %p'|-XX:+CrashOnOutOfMemoryError|" /opt/hivemq/bin/run.sh

RUN sed -i -e 's|exec "java" "${HOME_OPT}" "${HEAPDUMP_PATH_OPT}" ${JAVA_OPTS} -jar "${JAR_PATH}"|exec "java" "${HOME_OPT}" "${HEAPDUMP_PATH_OPT}" ${JAVA_OPTS} -XX:OnOutOfMemoryError="kill 0" -jar "${JAR_PATH}"|' /opt/hivemq/bin/run.sh

RUN apk add libstdc++ --no-cache

RUN rm -rf /opt/hivemq/extensions/hivemq-allow-all-extension

ADD cronjobs /tmp/cronjobs
RUN cat /tmp/cronjobs | crontab -
RUN rm /tmp/cronjobs

RUN mkdir -p /opt/hivemq/default-cert
RUN mkdir -p /opt/hivemq/cert

RUN chmod -R 777 /opt/hivemq
RUN chmod -R 777 /opt/hivemq-ce-2024.1

ADD check_extension.sh /opt/check_extension.sh
RUN chmod +x /opt/check_extension.sh

# Make broker data persistent throughout stop/start cycles
VOLUME /opt/hivemq/data

# Persist log data
VOLUME /opt/hivemq/log

VOLUME /opt/hivemq/cert

#mqtt-clients
EXPOSE 1883

#websockets
EXPOSE 8000

WORKDIR /opt/hivemq

ENTRYPOINT ["/sbin/tini", "--"]
CMD ["/opt/hivemq/bin/run.sh"]
