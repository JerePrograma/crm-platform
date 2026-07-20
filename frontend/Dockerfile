FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY package.json ./package.json
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=frontend-build /workspace/frontend/dist /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=12 \
  CMD wget -qO- http://localhost:8080/ >/dev/null || exit 1
