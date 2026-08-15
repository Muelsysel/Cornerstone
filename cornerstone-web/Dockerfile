# Cornerstone 前端多阶段构建：node 构建产物 → nginx 静态托管 + API 反向代理
# 构建镜像：docker compose build frontend  （或 docker build -t cornerstone-web ./cornerstone-web）

# ---- 阶段 1：构建 ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- 阶段 2：运行（nginx）----
FROM nginx:1.27-alpine
# 静态产物
COPY --from=build /app/dist /usr/share/nginx/html
# nginx 配置：SPA 回退 + /auth /system /demo 反代到网关
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
