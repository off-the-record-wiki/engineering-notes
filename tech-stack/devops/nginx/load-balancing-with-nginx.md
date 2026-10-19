# Nginx ‐ Load Balancing with Nginx

#### 로드 밸런서(Load Balancer)란?

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/2f86d55a-e9ea-42f0-ba9c-d2b92a58141e.png)

* 많은 트래픽이 발생해 한 대의 서버로는 사양이 부족한 경우가 발생한다.
* 이를 해결하기 위해 여러 서버를 운영하기도 한다.
* 사용자의 요청을 여러 대의 웹 애플리케이션 서버에 골고루 전달하기 위한 분산 장치가 필요한데 이것을 바로 로드 밸런서(Load Balancer)라고 한다.
* Nginx를 로드 밸런서로 활용하게 되면 아래와 같은 플로우를 보이게 된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3aacdaf7-67eb-4c0f-9480-835184805473.png)

* 결과적으로 Nginx를 도입해 Load Balancer 역할을 수행하면서 트래픽을 적절하게 분배가 가능해진다.

#### 하나의 EC2에서 백엔드 서버 2개를 로드 밸런싱하는 예제

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/8f2fd95d-472a-4197-b57f-88fd0b1ad60a.png)

* 서버를 2개 띄워(8080번, 8081번) Nginx를 앞단에 배치하여 로드 밸런서를 적용해보려고 한다.

```shell
limit_req_zone $binary_remote_addr zone=mylimit:10m rate=3r/s;

# 로드 밸런싱 대상 서버들을 upstream이라는 그룹으로 묶는다.
# upstream 그룹 이름을 backend라고 지정한다.
upstream backend {
        server localhost:8080;
        server localhost:8081;
}

server {

        server_name api.jangwoojin.p-e.kr;
        limit_req zone=mylimit;
        limit_req_status 429;

        # / 으로 시작하는 모든 경로를 처리
        location / {

                # upstream 그룹에서 지정한 서버들로 요청이 분산된다.
                proxy_pass http://backend;
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

> health 메서드로 로드 밸런서가 제대로 동작을 하는지 확인

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ecdad3b6-8e98-4bb1-b6cd-a114d5c805c6.png)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/0be0a3e6-dfa3-4c40-af7d-4df8d55e6e72.png)
