FROM php:8.3-cli

RUN docker-php-ext-install pdo_mysql
WORKDIR /app
COPY api/ ./api/
COPY web/ ./web/
COPY index.php router.php health.php ./
RUN mkdir -p uploads web/uploads/patrullas && chmod -R 777 uploads web/uploads

EXPOSE 10000
CMD ["sh", "-c", "php -S 0.0.0.0:${PORT:-10000} -t /app /app/router.php"]
