#!/usr/bin/env bash

set -eo pipefail

IMAGE=${TARGET_IMAGE:-hivemq/hivemq-ce:}

cd "$(dirname $0)/../"
HIVEMQ_VERSION=$(./gradlew properties | grep ^version: | sed -e "s/version: //")
echo "Building Docker image for HiveMQ ${HIVEMQ_VERSION}"
./gradlew hivemqZip
cd docker
cp ../build/zip/hivemq-ce-${HIVEMQ_VERSION}.zip .
docker build --build-arg HIVEMQ_VERSION=${HIVEMQ_VERSION} -f Dockerfile -t ${IMAGE} .
rm -f hivemq-ce-${HIVEMQ_VERSION}.zip

echo "Tagging image as ${HIVEMQ_VERSION}"
TAGGED_IMAGE="${IMAGE//:*}:${HIVEMQ_VERSION}"
docker tag ${IMAGE} "${TAGGED_IMAGE}"

if [[ ${PUSH_IMAGE} == true ]]; then
    echo "Pushing image as ${TAGGED_IMAGE}"
    docker push ${TAGGED_IMAGE}
fi

# If we're building a tagged commit, it will also be the new :latest image on Docker Hub.
if [[ ${PUSH_IMAGE} == true ]]; then
    echo "Tagging image as ${IMAGE//:*}:latest"
    docker tag ${IMAGE} "${IMAGE//:*}:latest"
    echo "Pushing image as ${IMAGE//:*}:latest"
    docker push ${IMAGE//:*}:latest
fi
