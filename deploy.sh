#!/bin/bash

set -e

NAMESPACE="online-store"

echo "=== Building Docker image for minikube ==="
minikube image build -t api-gateway:latest .

echo "=== Waiting for backend services to be ready ==="
kubectl wait --for=condition=available deployment/auth-service -n $NAMESPACE --timeout=180s
kubectl wait --for=condition=available deployment/user-service -n $NAMESPACE --timeout=180s
kubectl wait --for=condition=available deployment/order-service -n $NAMESPACE --timeout=180s
kubectl wait --for=condition=available deployment/payment-service -n $NAMESPACE --timeout=180s

echo "=== Applying Kubernetes manifests ==="
kubectl apply -f k8s/ -n $NAMESPACE

echo "=== Waiting for api-gateway deployment rollout ==="
kubectl rollout status deployment/api-gateway -n $NAMESPACE --timeout=120s

echo "=== Deployment complete ==="
echo "External URL:"
minikube service api-gateway -n $NAMESPACE --url
