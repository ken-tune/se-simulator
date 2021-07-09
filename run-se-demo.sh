#!/bin/bash

echo 'Requires kubectl & helm'
echo
sleep 10

# Get required environment variables
source repository-env.sh

# Substitute in the k8s yml as needed
sed "s/\$DOCKERHUB_ACCOUNT/$DOCKERHUB_ACCOUNT/g" k8s/se-demo.yml.template > k8s/se-demo.yml

# Make sure you have the aerospike chart
helm repo add aerospike https://aerospike.github.io/aerospike-kubernetes
echo

# Install Clustered Aerospike
helm install se-demo aerospike/aerospike --set enableAerospikeMonitoring=true --set rbac.create=true --set-file aerospikeConfFile=config/aerospike.conf \
| head -n 9
echo

# Install SE demo app
kubectl apply -f k8s/se-demo.yml
echo
echo Wait till all pods in the running state then ctrl-c
echo
echo This will take around 50 seconds
echo
sleep 10
while [ 1 ]
do
	kubectl get pods
	sleep 1
	echo
done

