#!/bin/bash

docker stop trade-data-gen 
docker rm trade-data-gen 
docker stop trade-server
docker rm trade-server
docker image rm trade-server

cd build
./build.sh 
cd ..

docker run -d --name trade-server trade-server
docker run --name trade-data-gen trade-data-gen