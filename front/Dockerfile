# Multi-stage Dockerfile for QueZap Frontend (Angular 21 + Bun + Nginx)
FROM oven/bun:1-alpine AS builder

WORKDIR /app

# Cache dependencies (skip git lifecycle scripts like husky in container)
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile --ignore-scripts

# Copy source and build
COPY . .
RUN bun run build

# Nginx Alpine Runtime
FROM nginx:alpine

COPY --from=builder /app/dist/quezap/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
