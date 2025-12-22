docker build -f Docker/local/Dockerfile -t tradingbotdev .

docker run \
  -p 127.0.0.1:8081:8081 \
  -v C:/Users/41786/Documents/GitHub/TradingBot:/home/gradle/app \
  -v C:/Users/41786/Documents/GitHub/TradingBot/security/tls:/app/security/tls:ro \
  -v C:/Users/41786/Documents/GitHub/TradingBot/security/auth:/app/security/auth:ro \
  --name tradingbotdev \
  -e APIKEY \
  -e PAPERAPIKEY \
  -e PAPERSECRET \
  -e SECRET \
  -e KEYSTORE_PRIVATEKEY_CERTIFICATE_PASSWORD \
  -e AUTHENTIFICATION_PASSWORD \
  -e AUTHENTIFICATION_USERNAME \
  -e KEYSTORE_PATH \
  -e PRIVATEKEY_PATH \
  -e PUBLICKEY_PATH \
  tradingbotdev \
  sh -c "cd /home/gradle/app && gradle clean buildFatJar --no-daemon && java -jar build/libs/*-all.jar"
