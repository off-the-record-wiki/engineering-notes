# Nginx ‐ Nginx Supplementary Summary

#### 웹 서버(Web Server)

* 웹 서버(Web Server)란, 정적 컨텐츠(HTML, CSS, JS, 이미지)를 제공해주는 컴퓨터를 말한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e0a8da09-6f8d-481c-9238-9b85e77bf39c.png)

#### Nginx 기본 문법

> `/etc/nginx/conf.d/default.conf`

```bash
# server : '하나의 웹 사이트에 관련된 설정'을 관리하는 단위
server {

    # localhost:80으로 들어오는 요청을 이 server 블럭에서 처리하도록 설정
    listen       80;
    server_name  localhost;

    #access_log  /var/log/nginx/host.access.log  main;

    # / 으로 시작하는 모든 경로를 처리 (ex. /index.html)
    location / {
        root   /usr/share/nginx/html;

        # /로 요청이 들어오면 /usr/share/nginx/html/index.html로 응답
        # 만약 /usr/share/nginx/html/index.html이 없을 경우, /usr/share/nginx/html/index.htm으로 응답
        index  index.html index.htm;
    }

    # Nginx에서 500, 502, 503, 504의 상태 코드가 발생했을 때 /50x.html로 redirect
    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   /usr/share/nginx/html;
    }
    
    # /50x.html과 완전히 일치하는 경로를 처리
    location = /50x.html {
        # /50x.html로 요청이 들어오면 /usr/share/nginx/html/50x.html 파일로 응답
        root   /usr/share/nginx/html;
    }

    }
}
```

> Nginx 설정 파일 문법 에러있는 지 확인하기

```shell
# Nginx 설정 파일 중 문법 에러가 있는지 체크
$ sudo nginx -t

# Nginx 설정 파일이 변경된 경우 아래 명령어를 입력해서 최신 설정 파일을 반영
$ sudo nginx -s reload
```
