# 1단계: 빌드용 Node.js 이미지 (Node.js 20 사용)
FROM node:20 AS build

WORKDIR /app

# package.json, package-lock.json 복사
COPY package*.json ./

# 패키지 설치 (캐시 정리 및 재설치)
RUN npm cache clean --force && \
    rm -rf node_modules package-lock.json && \
    npm install

# 소스 전체 복사
COPY . .

# 환경변수 파일 복사 (존재하는 경우에만)
COPY .env.production .env

# 빌드 실행 (플랫폼 호환성 개선)
RUN npm run build

# 2단계: nginx로 정적 파일 서비스
FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
