Hello!

This is a rough RTSP server for WYZE cameras of the GWELL variety

## preface
THANK YOU to Carson Loyal (carTloyal123) for the libraries to connect and get streams and pedroSG94 for RTSP related android libraries. I used the following repos:
- [https://github.com/carTloyal123/cryze-android](cryze-android) - the library for connecting to the cameras
- [https://github.com/carTloyal123/cryze](cryze) - scripts for getting tokens, capturing raw stream contents
- [https://github.com/pedroSG94/RootEncoder](RootEncoder) - library for streaming RTSP
- [https://github.com/pedroSG94/RTSP-Server](RTSP-Server) - library for serving RTSP streams

## Prereqs
- An x86 machine. I am using libhoudini in `redroid` to make the cryze android app work with the binaries for getting connections. This avoids the overhead of qemu or other android emulators.
- a kernel compatible with `redroid`. follow [this guide](https://github.com/remote-android/redroid-doc/blob/master/deploy/README.md)
- Wyze GWELL cameras. I've tested with `GW_GC1` (Wyze Cam OG) and `GW_BE1` (Wyze Cam Doorbell Pro	)

To use this, docker compose is easiest.
copy sample.env to .env
update your details

build the thing:
```
docker compose build
docker compose up -d
```

you can view the android container over adb with something like scrcpy: `scrcpy -s localhost:5555`

## Support
I am not tech support, I am sorry, but I just do not have time. To debug the android half, you _will_ need to use logcat/a debugger/android studio. The `redroid` logs are _not_ flushed to the docker log, and debugging is difficult. 

## Use
The RTSP server has issues. All sockets are written on the same thread and there's multi-connection issues. Use go2rtc/frigate/something to keep the connection count down.

AGAIN: DO NOT DIRECTLY OPEN THESE RTSP CONNECTIONS.

My personal docker-compose.yml is combined with a frigate and wyze-bridge container to consolidate everything

```yaml
networks:
  frigate:
    driver: bridge

services:
  wyze-bridge:
    container_name: wyze-bridge
    restart: unless-stopped
    image: mrlt8/wyze-bridge:latest
    networks:
      - frigate
    ports:
      - 5004:5000 # WEB-UI
    environment:
      ON_DEMAND: True
      LLHLS: True
      WYZE_EMAIL: ${WYZE_EMAIL}
      WYZE_PASSWORD: ${WYZE_PASSWORD}
      API_ID: ${WYZE_API_ID}
      API_KEY: ${WYZE_API_KEY}
      WB_AUTH: False # Set to false to disable web and stream auth.
  frigate:
    container_name: frigate
    privileged: true # this may not be necessary for all setups
    restart: unless-stopped
    image: ghcr.io/blakeblackshear/frigate:stable
    shm_size: "256mb" # update for your cameras based on calculation above
    networks:
      - frigate
    devices:
      - /dev/bus/usb:/dev/bus/usb # Passes the USB Coral, needs to be modified for other versions
      - /dev/dri/renderD128:/dev/dri/renderD128 # For intel hwaccel, needs to be updated for your har>    volumes:
      - /etc/localtime:/etc/localtime:ro
      - ./config:/config
      - ./storage:/media/frigate
      - type: tmpfs # Optional: 1GB of memory, reduces SSD/SD Card wear
        target: /tmp/cache
        tmpfs:
          size: 1000000000
    ports:
      - "8971:8971"
      - "5005:5000" # Internal unauthenticated access. Expose carefully.
      - "8554:8554" # RTSP feeds
      - "8555:8555/tcp" # WebRTC over tcp
      - "8555:8555/udp" # WebRTC over udp
    environment:
      FRIGATE_RTSP_PASSWORD: "password"

  cryze_api:
    networks:
      - frigate
    build:
      context: ./cryze_api
      dockerfile: Dockerfile
    ports:
      - 8080:8080
    env_file:
      - .env

  cryze_android_app:
    networks:
      - frigate
    build:
      context: ./cryze_android_app
      dockerfile: Dockerfile
    privileged: true
    volumes:
      - /dev/dri/renderD128:/dev/dri/renderD128
    ports:
      - 5555:5555
    entrypoint: # need to override the entrypoint to run the app on arch, see the redroid docs
      - /init
      - qemu=1
      - androidboot.hardware=redroid
      - androidboot.use_memfd=1
      - androidboot.redroid_net_ndns=1
      - androidboot.redroid_net_dns1=127.0.0.11
      - androidboot.redroid_gpu_mode=host
      - androidboot.redroid_gpu_node=/dev/dri/renderD128
```

I am using frigate with a config like this:
```yaml
cameras:
  doorbell:
    enabled: true
    ffmpeg:
      retry_interval: 0
      inputs:
        - path: rtsp://cryze_android_app:8001
          input_args: preset-rtsp-restream
          roles:
            - detect
    detect:
      width: 1440
      height: 1440
      enabled: true
```
