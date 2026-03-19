# Stage 1: Build the Angular application
# Actualizamos a Node 22 (LTS en 2026) para cumplir con los requisitos de Angular
FROM node:22-alpine AS build

WORKDIR /app

# Copy package.json and package-lock.json
COPY package*.json ./

# Install dependencies (usamos clean-install para asegurar consistencia)
RUN npm ci

# Copy the rest of the application files
COPY . .

# Build the application for production.
RUN npm run build -- --configuration production


# Stage 2: Serve the application from Nginx
FROM nginx:stable-alpine

# Copy the build output from the build stage to the Nginx html directory
# OJO: Asegúrate de que tu proyecto se llame exactamente "geovideo-frontend"
COPY --from=build /app/dist/geovideo-frontend/browser /usr/share/nginx/html

# Copy the custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80 for the web server
EXPOSE 80

# Comando para arrancar Nginx
CMD ["nginx", "-g", "daemon off;"]