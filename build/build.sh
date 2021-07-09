#!/bin/bash

docker build -t trade-data-gen -t pbanavara/se-simulator:tradeGen -f "../docker/Dockerfile-tradeGen" ../docker
docker build -t trade-store-server -t pbanavara/se-simulator:trade-store-server -f "../docker/Dockerfile-TradeStoreServer" ../docker

docker push pbanavara/se-simulator:tradeGen
docker push pbanavara/se-simulator:trade-store-server
