#!/bin/bash

set -e

if [[ ! $BUILD_ENV ]]; then
    BUILD_ENV=target/docker-image
fi

if [[ $IMG_NAME ]]; then

    echo "Preparing environment@$BUILD_ENV"
    mkdir -p $BUILD_ENV
    cp external_webapps/*.war $BUILD_ENV
    cp target/*.war $BUILD_ENV
    cp -r ../pyAddons $BUILD_ENV/
    cp -u ../karma-web-plugins/*/target/*.war $BUILD_ENV
    cp Dockerfile $BUILD_ENV/
    echo "Building image $IMG_NAME"
    docker build -t $IMG_NAME $BUILD_ENV

    echo "Cleaning..."
    rm -rf $BUILD_ENV
    echo "DONE!"

else
    echo "You need to specify IMG_NAME. Try with \`IMG_NAME=\"your.cool.name/image:tag\" $0\`"
fi


