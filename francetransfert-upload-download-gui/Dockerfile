FROM node:24-alpine AS builder

WORKDIR /appli/
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx/nginx-unprivileged:stable-alpine
COPY --from=builder /appli/dist/francetransfert-updown-gui /app
COPY ./docker/statics /app/statics
COPY ./docker/ft.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
