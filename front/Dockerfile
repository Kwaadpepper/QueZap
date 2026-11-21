# Multi-stage Dockerfile for QueZap Frontend (Angular 22 + Bun + Nginx)
FROM oven/bun:1-alpine AS builder

WORKDIR /app

# Cache dependencies (skip git lifecycle scripts like husky in container)
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile --ignore-scripts

# Copy source and build
COPY . .
RUN bun run build

# Unprivileged Nginx Alpine Runtime (UID/GID 101)
FROM nginxinc/nginx-unprivileged:1.27-alpine

USER root

COPY --from=builder --chown=101:101 /app/dist/quezap/browser /usr/share/nginx/html
COPY --chown=101:101 nginx-security-headers.conf /etc/nginx/includes/security-headers.conf
COPY --chown=101:101 nginx.conf /etc/nginx/conf.d/default.conf
COPY --chown=101:101 docker-entrypoint.d/40-generate-config.sh /docker-entrypoint.d/40-generate-config.sh

RUN chmod +x /docker-entrypoint.d/40-generate-config.sh \
    && chown -R 101:101 /usr/share/nginx/html

USER 101

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
