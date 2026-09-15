# Doorbell

## Build locally to Podman

```sh
./gradlew jibDockerBuild -Djib.dockerClient.executable=$(which podman) --image=doorbell
```

## Run locally with Podman

```sh
podman run --rm \
  --env 'TELEGRAM_ENABLED=true' \
  --env 'TELEGRAM_BOT_TOKEN=<TOKEN>' \
  --env 'TELEGRAM_CHAT_ID=<CHAT_ID>' \
  doorbell
```
