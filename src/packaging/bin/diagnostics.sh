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
echo -------------------------------------------------------------------------
echo
echo '                  _    _  _              __  __   ____'
echo '                 | |  | |(_)            |  \/  | / __ \ '
echo '                 | |__| | _ __   __ ___ | \  / || |  | |'
echo '                 |  __  || |\ \ / // _ \| |\/| || |  | |'
echo '                 | |  | || | \ V /|  __/| |  | || |__| |'
echo '                 |_|  |_||_|  \_/  \___||_|  |_| \___\_\'
echo
echo "-------------------------------------------------------------------------"
echo ""
echo "  HiveMQ Start Script for Linux/Unix v1.13"
echo ""
echo "                 DIAGNOSTIC MODE "
echo ""

echoerr() { printf "%s\n" "$*" >&2; }

if hash java 2>/dev/null; then

    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | sed 's/\..*//')

    if [[ "$((java_version))" -lt 11 ]]; then
        echoerr "HiveMQ requires at least Java version 11"
        exit 1
    fi

    ############## VARIABLES
    JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"

    JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"
    JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.nio=ALL-UNNAMED"
    JAVA_OPTS="$JAVA_OPTS --add-opens java.base/sun.nio.ch=ALL-UNNAMED"
    JAVA_OPTS="$JAVA_OPTS --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"
    JAVA_OPTS="$JAVA_OPTS --add-exports java.base/jdk.internal.misc=ALL-UNNAMED"

    if [ -c "/dev/urandom" ]; then
        # Use /dev/urandom as standard source for secure randomness if it exists
        JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"
    fi

    # JMX Monitoring
    JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

    JAVA_OPTS="$JAVA_OPTS -DdiagnosticMode=true"

    if [ -z "$HIVEMQ_HOME" ]; then
        HIVEMQ_FOLDER="$( cd "$( dirname "${BASH_SOURCE[0]}" )/../" && pwd )"
        HOME_OPT="-Dhivemq.home=$HIVEMQ_FOLDER"
    else
        HIVEMQ_FOLDER="$HIVEMQ_HOME"
        HOME_OPT="-Dhivemq.home=$HIVEMQ_FOLDER"
    fi

    if [ -z "$HIVEMQ_HEAPDUMP_FOLDER" ]; then
        HEAPDUMP_PATH="$HIVEMQ_FOLDER"
    else
        HEAPDUMP_PATH="$HIVEMQ_HEAPDUMP_FOLDER"
    fi

    if [ ! -d "$HIVEMQ_FOLDER" ]; then
        echoerr "ERROR! HiveMQ Home Folder not found."
    else

        if [ ! -w "$HIVEMQ_FOLDER" ]; then
            echoerr "ERROR! HiveMQ Home Folder Permissions not correct."
        else

            if [ ! -f "$HIVEMQ_FOLDER/bin/hivemq.jar" ]; then
                echoerr "ERROR! HiveMQ JAR not found."
                echoerr "$HIVEMQ_FOLDER";
            else
                JAVA_OPTS="$JAVA_OPTS -XX:+CrashOnOutOfMemoryError"
                JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
                HEAPDUMP_PATH_OPT="-XX:HeapDumpPath=$HEAPDUMP_PATH/heap-dump.hprof"
                ERROR_FILE_PATH_OPT="-XX:ErrorFile=$HEAPDUMP_PATH/hs_err_pid%p.log"

                echo "-------------------------------------------------------------------------"
                echo ""
                echo "  HIVEMQ_HOME: $HIVEMQ_FOLDER"
                echo ""
                echo "  JAVA_OPTS: $JAVA_OPTS"
                echo ""
                echo "  JAVA_VERSION: $java_version"
                echo ""
                echo "-------------------------------------------------------------------------"
                echo ""
                # Run HiveMQ
                JAR_PATH="$HIVEMQ_FOLDER/bin/hivemq.jar"
                exec "java" "${HOME_OPT}" "${HEAPDUMP_PATH_OPT}" "${ERROR_FILE_PATH_OPT}" ${JAVA_OPTS} -jar "${JAR_PATH}"
            fi
        fi
    fi

else
  echoerr "ERROR! You do not have the Java Runtime Environment installed, please install Java JRE from https://adoptopenjdk.net/?variant=openjdk11 and try again."
fi
