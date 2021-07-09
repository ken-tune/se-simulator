#!/bin/bash

kubectl delete -f k8s/se-demo.yml
helm uninstall se-demo