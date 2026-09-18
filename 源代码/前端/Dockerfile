FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
ENV TZ=Asia/Shanghai
EXPOSE 80
CMD ["nginx","-g","daemon off;"]