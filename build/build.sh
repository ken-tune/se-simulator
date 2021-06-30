#!/bin/bash

docker build -t trade-data-gen -t ktune/se-simulator:tradeGen -f "../docker/Dockerfile-tradeGen" ../docker
docker build -t trade-store-server -t ktune/se-simulator:trade-store-server -f "../docker/Dockerfile-TradeStoreServer" ../docker

docker push ktune/se-simulator:tradeGen
docker push ktune/se-simulator:trade-store-server
