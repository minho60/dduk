FROM nginx:alpine

# Copy static frontend assets
COPY . /usr/share/nginx/html

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom Nginx configuration template for dynamic PORT and BACKEND_URL rendering
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

ENV PORT=80
ENV BACKEND_URL=http://backend:8080

EXPOSE 80
