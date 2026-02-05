FROM node:20-alpine AS build
ARG VITE_TURN_URL
ARG VITE_TURN_USER
ARG VITE_TURN_PASS
ENV VITE_TURN_URL=$VITE_TURN_URL
ENV VITE_TURN_USER=$VITE_TURN_USER
ENV VITE_TURN_PASS=$VITE_TURN_PASS
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.25-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
