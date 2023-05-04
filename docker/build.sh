#!/usr/bin/env bash

set -eo pipefail

IMAGE="suhrmann/hivemq-ce:snapshot"

cd "$(dirname $0)/../"
HIVEMQ_VERSION=$(./gradlew properties | grep ^version: | sed -e "s/version: //")
echo "Building Docker image for HiveMQ ${HIVEMQ_VERSION}"
./gradlew hivemqZip
cd docker
cp ../build/distributions/hivemq-ce-${HIVEMQ_VERSION}.zip .

PUSH=""
if [[ ${PUSH_IMAGE} == true ]]; then
    echo "Pushing image"
    PUSH="--push"
fi

PLATFORMS=linux/amd64,linux/arm64,linux/arm/v7  # check support with jre base image!
docker buildx build ${PUSH} --platform ${PLATFORMS}  \
    --build-arg HIVEMQ_VERSION=${HIVEMQ_VERSION}  \
    -f Dockerfile  \
    -t ${IMAGE} .

rm -f hivemq-ce-${HIVEMQ_VERSION}.zip
