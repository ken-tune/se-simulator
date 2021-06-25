#!/bin/bash

docker build -t trade-data-gen -t ktune/se-simulator:tradeGen -f "../docker/Dockerfile-tradeGen" ../docker
docker build -t trade-server -t ktune/se-simulator:trade-server -f "../docker/Dockerfile-tradeServer" ../docker

docker push ktune/se-simulator:tradeGen
docker push ktune/se-simulator:trade-server
