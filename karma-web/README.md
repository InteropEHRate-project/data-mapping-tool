# Instruction to build docker image from source code
1) from Web-Karma directory
   ```
   $mvn clean install -DskipTests
   ```
2) from karma-web directory
   ```
   $mvn package -DskipTest
   ```
3) (you may need to make the file executable) from karma-web directory
   ```
   chmod +x build-docker.sh
   ```
4) from karma-web directory
   ```
   $IMG_NAME=your.cool.name/image:tag build-docker.sh
   ```
   for zsh change the `#!/bin/bash` to `#!/usr/bin/env bash` in the build-docker.sh file and run the following instead.
   ```
   $IMG_NAME=your.cool.name/image:tag ./build-docker.sh
   ```

# docker run Example
   ```
   $docker run --name karmalinker -p 7000:7000 your.cool.name/image:tag
   ```


# docker-compose.yml Example
```
karmalinker:
        image: your.cool.name/image:tag
        container_name: karmalinker
        environment:
            - SCROLL_SERVICE_URL=http://sweb:8080/
            - EML_SERVICE_URL=http://converter:6000/
            - KOS_SERVICE_URL=http://localhost:5001/
            - K_IMPORTER_URL=
        restart: unless-stopped
        ports:
            - "7000:7000"
```
