#!/usr/bin/env bash

set -eo pipefail

IMAGE=${TARGET_IMAGE:-hivemq/hivemq-ce:snapshot}

cd "$(dirname $0)/../"
HIVEMQ_VERSION=$(./gradlew properties | grep ^version: | sed -e "s/version: //")
echo "Building Docker image for HiveMQ ${HIVEMQ_VERSION}"
./gradlew hivemqZip
cd docker
cp ../build/zip/hivemq-ce-${HIVEMQ_VERSION}.zip .
docker build --build-arg HIVEMQ_VERSION=${HIVEMQ_VERSION} -f Dockerfile -t ${IMAGE} .
rm -f hivemq-ce-${HIVEMQ_VERSION}.zip

echo "Tagging image as snapshot"
TAGGED_IMAGE="${IMAGE//:*}:snapshot"
docker tag ${IMAGE} "${TAGGED_IMAGE}"


if [[ ${PUSH_IMAGE} == true ]]; then
    echo "Pushing image"
    docker push ${IMAGE}
fi