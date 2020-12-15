FROM php:fpm-alpine
COPY ./app /var/www/html
COPY ./config/php/php.ini /usr/local/etc/php/php.ini
EXPOSE 9000
CMD ["php-fpm"]