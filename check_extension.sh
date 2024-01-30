#!/bin/bash

echo "$(date -u) Check Hive MQ Extensions." >> check_extension.log
FILE=/opt/hivemq/extensions/mqtt-hivemq-broker-service/DISABLED

if rm $FILE; then
    echo "$(date -u) Disabled Extension found. Shutdown HiveMQ." >> check_extension.log
    /opt/hivemq/bin/init-script/hivemq stop
fi
