# Etapa 1: compila el portal React (contexto adicional "frontend" definido en docker-compose.yml)
FROM node:20-alpine AS ui
WORKDIR /ui
COPY --from=frontend package.json ./
RUN npm install --no-audit --no-fund --loglevel=error
COPY --from=frontend public ./public
COPY --from=frontend src ./src
ENV REACT_APP_API_URL=/api/clinica
RUN npm run build

# Etapa 2: nginx sirve el portal y actúa como API gateway
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=ui /ui/build /usr/share/nginx/html
