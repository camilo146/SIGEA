# syntax=docker/dockerfile:1

FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
# En algunos servidores NODE_ENV puede quedar en production y omitir devDependencies.
# Forzamos include=dev para que exista @angular/cli (comando ng) durante el build.
RUN npm ci --include=dev

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/dist/sigea-frontend/browser /usr/share/nginx/html

EXPOSE 80
