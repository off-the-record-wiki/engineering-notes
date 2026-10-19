# Nginx - Advanced Concept

#### 정적 파일 서빙 Path Matching 기법 root와 alias

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/273b1fca-9726-487d-9566-df2e88409a22.png)

```nginx
server {
    listen 8080;
    root /var/www/html;

    location /images {
        root /var/www/static;
        # /images/logo.png

        # alias /var/www/static/img;
    }

    # 1. URI와 폴더 구조가 일치 = root
        # /css/style.css -> /var/www/html/css/style.css

    location /css {
        root /var/www/html;
    }

    # 2. URI와 폴더 구조가 다르다 = alias
        # /static/style.css -> /var/www/assets/style.css

    location /static {
        alias /var/www/assets;
    }
        
}
```

* `root`는 지시어에 적은 디렉터리를 기준으로 삼아 요청 URI 전체를 그 뒤에 그대로 이어 붙이기 때문에, `root /var/www/static;` 아래에서 `/images/logo.png`를 요청하면 서버는 `/var/www/static/images/logo.png`를 찾는다.
* `alias`는 기준점이 아니라 치환이어서 매칭된 `location` 경로를 잘라낸 뒤 나머지만 붙이므로, 같은 요청이 `alias /var/www/static/;`에서는 `/var/www/static/logo.png`로 해석된다.
* 따라서 `location /images/` 블록 안에서 `alias`처럼 생각하고 `root /var/www/static/images;`라고 쓰면 경로에 `images`가 두 번 들어가 404가 발생하므로 디렉터리 이름과 URL 경로가 일치할 때는 `root`를, 서로 다를 때는 `alias`를 쓰는 편이 안전하다.
* 이미지, CSS, JS, 폰트 같은 건 애플리케이션(Django, Spring, Node 등)까지 갈 필요가 없다. Nginx가 파일을 찾아서 바로 응답하니 앱 프로세스는 아예 깨어나지 않고, 그만큼 응답이 빠르고 앱 서버 부하도 줄어든다.
* 파일 배치를 바꾸거나 URL 구조를 바꿔도 설정 한 줄만 고치면 된다. 코드 수정·재배포가 필요 없다.
* 캐시 헤더, gzip, Range 요청(동영상 구간 재생) 같은 것도 Nginx가 알아서 처리해준다.

#### 여러 도메인 호스팅을 위한 Virtual Host Setting

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f321bb1a-e082-489e-ac46-ff84c2f379bf.png)

```nginx
http {
    server {
        listen 80;
        server_name example.com www.example.com
        root /var/www/example;
    }

    server {
        listen 80;
        server_name blog.example.com;
        root /var/www/blog;
    }

    server {
        listen 80;
        server_name api.example.com;
        root /var/www/api;
    }

    server {
        listen 80 default_server;
        server_name _;

        return 444;
    }
}
```
```nginx
events {

}

http {
    # 1. 기본 설정되지 않은 요청 처리 (default_server)
    server {
        listen 80 default_server;
        server_name _;
        return 444;  # NGINX 전용 코드: 연결 끊기
    }

    # 2. 메인 사이트 (정확한 일치 - 1순위)
    server {
        listen 80;
        server_name example.com www.example.com;
        root /var/www/example;
        
        location / {
            return 200 "Main Site: example.com\n";
            add_header Content-Type text/plain;
        }
    }

    # 3. 블로그 사이트 (서브도메인)
    server {
        listen 80;
        server_name blog.example.com;
        root /var/www/blog;
        
        location / {
            return 200 "Blog Site: blog.example.com\n";
            add_header Content-Type text/plain;
        }
    }

    # 4. API 서버 (포트 분리)
    server {
        listen 8080;
        server_name example.com;
        root /var/www/api;
        
        location / {
            return 200 "API Server: example.com:8080\n";
            add_header Content-Type text/plain;
        }
    }

    # 5. 와일드카드 매칭 (앞쪽 와일드카드 - 2순위)
    server {
        listen 80;
        server_name *.example.com;
        
        location / {
            return 200 "Wildcard Match: $host\n";
            add_header Content-Type text/plain;
        }
    }
}
```

* 서버는 한 대(IP도 하나, 포트도 80/443)인데, 요청이 전부 하나로 들어온다.
* Nginx는 HTTP 요청 헤더의 `Host:`값을 보고, 그 값과 `server_name`이 일치하는 `server` 블록으로 요청을 넘긴다.

#### Redirection과 Rewrite Directive

* Rewrite : 서버 안에서만 바뀐다. 브라우저는 자기가 요청한 URL 그대로라고 믿는다. Nginx가 내부적으로 URI를 갈아끼우고 처리 단계를 다시 태울 뿐이라, 왕복은 한 번이고 주소창도 그대로다.
* Redirect : 클라이언트에게 특정 위치로 이동하라고 시킨다. Nginx는 301/302와 Location 헤더만 돌려주고 끝낸다. 실제 페이지를 가져오는 건 브라우저가 새 URL로 요청을 한 번 더 보내기에 왕복 2회가 되고 주소창도 바뀐다.

#### Redirection Status Code

* **301 Moved Permanently** : 검색엔진이 색인을 새로운 URL로 옮기고, 기존 링크 점수도 넘겨준다. 브라우저가 강하게 캐시하는 것이 함정이라, 한 번 잘못 내보내면 사용자 측 캐시를 지우기 전까지 설정을 고쳐도 계속 그리로 간다. HTTP → HTTPS 강제, 도메인 통합에 사용된다.
* **302 Found** : 명세상 메서드를 바꾸면 안 되는데, 초기 브라우저들이 POST를 GET으로 바꿔버렸고 그게 사실상 표준으로 굳어졌다. 그래서 301/302는 메서드 보존을 신뢰할 수 없다.
* **303 See Other** : POST 처리 후 결과 페이지로 보낼 때 이 상태 코드를 사용한다. 이게 없으면 사용자가 새로고침할 때 브라우저가 폼을 다시 제출할지를 묻고 주문이 중복 생성되어버리는데 이 패턴을 PRG라고 부른다.
* **307 Temporary Redirect** : 302의 제대로 된 버전이다. 의미는 같은 임시 이동인데 메서드와 본문을 그대로 유지하라고 명세에 못 박은 것이다. POST 요청이 다시 POST로 간다. API처럼 GET이 아닌 요청이 오가는 곳의 임시 이전에 사용한다.
* **308 Permanent Redirect** : 301의 제대로 된 버전이다. 영구 이동 + 메서드 유지이다. API 엔드포인트를 영구 이전할 때 301을 쓰면 클라이언트의 POST가 GET으로 바뀌어 조용히 깨지므로 이 경우는 308이 정답이다.

#### Rewrite Flag

* **last** : 바뀐 URI를 들고, `location` 선택 단계로 되돌아간다. 새로운 URI에 맞는 다른 `location` 블록이 처리하고 싶을 때 쓴다.
* **break** : 현재 `location`에 눌러앉는다. URI만 바꾸고 지금 블록 안에서 계속 진행한다.
* **redirect** : 302 임시. 캐시가 안 된다.
* **permanent** : 301 영구. 브라우저가 캐시하고 검색엔진이 색인을 옮긴다.
