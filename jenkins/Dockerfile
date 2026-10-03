# Jenkins listo para este repo: JDK 21, Maven, Chromium y chromedriver. Úsalo en TU máquina (docker compose -f docker-compose.jenkins.yml up -d).
FROM jenkins/jenkins:lts-jdk21
USER root
RUN apt-get update \
 && apt-get install -y --no-install-recommends maven chromium chromium-driver git curl \
 && rm -rf /var/lib/apt/lists/*
ENV CHROME_BIN=/usr/bin/chromium CHROMEDRIVER_PATH=/usr/bin/chromedriver
USER jenkins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
