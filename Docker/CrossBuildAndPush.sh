docker buildx create --use --name mybuilder || true
docker buildx inspect mybuilder --bootstrap
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t naegerr/tradingbot:latest \
  -f Docker/deployment/Dockerfile \
  . \
  --push

#docker tag naegerr/tradingbot:latest naegerr/tradingbot:build-9999
#docker push naegerr/tradingbot:build-9999
