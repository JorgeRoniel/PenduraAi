FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG API_URL
RUN API_URL="$API_URL" node -e 'const fs = require("fs"); const file = "src/environments/environment.production.ts"; const apiUrl = process.env.API_URL; if (!apiUrl) throw new Error("API_URL deve ser informado no build"); const source = fs.readFileSync(file, "utf8"); fs.writeFileSync(file, source.replace("__API_URL__", apiUrl.replace(/\/$/, "")));'
RUN npm run build -- --configuration production

FROM nginx:alpine

COPY --from=build /app/dist/frontend-angular/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
