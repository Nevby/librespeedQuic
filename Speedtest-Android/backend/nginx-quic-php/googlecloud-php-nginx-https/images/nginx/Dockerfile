FROM ranadeeppolavarapu/nginx-http3
COPY ./app /var/www/html
COPY ./config/nginx/nginx.conf /etc/nginx/nginx.conf
COPY ./config/nginx/ngx_conf /etc/nginx/ngx_conf
CMD ["nginx", "-g", "daemon off;"]