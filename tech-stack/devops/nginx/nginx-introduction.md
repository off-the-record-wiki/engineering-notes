# Nginx ‐ Nginx Introduction

#### Nginx 사용처

* 정적 컨텐츠 제공
* SSL 처리
* 로드 밸런싱
* 장애 대응
* 캐싱
* 보안 처리(IP 차단, 요청 수 제한)

#### Nginx 설치 및 실행하기

* Ubuntu 환경에서 Nginx 설치한다.

```shell
$ sudo apt update

$ sudo apt install curl gnupg2 ca-certificates lsb-release ubuntu-keyring

$ curl https://nginx.org/keys/nginx_signing.key | gpg --dearmor \
    | sudo tee /usr/share/keyrings/nginx-archive-keyring.gpg >/dev/null

$ gpg --dry-run --quiet --no-keyring --import --import-options import-show /usr/share/keyrings/nginx-archive-keyring.gpg

$ echo "deb [signed-by=/usr/share/keyrings/nginx-archive-keyring.gpg] \
http://nginx.org/packages/ubuntu `lsb_release -cs` nginx" \
    | sudo tee /etc/apt/sources.list.d/nginx.list

$ sudo systemctl status nginx

$ sudo systemctl start nginx
```

> [Nginx 공식문서](https://nginx.org/en/linux_packages.html#Ubuntu)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e5939cd1-22ca-4812-931f-86ca2f681fd8.png)

#### Nginx 로그 확인하는 방법

* Nginx의 로그 파일 위치는 `/var/log/nginx`에 있다.
* 해당 경로로 이동하면 access.log와 error.log 파일이 있다.
* access.log에는 Nginx 서버로 접근한 요청에 대한 정보 기록이 남아있고, error.log에는 에러 메시지에 대한 내용이 담겨 있다.

```shell
ubuntu@ip-172-31-40-249:/var/log/nginx$ tail -f access.log
58.233.115.254 - - [26/Jan/2025:14:28:47 +0000] "GET / HTTP/1.1" 200 615 "-" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36" "-"
58.233.115.254 - - [26/Jan/2025:14:28:47 +0000] "GET /favicon.ico HTTP/1.1" 404 555 "http://3.36.70.82/" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36" "-"
```
