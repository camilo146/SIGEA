# syntax=docker/dockerfile:1

FROM node:20-alpine AS build
WORKDIR /app

# Forzar instalación de devDependencies durante el build del frontend.
ENV NODE_ENV=development
ENV NPM_CONFIG_PRODUCTION=false

COPY package*.json ./
RUN npm ci --include=dev

COPY . .
RUN npx ng build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/dist/sigea-frontend/browser /usr/share/nginx/html

EXPOSE 80
