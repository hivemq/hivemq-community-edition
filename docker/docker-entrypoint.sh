#!/usr/bin/env bash

set -eo pipefail

# We set the bind address here to ensure HiveMQ uses the correct interface. Defaults to using the container hostname (which should be hardcoded in /etc/hosts)
if [ -z "${HIVEMQ_BIND_ADDRESS}" ]; then
    echo "Getting bind address from container hostname"
    ADDR=$(getent hosts ${HOSTNAME} | grep -v 127.0.0.1 | awk '{ print $1 }' | head -n 1)
else
    echo "HiveMQ bind address was overridden by environment variable (value: ${HIVEMQ_BIND_ADDRESS})"
    ADDR=${HIVEMQ_BIND_ADDRESS}
fi

# Remove allow all extension if applicable

if [[ "${HIVEMQ_ALLOW_ALL_CLIENTS}" != "true" ]]; then
    echo "Disabling allow all extension"
    rm -rf /opt/hivemq/extensions/hivemq-allow-all-extension &>/dev/null || true
fi

echo "set bind address from container hostname to ${ADDR}"
export HIVEMQ_BIND_ADDRESS=${ADDR}

# Step down from root privilege, only when we're attempting to run HiveMQ though.
if [[ "$1" = "/opt/hivemq/bin/run.sh" && "$(id -u)" = '0' && "${HIVEMQ_NO_ROOT_STEP_DOWN}" != "true" ]]; then
    uid="hivemq"
    gid="hivemq"
    exec_cmd="exec gosu hivemq:hivemq"
else
    uid="$(id -u)"
    gid="$(id -g)"
    exec_cmd="exec"
fi

readonly uid
readonly gid
readonly exec_cmd

if [[ "$(id -u)" = "0" ]]; then
    chown "${uid}":"${gid}" /opt/hivemq/data
    chmod 700 /opt/hivemq/data
    # Any of the following may fail but still allow HiveMQ to start normally, so lets ignore errors
    set +e
    chown "${uid}":"${gid}" /opt/hivemq
    chown "${uid}":"${gid}" /opt/hivemq-*
    chown "${uid}":"${gid}" /opt/hivemq/log
    chown "${uid}":"${gid}" /opt/hivemq/conf
    chown "${uid}":"${gid}" /opt/hivemq/conf/config.xml
    # Recursive for bin, no volume here
    chown -R "${uid}":"${gid}" /opt/hivemq/bin
    chmod 700 /opt/hivemq
    chmod 700 /opt/hivemq-*
    chmod -R 700 /opt/hivemq/bin
fi

HIVEMQ_BIND_ADDRESS=${ADDR} ${exec_cmd} "$@"
