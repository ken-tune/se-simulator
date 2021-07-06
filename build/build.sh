#!/bin/bash

# Run trade-date-gen build with --no-cache to make sure we always get the latest code
docker build --no-cache -t trade-data-gen -t ktune/se-simulator:tradeGen -f "../docker/Dockerfile-tradeGen" ../docker
# Changing the BUILD_TIME argument forces a git clone every time
docker build --build-arg BUILD_TIME=$(date +%Y%m%d-%H%M%S) -t trade-store-server -t ktune/se-simulator:trade-store-server -f "../docker/Dockerfile-TradeStoreServer" ../docker

docker push ktune/se-simulator:tradeGen
docker push ktune/se-simulator:trade-store-server
