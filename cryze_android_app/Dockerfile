FROM openjdk:18-jdk-slim as builder

LABEL maintainer "Carson Loyal (from Amir)"

ENV DEBIAN_FRONTEND noninteractive

WORKDIR /
#=============================
# Install Dependenices 
#=============================
SHELL ["/bin/bash", "-c"]   

RUN apt update && apt install -y curl sudo wget unzip bzip2 libdrm-dev libxkbcommon-dev libgbm-dev libasound-dev libnss3 libxcursor1 libpulse-dev libxshmfence-dev xauth xvfb x11vnc fluxbox wmctrl libdbus-glib-1-2 jq

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
              mkdir -p $ANDROID_SDK_ROOT/cmdline-tools/tools && cd $ANDROID_SDK_ROOT/cmdline-tools &&  mv NOTICE.txt source.properties bin lib tools/  && \
              cd $ANDROID_SDK_ROOT/cmdline-tools/tools && ls

#============================================
# Install required package using SDK manager
#============================================
RUN yes Y | sdkmanager --licenses 
RUN yes Y | sdkmanager --verbose --no_https ${ANDROID_SDK_PACKAGES} 

#============================================
# Create required emulator
#============================================
ARG EMULATOR_NAME="nexus"
ARG EMULATOR_DEVICE="Nexus 6"
ENV EMULATOR_NAME=$EMULATOR_NAME
ENV DEVICE_NAME=$EMULATOR_DEVICE
# install the emulator package
RUN yes Y | sdkmanager --install "emulator"
RUN echo "no" | avdmanager --verbose create avd --force --name "${EMULATOR_NAME}" --device "${EMULATOR_DEVICE}" --package "${EMULATOR_PACKAGE}"

#============================================
# build the apk
#============================================
COPY . /app
WORKDIR /app
RUN chmod +x gradlew && ./gradlew --no-daemon assembleDebug && mv app/build/outputs/apk/debug/app-debug.apk ./app.apk

## libhoudini builder, for arm64 translation
FROM alpine:3.14 AS libhoudini-builder
# this image has a strange layout so we cant just expand it directly onto the emulator image
ADD https://codeload.github.com/natsumerinchan/libhoudini-package/tar.gz/refs/tags/v3.0 /tmp/libhoudini-android11-x86_64.tar.gz
RUN apk add --no-cache tar gzip
WORKDIR /tmp
RUN tar xvf /tmp/libhoudini-android11-x86_64.tar.gz
RUN chmod 755 /tmp/libhoudini-package-3.0/src/system/lib/libhoudini.so
RUN chmod 755 /tmp/libhoudini-package-3.0/src/system/lib64/libhoudini.so

## final image
FROM redroid/redroid:11.0.0-latest

# copy the libhoudini files so we can use arm64 translation
COPY --from=libhoudini-builder /tmp/libhoudini-package-3.0/src/system/lib/libhoudini.so /system/lib/libhoudini.so
COPY --from=libhoudini-builder /tmp/libhoudini-package-3.0/src/system/lib64/libhoudini.so /system/lib64/libhoudini.so

# copy the apk and install script
COPY docker_contents/install.app.rc /vendor/etc/init/install.app.rc
COPY --from=builder /app/app.apk /app/app.apk
