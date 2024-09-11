FROM openjdk:18-jdk-slim AS app-builder

ENV DEBIAN_FRONTEND noninteractive

WORKDIR /
#=============================
# Install Dependenices 
#=============================
SHELL ["/bin/bash", "-c"]

RUN apt update &&\
    apt install -y curl sudo wget unzip bzip2 \
    libdrm-dev libxkbcommon-dev libgbm-dev libasound-dev \
    libnss3 libxcursor1 libpulse-dev libxshmfence-dev \
    xauth xvfb x11vnc fluxbox wmctrl libdbus-glib-1-2 jq

#==============================
# Android SDK ARGS
#==============================
ARG ARCH="x86_64" 
ARG TARGET="google_apis_playstore"  
ARG API_LEVEL="34" 
ARG BUILD_TOOLS="34.0.0"
ARG ANDROID_ARCH=${ANDROID_ARCH_DEFAULT}
ARG ANDROID_API_LEVEL="android-${API_LEVEL}"
ARG ANDROID_APIS="${TARGET};${ARCH}"
ARG EMULATOR_PACKAGE="system-images;${ANDROID_API_LEVEL};${ANDROID_APIS}"
ARG PLATFORM_VERSION="platforms;${ANDROID_API_LEVEL}"
ARG BUILD_TOOL="build-tools;${BUILD_TOOLS}"
ARG ANDROID_CMD="commandlinetools-linux-11076708_latest.zip"
ARG ANDROID_SDK_PACKAGES="${EMULATOR_PACKAGE} ${PLATFORM_VERSION} ${BUILD_TOOL} platform-tools"

#==============================
# Set JAVA_HOME - SDK
#==============================
ENV ANDROID_SDK_ROOT=/opt/android
ENV PATH "$PATH:$ANDROID_SDK_ROOT/cmdline-tools/tools:$ANDROID_SDK_ROOT/cmdline-tools/tools/bin:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/tools/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS}"
ENV DOCKER="true"

#============================================
# Install required Android CMD-line tools
#============================================
RUN wget https://dl.google.com/android/repository/${ANDROID_CMD} -P /tmp && \
              unzip -d $ANDROID_SDK_ROOT /tmp/$ANDROID_CMD && \
              mkdir -p $ANDROID_SDK_ROOT/cmdline-tools/tools && \
              cd $ANDROID_SDK_ROOT/cmdline-tools && \
              mv NOTICE.txt source.properties bin lib tools/

#============================================
# Install required package using SDK manager
#============================================
RUN yes Y | sdkmanager --licenses 
RUN yes Y | sdkmanager --verbose --no_https ${ANDROID_SDK_PACKAGES} 

#===========================================
# cache the dependencies. really, just speeds up rebuilds
#===========================================
WORKDIR /app

# copy everything except the actual application, allowing us to cache a baseline
COPY ./android/build.gradle.kts /app/
COPY ./android/gradle.properties /app/
COPY ./android/settings.gradle.kts /app/
COPY ./android/gradle/ /app/gradle/
COPY ./android/gradlew /app/
COPY ./android/app/build.gradle.kts /app/app/

ENV GRADLE_USER_HOME=/app/gradle_home

# Cache Gradle and the project dependencies
RUN mkdir -p $GRADLE_USER_HOME && \
    chmod +x gradlew && \
    ./gradlew build --no-daemon || true

#============================================
# build the apk
#============================================
# NOTE: this is only copying the app subfolder. anything in the project root unrelated to gradle bootstrapping needs to be copied too
COPY ./android /app 
ARG CRYZE_BACKEND_URL "http://cryze_api:8080"
ENV CRYZE_BACKEND_URL=$CRYZE_BACKEND_URL
ARG CRYZE_RTSP_SERVER "localhost"
ENV CRYZE_RTSP_SERVER=$CRYZE_RTSP_SERVER
# produce a debug apk
RUN chmod +x gradlew && \
    ./gradlew --no-daemon assembleDebug && \
    mv app/build/outputs/apk/debug/app-debug.apk ./app.apk

## libhoudini builder, for arm64 translation
FROM alpine:3.14 AS libhoudini-builder
# this image has a strange layout so we cant just expand it directly onto the emulator image
ADD https://codeload.github.com/natsumerinchan/libhoudini-package/tar.gz/refs/tags/v3.0 /tmp/libhoudini-android11-x86_64.tar.gz
RUN apk add --no-cache tar gzip
WORKDIR /tmp
RUN tar xvf /tmp/libhoudini-android11-x86_64.tar.gz
## chmod all the binaries in the package
RUN chmod 755 -R /tmp/libhoudini-package-3.0/src/system/*

FROM alpine:3.14 AS busybox-builder
ADD https://github.com/meefik/busybox/releases/download/1.34.1/busybox-v1_34_1.zip /
RUN apk add --no-cache zip
RUN unzip /busybox-v1_34_1.zip -d /busybox
RUN chmod +x /busybox/x86_64/bin/busybox
# we don't need it, but there's also /busybox-v1_34_1/x86_64/bin/ssl_helper

FROM gcc:latest AS launcher-builder

WORKDIR /app

# Copy the source code
COPY fork_logger/ .

# Compile the binary statically
RUN gcc -static -o launcher main.c

## final image
FROM redroid/redroid:11.0.0-latest
#SHELL ["/system/bin/busybox", "ash", "-c"]

# used to redirect the /init stdout handle to in-container fifo pipe
COPY --from=launcher-builder --chmod=777 /app/launcher /system/bin/launcher
## logger service pieces
COPY --chmod=644 fork_logger/dockercat.rc /vendor/etc/init/dockercat.rc
COPY --chmod=777 fork_logger/log_watcher.sh /system/bin/log_watcher.sh

# copy the libhoudini files so we can use arm64 translation
# definitely want to remove this on arm64 hosts
COPY --from=libhoudini-builder /tmp/libhoudini-package-3.0/src/system/lib/libhoudini.so /system/lib/libhoudini.so
COPY --from=libhoudini-builder /tmp/libhoudini-package-3.0/src/system/lib64/libhoudini.so /system/lib64/libhoudini.so

# add busybox, used for all kinds of things in this gigantic "hack"
COPY --from=busybox-builder /busybox/x86_64/bin/busybox /system/bin/busybox

# copy the apk, service definitions, and the scrip that installs, launches and restarts the app
COPY --chmod=644 docker_contents/install.app.rc /vendor/etc/init/install.app.rc
# to cause this script to exit, setprop breakloop 1
COPY --chmod=777 docker_contents/ensure_running.sh /system/bin/ensure_running.sh
COPY --from=app-builder /app/app.apk /app/app.apk

# use the rewritten entrypoint to capture logs into docker
ENTRYPOINT [ "/system/bin/launcher","qemu=1","androidboot.hardware=redroid" ]