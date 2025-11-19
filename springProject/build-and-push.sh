#!/bin/bash

# Exit on error
set -e

# 멀티 아키텍처 이미지 빌드 및 푸시 스크립트

IMAGE_NAME="ddingsh9/yeyakhaeyoserver"
VERSION="1.0.0"

echo "Building and pushing multi-architecture Docker image..."
echo "Image: ${IMAGE_NAME}:${VERSION}"
echo "Platforms: linux/amd64, linux/arm64"

# buildx builder 생성 (없으면)
docker buildx create --name multiarch-builder --use 2>/dev/null || docker buildx use multiarch-builder

# buildx bootstrap
docker buildx inspect --bootstrap

# 멀티 아키텍처 빌드 및 푸시
echo "Starting build..."
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t ${IMAGE_NAME}:${VERSION} \
  -t ${IMAGE_NAME}:latest \
  --push \
  .

# Only print success if we get here (set -e will exit on error)
echo ""
echo "✓ Build and push completed successfully!"
echo "✓ Image pushed: ${IMAGE_NAME}:${VERSION}"
echo "✓ Image pushed: ${IMAGE_NAME}:latest"