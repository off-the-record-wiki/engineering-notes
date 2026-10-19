# Nginx ‐ Implementing HTTPS with Nginx

#### HTTPS(HTTP Secure)

* 보안적인 이유 : 데이터를 서버와 주고 받을 때 암호화를 시켜서 통신을 한다. 암호화를 하지 않으면 누군가 중간에서 데이터를 가로채서 해킹할 수도 있다. 보안을 위해서라면 HTTPS는 필수이다.
* 사용자 이탈 : 보안이 지켜지지 않아 보안 관련 경고가 표시된다면 사용자가 들어오지 않게 된다.

> [HTTPS MDN Web Docs](https://developer.mozilla.org/ko/docs/Glossary/HTTPS)

#### Nginx, Certbot을 활용해 HTTPS 적용하기

* Certbot이란, Let's encrypt를 이용해서 SSL 인증서를 발급, 갱신할 수 있게 도와주는 무료 오픈소스이다.

> [https://certbot.eff.org/instructions?ws=nginx&os=snap](https://certbot.eff.org/instructions?ws=nginx&os=snap)

* Certbot 설치하기

```shell
$ sudo snap install --classic certbot
$ sudo ln -s /snap/bin/certbot /usr/bin/certbot
```

* HTTPS 인증서 발급 받기

```shell
$ sudo certbot --nginx -d <도메인 주소>
```

* 무료 도메인을 EC2 IP와 연결한 후 HTTPS 인증서를 발급받으면 아래와 같이 정상적으로 발급이 된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/642319a0-e531-473d-ba57-617aa8c12f4d.png)

* `https://` 프로토콜로 입력하니 아래와 같은 결과가 떴다. 앞서 배웠던 Nginx 로그 분석을 통해 알아보았더니 아래와 같았다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/cbf140f5-c486-403e-bd3c-2895abc08352.png)

```shell
$ ubuntu@ip-172-31-40-249:/var/log/nginx$ tail -f access.log
206.189.2.13 - - [26/Jan/2025:16:33:44 +0000] "GET /?rest_route=/wp/v2/users/ HTTP/1.1" 403 153 "-" "Go-http-client/1.1" "-"
205.169.39.192 - - [26/Jan/2025:16:33:56 +0000] "GET / HTTP/1.1" 403 555 "-" "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36" "-"
34.56.73.132 - - [26/Jan/2025:16:34:09 +0000] "GET / HTTP/1.1" 403 153 "-" "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/29.0" "-"
106.75.169.16 - - [26/Jan/2025:16:37:02 +0000] "GET / HTTP/1.1" 301 169 "https://smtp.neria-tech.com" "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36" "-"
106.75.169.16 - - [26/Jan/2025:16:37:07 +0000] "GET / HTTP/1.1" 403 555 "https://smtp.neria-tech.com" "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36" "-"
106.75.169.16 - - [26/Jan/2025:16:37:11 +0000] "GET /favicon.ico HTTP/1.1" 404 555 "https://jangwoojin.p-e.kr" "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36" "-"
106.75.175.130 - - [26/Jan/2025:16:37:13 +0000] "GET / HTTP/1.1" 403 555 "http://www.automailertracking.com" "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36" "-"
106.75.175.130 - - [26/Jan/2025:16:37:18 +0000] "GET /favicon.ico HTTP/1.1" 404 555 "https://jangwoojin.p-e.kr" "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36" "-"
58.233.115.254 - - [26/Jan/2025:16:38:26 +0000] "GET / HTTP/1.1" 403 555 "-" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36" "-"
58.233.115.254 - - [26/Jan/2025:16:38:26 +0000] "GET /favicon.ico HTTP/1.1" 404 555 "https://jangwoojin.p-e.kr/" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36" "-"
```
