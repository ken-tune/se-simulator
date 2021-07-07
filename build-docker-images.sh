#!/bin/bash

DOCKERHUB_ACCOUNT=ktune

# Run trade-date-gen build with --no-cache to make sure we always get the latest code
docker build --no-cache -t trade-data-gen -t ${DOCKERHUB_ACCOUNT}/se-simulator:tradeGen -f "docker/Dockerfile-tradeGen" docker
# Changing the BUILD_TIME argument forces a git clone every time
docker build --build-arg BUILD_TIME=$(date +%Y%m%d-%H%M%S) -t trade-store-server -t ${DOCKERHUB_ACCOUNT}/se-simulator:trade-store-server -f "docker/Dockerfile-TradeStoreServer" docker

docker push ${DOCKERHUB_ACCOUNT}/se-simulator:tradeGen
docker push ${DOCKERHUB_ACCOUNT}/se-simulator:trade-store-server
