# Stage 1: Build the Angular application
FROM node:18-alpine AS build

WORKDIR /app

# Copy package.json and package-lock.json
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy the rest of the application files
COPY . .

# Build the application for production. The output will be in /app/dist/geovideo-frontend/browser
RUN npm run build -- --configuration production


# Stage 2: Serve the application from Nginx
FROM nginx:1.21-alpine

# Copy the build output from the build stage to the Nginx html directory
COPY --from=build /app/dist/geovideo-frontend/browser /usr/share/nginx/html

# Copy the custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80 for the web server
EXPOSE 80
