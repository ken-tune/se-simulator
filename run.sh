#!/bin/bash

kubectl create configmap aero-conf --from-file=config/aerospike.conf
kubectl apply -f k8s/demo.yml
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

