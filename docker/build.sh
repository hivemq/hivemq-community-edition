#!/usr/bin/env bash

set -eo pipefail

IMAGE=${TARGET_IMAGE:-hivemq/hivemq-ce:snapshot}

if [[ ${TRAVIS} == "true" ]]; then
    echo "Logging into DockerHub"
    echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
fi

cd "$(dirname $0)/../"
HIVEMQ_VERSION=$(./gradlew properties | grep ^version: | sed -e "s/version: //")
echo "Building Docker image for HiveMQ ${HIVEMQ_VERSION}"
./gradlew packaging
cd docker
cp ../build/zip/hivemq-ce-${HIVEMQ_VERSION}.zip .
docker build --build-arg HIVEMQ_VERSION=${HIVEMQ_VERSION} -f Dockerfile -t ${IMAGE} .
rm -f hivemq-ce-${HIVEMQ_VERSION}.zip

if [[ ! -z ${TRAVIS_TAG} ]]; then
    echo "Tagging image as ${TRAVIS_TAG}"
    TAGGED_IMAGE="${IMAGE//:*}:${TRAVIS_TAG}"
    docker tag ${IMAGE} "${TAGGED_IMAGE}"

    if [[ ${PUSH_IMAGE} == true ]]; then
        docker push ${TAGGED_IMAGE}
    fi

    # If we're building a tagged commit, it will also be the new :latest image on Docker Hub.
    if [[ ${PUSH_IMAGE} == true ]]; then
        docker tag ${IMAGE} "${IMAGE//:*}:latest"
        docker push ${IMAGE//:*}:latest
    fi

fi

if [[ ${PUSH_IMAGE} == true ]]; then
    echo "Pushing image"
    docker push ${IMAGE}
fi