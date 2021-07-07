#!/bin/bash

# Get required environment variables
source repository-env.sh

# Substitute in the k8s yml as needed
sed "s/\$DOCKERHUB_ACCOUNT/$DOCKERHUB_ACCOUNT/g" k8s/se-demo.yml.template > k8s/se-demo.yml

kubectl create configmap aero-conf --from-file=config/aerospike.conf
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

