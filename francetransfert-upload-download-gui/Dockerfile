FROM node:20.11-alpine3.19 as builder

COPY . /appli/
WORKDIR /appli/
RUN npm install --force
RUN npm run build 

FROM nginxinc/nginx-unprivileged:stable-alpine3.21
COPY --from=builder /appli/dist/francetransfert-updown-gui /app
COPY ./docker/statics /app/statics
COPY ./docker/ft.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080