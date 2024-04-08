FROM node:20.11-alpine3.19 as builder

COPY . /appli/
WORKDIR /appli/
RUN npm install --force
RUN npm run build 

FROM bitnami/nginx:latest
COPY --from=builder /appli/dist/francetransfert-updown-gui /app
COPY ./docker/statics /app/statics
COPY ./docker/ft.conf /opt/bitnami/nginx/conf/server_blocks/ft.conf
EXPOSE 8080