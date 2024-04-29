#!/bin/bash
# Copyright 2019-present HiveMQ GmbH
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
echo '-------------------------------------------------------------------------'
echo ''
echo '                  _    _  _              __  __   ____'
echo '                 | |  | |(_)            |  \/  | / __ \'
echo '                 | |__| | _ __   __ ___ | \  / || |  | |'
echo '                 |  __  || |\ \ / // _ \| |\/| || |  | |'
echo '                 | |  | || | \ V /|  __/| |  | || |__| |'
echo '                 |_|  |_||_|  \_/  \___||_|  |_| \___\_\'
echo ''
echo '-------------------------------------------------------------------------'
echo ''
echo '  HiveMQ Start Script for Linux/Unix v1.14'
echo ''

if ! hash java 2>/dev/null; then
    echo 'ERROR! You do not have the Java Runtime Environment installed, please install Java JRE from https://adoptium.net/?variant=openjdk11 and try again.' >&2
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | sed 's/\..*//')

if [ "$JAVA_VERSION" -lt 11 ]; then
    echo 'ERROR! HiveMQ requires at least Java version 11.' >&2
    exit 1
fi

############## VARIABLES
JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"

JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.nio=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/sun.nio.ch=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-exports java.base/jdk.internal.misc=ALL-UNNAMED"

if [ -c '/dev/urandom' ]; then
    # Use /dev/urandom as standard source for secure randomness if it exists
    JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"
fi

# JMX Monitoring
if [ "${HIVEMQ_JMX_ENABLED:-true}" = 'true' ]; then
    JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=${HIVEMQ_JMX_PORT:-9010} -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
fi

# Disable Localization
JAVA_OPTS="$JAVA_OPTS -Duser.language=en -Duser.region=US"

# Uncomment for enabling Diagnostic Mode
#JAVA_OPTS="$JAVA_OPTS -DdiagnosticMode=true"

if [ -z "$HIVEMQ_HOME" ]; then
    HIVEMQ_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )/../" && pwd )"
fi
HOME_OPT="-Dhivemq.home=$HIVEMQ_HOME"

if [ ! -d "$HIVEMQ_HOME" ]; then
    echo 'ERROR! HiveMQ home folder not found.' >&2
    exit 1
fi

if [ ! -w "$HIVEMQ_HOME" ]; then
    echo 'ERROR! HiveMQ home folder permissions not correct.' >&2
    exit 1
fi

JAR_PATH="$HIVEMQ_HOME/bin/hivemq.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo 'ERROR! HiveMQ JAR not found.' >&2
    exit 1
fi

if [ -z "$HIVEMQ_HEAPDUMP_FOLDER" ]; then
    HIVEMQ_HEAPDUMP_FOLDER="$HIVEMQ_HOME"
fi

JAVA_OPTS="$JAVA_OPTS -XX:+CrashOnOutOfMemoryError"
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
HEAPDUMP_PATH_OPT="-XX:HeapDumpPath=$HIVEMQ_HEAPDUMP_FOLDER/heap-dump.hprof"
ERROR_FILE_PATH_OPT="-XX:ErrorFile=$HIVEMQ_HEAPDUMP_FOLDER/hs_err_pid%p.log"

echo '-------------------------------------------------------------------------'
echo ''
echo "  HIVEMQ_HOME: $HIVEMQ_HOME"
echo ''
echo "  JAVA_OPTS: $JAVA_OPTS"
echo ''
echo "  JAVA_VERSION: $JAVA_VERSION"
echo ''
echo '-------------------------------------------------------------------------'
echo ''

exec java "${HOME_OPT}" "${HEAPDUMP_PATH_OPT}" "${ERROR_FILE_PATH_OPT}" ${JAVA_OPTS} -jar "${JAR_PATH}"
