#!/usr/bin/env bash

set -eo pipefail

IMAGE_PREFIX="hivemq/hivemq-ce"
LATEST_IMAGE="${IMAGE_PREFIX}:latest"

cd "$(dirname $0)/../"
HIVEMQ_VERSION=$(./gradlew properties | grep ^version: | sed -e "s/version: //")
echo "Building Docker image for HiveMQ ${HIVEMQ_VERSION}"
./gradlew hivemqZip
cd docker
cp ../build/distributions/hivemq-ce-${HIVEMQ_VERSION}.zip .

echo "Tagging image as ${HIVEMQ_VERSION}"
VERSIONED_IMAGE="${IMAGE_PREFIX}:${HIVEMQ_VERSION}"

PUSH=""
if [[ ${PUSH_IMAGE} == true ]]; then
    PUSH="--push"
    echo "Pushing image as ${VERSIONED_IMAGE}"
    echo "Pushing image as ${LATEST_IMAGE}"
fi

PLATFORMS=linux/amd64,linux/arm64,linux/arm/v7  # check support with jre base image!
docker buildx build ${PUSH} --platform ${PLATFORMS}  \
  --build-arg HIVEMQ_VERSION=${HIVEMQ_VERSION}  \
  -f Dockerfile  \
  -t ${LATEST_IMAGE}  \
  -t ${VERSIONED_IMAGE} .

rm -f hivemq-ce-${HIVEMQ_VERSION}.zip
