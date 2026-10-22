# Nginx ‐ Backend Deployment via Nginx Reverse Proxy

#### 리버스 프록시(Reverse Proxy) 개념

* 프록시(Proxy)란, "중계"의 의미를 가진다.
* 프록시 서버는 중계 서버를 말한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fdbcdcc1-c5b7-472a-a390-8c6776864ce5.png)

* 위와 같이 원 서버(Origin Server)의 정보를 노출시키지 않도록 하기 위해 앞단에 중계 서버(Proxy Server)를 두는 방식을 리버스 프록시라고 한다.

#### 포워드 프록시(Forward Proxy) 개념

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/90720dee-b70d-4ed8-9b08-08bd26a890c0.png)

* 반대로 위와 같이 보내려고 하는 요청을 관리 또는 보안 처리를 위한 용도로 사용하는 서버를 포워드 프록시 서버라고 한다.
* 리버스 프록시를 적용하는 이유는 들어오는 요청을 관리 또는 보안 처리를 하기 위해 사용하기 위함이다. 만약 리버스 프록시를 적용하지 않고 직접적으로 들어오는 요청을 백엔드 서버가 받아 처리하게 된다면 보안적으로 문제가 된다.
* 악의적인 사용자가 수많은 요청을 갑자기 보내는 경우 실제 백엔드 서버가 그 요청을 받아서 처리하려고 하기 때문에 과부하가 걸려 서버에 장애가 발생하게 된다.

#### 리버스 프록시를 활용해 Spring Boot 서버 배포 예제

```shell
# /etc/nginx/conf.d/websites/api.jangwoojin.p-e.kr.conf
server {
                                
        listen 80;
        server_name api.jangwoojiin.p-e.kr;

        # / 으로 시작하는 모든 경로를 처리
        location / {

                # 들어온 요청을 전부 http://localhost:8080(Spring Boot 서버)로 전달
                proxy_pass http://localhost:8080;
        }
}
```
```shell
# /etc/nginx/conf.d/default.conf
include conf.d/websites/*.conf;
```

#### Spring Boot 서버에 무료 도메인 적용 예제

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d2c6bf0f-3f61-4635-824c-8b75d9aff5ec.png)

#### Spring Boot 서버에 HTTPS 적용 예제

```shell
$ sudo certbot --nginx -d <도메인 네임명>
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/aa448d13-f830-4d70-90cb-984f0ca00488.png)

> HTTPS 인증서 발급 후 HTTPS 관련 내용이 추가되었는지 확인

```shell
server {

        server_name api.jangwoojin.p-e.kr;
        
        # / 으로 시작하는 모든 경로를 처리
        location / {
                
                # 들어온 요청을 전부 http://localhost:8080(Spring Boot 서버)로 전달
                proxy_pass http://localhost:8080;
        }       

        listen 443 ssl; # managed by Certbot
        ssl_certificate /etc/letsencrypt/live/api.jangwoojin.p-e.kr/fullchain.pem; # managed by Certbot
        ssl_certificate_key /etc/letsencrypt/live/api.jangwoojin.p-e.kr/privkey.pem; # managed by Certbot
        include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
        ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot
}

server {
    if ($host = api.jangwoojin.p-e.kr) {
        return 301 https://$host$request_uri;
    } # managed by Certbot


    listen 80;
    server_name api.jangwoojin.p-e.kr;
    return 404; # managed by Certbot
}
```

#### IP당 요청 수 제한 예제

* 리버스 프록시를 적용하지 않게 되면 악의적인 사용자에 의한 API 과다 호출로 서버에 과부하가 일어나 서버에 장애가 발생할 수 있다.
* 작성한 파일에 IP당 요청 수 제한 설정 코드를 추가해주면 된다.

```shell
# limit_req_zone : 요청 수를 제한하기 위한 메모리 공간(zone)과 요청 속도(rate)를 정의
# $binary_remote_addr : 요청 수를 제한하는 기준을 클라이언트의 IP로 설정
# one=mylimit:10m : 메모리 공간(zone)의 이름을 mylimit이라고 지정, 메모리 공간의 크기를 10MB 제한 (약 16만개의 IP 주소를 관리할 수 있음)
# rate=3r/s : 1초에 최대 3개의 요청만 허용
limit_req_zone $binary_remote_addr zone=mylimit:10m rate=3r/s;

server {

        server_name api.jangwoojin.p-e.kr;
        limit_req zone=mylimit;  # limit_req_zone에서 정의한 mylimit이라는 조건을 이 server 블럭에 적용
        limit_req_status 429;    # 요청이 제한됐을 때 429(Too Many Requests) 상태 코드를 반환

        # / 으로 시작하는 모든 경로를 처리
        location / {
                                        # 들어온 요청을 전부 http://localhost:8080(Spring Boot 서버)로 전달
                proxy_pass http://localhost:8080;
        }

    listen 443 ssl; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/api.jangwoojin.p-e.kr/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/api.jangwoojin.p-e.kr/privkey.pem; # managed by Certbot
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot

}
server {
    if ($host = api.jangwoojin.p-e.kr) {
        return 301 https://$host$request_uri;
    } # managed by Certbot


        listen 80;
        server_name api.jangwoojin.p-e.kr;
    return 404; # managed by Certbot


}
```

> 고의적으로 새로 고침을 여러 번 시도하면 다음과 같이 429 상태 코드가 반환된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8f8bd0ff-8b5d-4a64-b091-60244c534a85.png)
