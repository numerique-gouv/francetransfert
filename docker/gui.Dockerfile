# syntax=docker/dockerfile:1.6
# Build context: repo root.

FROM node:20.11-alpine3.19 AS builder
WORKDIR /app

COPY francetransfert-upload-download-gui/ /app/

# Patch environment.ts to use the same origin as the GUI (relative URLs).
# nginx inside the GUI container proxies /api-private/upload-module/* and
# /api-private/confirmation-module/* to upload-api, and
# /api-private/download-module/* to download-api.
# Original:
#   host: 'http://localhost:'
#   upload: '8080/api-private/upload-module/upload'
#   download: '8081/api-private/download-module/download-info'
# Patched:
#   host: ''
#   upload: '/api-private/upload-module/upload'
#   download: '/api-private/download-module/download-info'
RUN sed -i "s|'http://localhost:'|''|g" src/environments/environment.ts && \
    sed -i "s|'8080/|'/|g" src/environments/environment.ts && \
    sed -i "s|'8081/|'/|g" src/environments/environment.ts && \
    cp src/environments/environment.ts src/environments/environment.prod.ts

RUN npm install --force
RUN npm run build

FROM nginxinc/nginx-unprivileged:stable-alpine
COPY --from=builder /app/dist/francetransfert-updown-gui /app
COPY francetransfert-upload-download-gui/docker/statics /app/statics
COPY docker/gui.nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
