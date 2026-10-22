FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY package*.json ./
RUN if [ -f package-lock.json ]; then \
      npm ci --no-audit --no-fund; \
    else \
      npm install --no-audit --no-fund; \
    fi
COPY . .
RUN npm run build

FROM nginx:1.30.6-alpine@sha256:c59d1f8b1e0e42da75be973424cd72e23f7f8adfb54e982f4d1cd0216a5a5576
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx-trusted-edge.conf /etc/nginx/crm-trusted-edge.conf
COPY --from=frontend-build /workspace/frontend/dist /usr/share/nginx/html
RUN apk del --no-network curl \
    && ! apk info --exists curl \
    && ! apk info --exists libcurl \
    && sed -i 's#^pid .*;#pid /tmp/nginx.pid;#' /etc/nginx/nginx.conf
USER 101
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=12 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null || exit 1
ENTRYPOINT ["nginx", "-g", "daemon off;"]
