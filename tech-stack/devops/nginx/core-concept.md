# Nginx - Core Concept

#### Apache 단점을 해결한 Nginx

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/352d0a05-3d7a-4c21-b2e2-ae0749e96796.png)

* Nginx로 위와 같은 작업들을 처리할 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f56e2002-e404-4da8-b820-6a9260339d45.png)

* 전통적 Apache(prefork MPM)는 **연결 하나 = 프로세스(또는 스레드) 하나** 모델이다.
* 요청 10,000개가 동시에 들어오면 프로세스 10,000개가 필요한데, 프로세스마다 메모리를 수 MB씩 먹고 OS는 그 수만 개를 번갈아 실행하느라 컨텍스트 스위칭 비용이 폭증한다.
* 동시 접속 1만 개를 감당하지 못하는 이 한계가 그 유명한 **C10K 문제**다.
* 다만 Apache도 2.4의 event MPM부터는 이벤트 기반 처리를 도입해 이 격차를 상당히 줄였다. 아래 비교는 "전통적 prefork 모델"과의 비교로 읽어야 한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/2aef197d-e270-4beb-a1eb-e4c40521c3ac.png)

* 2004년 이고르 시소예프(Igor Sysoev)가 C10K를 풀기 위해 공개한 것이 NGINX이고, 핵심은 **이벤트 기반(event-driven) + 비동기 논블로킹 I/O**다.
* 연결마다 프로세스를 만들지 않고, 워커 몇 개(보통 CPU 코어 수)가 **각자의 이벤트 루프**로 수만 개의 연결을 다중화한다.
* 워커는 어떤 연결이 I/O를 기다리는 동안 멈춰 있지 않고, OS의 이벤트 알림(epoll/kqueue)으로 **지금 처리할 수 있는 연결만** 골라 처리한다.
* 연결이 늘어도 프로세스 수는 그대로이고 연결당 비용은 수 KB 수준이라, 1만 연결에도 메모리는 수십 MB대에 머문다.

> **그래서 NGINX는 프록시와 정적 서빙에 특화됐다.** 워커 하나가 수천 개의 연결을 물고 있으므로, 워커가 무거운 계산으로 블로킹되면 그 연결들이 전부 멈춘다. 그래서 NGINX는 I/O 중심의 일(파일 전송, 요청 중계, 암호화, 캐시)만 문 앞에서 끝내고, 진짜 계산은 뒤의 WAS로 넘기는 분업 구조를 갖는다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/5d23691b-5520-458a-8a5f-9ef4904d89c6.png)

* **정적 파일 서빙(Web Server)** : HTML·CSS·JS·이미지처럼 계산이 필요 없는 파일을 디스크에서 바로 내보낸다. 이벤트 기반 구조 덕에 WAS를 거치는 것보다 압도적으로 싸고 빠르며, sendfile 같은 커널 기능으로 파일을 유저 공간에 복사하지 않고 소켓으로 직송한다.
* **리버스 프록시(Reverse Proxy)** : 클라이언트의 요청을 대신 받아 뒤의 백엔드 서버로 전달한다. 백엔드의 존재(IP, 포트, 대수)를 바깥에서 숨기는 것이 핵심이다.
* **로드 밸런싱(Load Balancing)** : 리버스 프록시의 확장 개념으로 전달할 백엔드가 여러 대일 때 요청을 분산한다. 기본은 라운드 로빈이고, 연결이 적은 서버를 고르는 `least_conn`, 같은 클라이언트를 같은 서버로 보내는 `ip_hash`(세션 고정)를 선택할 수 있다.
* **SSL/TLS 터미네이션** : HTTPS 암호화·복호화를 NGINX가 전담하고, 내부망에서는 평문 HTTP로 통신한다. 인증서 관리 지점이 한 곳으로 모여 백엔드 N대에 인증서 배포가 불필요해지고, 암복호화 CPU 비용을 백엔드에서 제거할 수 있다.(내부 평문 통신은 내부망 신뢰를 전제한다. 제로 트러스트 환경이라면 내부 구간 암호화도 검토한다.)
* **HTTP 캐시** : 백엔드 응답을 저장해뒀다가 같은 요청에 백엔드를 거치지 않고 응답한다. 핵심 난제는 무효화로 원본이 바뀌었을 때 언제 캐시를 버릴지가 설계의 전부다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8be07aab-4bbc-4683-9b2e-ff883d46db1b.png)

* 워커가 요청 A를 백엔드로 넘긴 뒤 응답을 기다리며 멈추는 게 아니라, 그 사이 요청 B(정적 파일)를 처리해 먼저 응답한다. "비동기 논블로킹"이 실제로 뜻하는 것이 이 흐름이다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/77428383-7f62-4d25-ad7f-915be4865f9b.png)

* Master 프로세스는 설정 읽기·워커 관리·시그널 처리만 하고, **요청 처리는 전적으로 워커의 몫**이다. `reload` 시 master가 새 설정으로 워커를 갈아끼우기 때문에 무중단 설정 반영이 가능하다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/2e1c171d-90ea-4d2b-bad9-3f395e7038b3.png)

* 하나의 요청은 TCP 연결 수립 → 요청 파싱 → server 블록 매칭(Host 헤더) → location 블록 매칭(URI) → 처리(정적/프록시/리다이렉트) → 응답 → Keep-Alive 판단의 순서로 흐른다. 이 매칭 단계가 아래 Context Block 설정과 일대일로 대응한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d2a4778d-1100-4e4b-9f3b-42b2ec2491ea.png)

#### Nginx Context Block

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/eeb4b40e-7425-4cd2-8f8d-d658338f144e.png)

* `nginx.conf`는 딱 두 가지 요소로 이뤄진다. `이름 값` 형태의 설정 한 줄인 **지시어(directive)**와, 이름 `{ }` 형태로 지시어들을 묶어 적용 범위를 정하는 블록인 **컨텍스트(context)**다.
* `main(최상위)` : 중괄호 없이 파일 맨 바깥에 놓이는 영역. 프로세스 전체에 걸리는 설정이 들어간다. `worker_processes auto;`(워커 수를 CPU 코어 수에 맞춤), user, error_log, pid 등.
* `events { }` : 연결 처리 모델 설정. `worker_connections 1024;`는 워커 하나가 동시에 물 수 있는 최대 연결 수다. 내용이 없어도 블록 자체는 반드시 있어야 nginx가 뜬다.
* `http { }` : HTTP 서버 전반의 컨테이너. `include mime.types;(확장자 → Content-Type 매핑)`, `sendfile`, `keepalive_timeout`, `gzip` 같은 공통 설정과 여러 개의 server 블록이 들어간다.
* `server { }` : 가상 호스트(virtual host) 하나. `listen(포트)`과 `server_name(도메인)`의 조합이 어느 server 블록이 이 요청을 받을지를 결정하며, 요청의 Host 헤더와 server_name을 대조해 매칭한다.
* `location { }` : server 안에서 URI 경로별 처리를 정의한다. `location /`은 정적 서빙(root), `location /api`는 백엔드 중계(proxy_pass)처럼 경로마다 다른 동작을 지정하는 식이다.

```nginx
worker_processes auto;           # main : 프로세스 전체 설정

events { 
    worker_connections 1024;     # 워커당 동시 연결 수
}

http {
    include mime.types;          # http 공통 설정(하위로 상속된다)

    server {                     # 가상 호스트 : listen + server_name으로 매칭
        listen 8080;
        server_name localhost;

        location / {             # URI별 처리 : 정적 파일 서빙
            root /opt/homebrew/var/www;
            index index.html;
        }

        location /api {          # URI별 처리 : 백엔드로 중계
            proxy_pass http://localhost:3000;
        }
    }
}
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e2933d3f-93b8-4ec4-8a32-5c55d2d13b81.png)

* 계층은 `main → events` / `http → server → location` 순으로 좁아진다. 이 계층이 곧 요청 처리 흐름과 일대일로 대응한다. 앞 섹션의 요청 흐름에서 본 **server 블록 매칭(Host 헤더) → location 블록 매칭(URI)**이 정확히 이 중첩 구조를 따라 내려가는 과정이다.
* 상속(inheritance) : 상위 컨텍스트에 선언한 지시어는 하위 컨텍스트로 자동 상속되고, 하위에서 같은 지시어를 다시 선언하면 그 범위에서만 덮어쓴다. 예를 들어 root를 http에 한 번만 두면 모든 server/location이 물려받고, 특정 location만 재선언으로 다른 경로를 쓰게 할 수 있다. 반복 설정을 줄이는 핵심 메커니즘이다.
* location 매칭 우선순위 : `=(정확 일치)` → `^~(우선 접두사)` → `~/~*(정규식, 선언 순서대로)` → 최장 접두사 순이다. 접두사끼리는 더 긴 쪽이 이기므로 `/`와 `/api`가 같이 있으면 `/api/users` 요청은 `/api`로 간다. 선언 순서가 아니라 매칭 규칙이 우선한다는 점이 중요하다.

> 핵심은 "설정이 적힌 위치가 곧 적용 범위"라는 것. 같은 지시어라도 http에 적으면 모든 가상 호스트에, server에 적으면 그 호스트에만, location에 적으면 그 경로에만 걸린다. 설정 파일을 읽을 때는 항상 "이 지시어가 어느 컨텍스트 안에 있는가"부터 확인해야 한다.

#### Nginx 표준 문법 특수한 상속 기법과 덮어쓰기 방식

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/408c3afa-acd2-40a6-a981-dc6086f88621.png)

* 상속의 기본 원칙은 단순하다. 상위 컨텍스트에 선언한 지시어는 하위로 자동 상속되고, 하위에서 같은 지시어를 다시 선언하면 "가장 가까운 컨텍스트의 값"이 이긴다. `http`에 `gzip on;`을 두면 모든 `server/location`이 물려받고, 압축이 무의미한 경로(`location /raw`)에만 `gzip off;`를 선언해 그 범위만 덮어쓰는 식이다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/008eba70-d855-45df-a15e-6379cf5123dc.png)

* 단, 모든 지시어가 상속되는 것은 아니다. `listen`, `server_name`, 그리고 `location` 블록 자체는 상속 개념이 없다. 이들은 설정 값이 아니라 컨텍스트 자체를 정의하는 지시어라서, 물려받을 대상이 아니기 때문이다. 반면 `gzip`, `client_max_body_size`, `error_page`, `proxy_connect_timeout`, 로그 설정처럼 "동작 방식"을 정하는 지시어는 상속된다.
* 배열형 디렉티브의 전부-아니면-전무(all-or-nothing) 상속이다.
* `add_header`, `proxy_set_header`, `access_log`처럼 여러 번 선언해 값을 쌓는 디렉티브는 상속 시 병합되지 않는다. 하위 컨텍스트에서 같은 디렉티브를 하나라도 선언하는 순간, 상위에서 쌓은 것 전체가 통째로 무효화된다.

```nginx
server {
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;

    location /api {
        add_header Cache-Control no-store;
        # 이 한 줄로 위의 X-Frame-Options, X-Content-Type-Options는
        # /api 응답에서 전부 사라진다! (병합 X, 통째 교체 O)
    }
}
```
* `proxy_set_header`도 똑같은 함정이 있다. `server` 레벨에 `Host`, `X-Real-IP`를 잘 세팅해 두고 특정 `location`에서 헤더 하나를 추가하면, 기존 세팅이 전부 날아가 백엔드가 엉뚱한 `Host` 헤더를 받는 사고로 이어진다. "헤더 하나 추가했을 뿐인데 다른 헤더가 사라졌다"면 십중팔구 이 규칙 때문이다.

#### Include와 확장성을 고려하여 추상화 개념을 도입한 Nginx 모듈화 패턴

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/05cc0beb-4569-4ad7-8947-020d37ab6924.png)

* `include`는 지시어가 적힌 그 자리에 대상 파일의 내용을 그대로 읽어 끼워넣는 단순한 텍스트 치환이다. 특별한 링크나 참조 개념이 아니라서, `include`된 파일의 내용은 `include`가 적힌 컨텍스트의 규칙(문법·상속)을 그대로 따른다. `include servers/*.conf;`처럼 와일드카드를 쓰면 디렉토리의 파일 전부를 한 줄로 불러온다.
* 사실 `include`는 이미 곳곳에서 쓰이고 있다. `include mime.types;`가 대표적인데, 수백 줄짜리 확장자 → Content-Type 매핑 테이블을 본 파일 밖으로 분리해둔 것이다. **"길고 반복적인 설정은 파일로 빼고 본 파일에는 한 줄만 남긴다"**는 것이 `include`의 존재 이유다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d70d4621-2522-498d-a25b-a889af675629.png)

* 이를 확장하면 표준 모듈화 패턴이 나온다. 역할별로 디렉토리를 나누는 것이다.
* `nginx.conf(메인 설정)` : 뼈대만 남긴다. 전역 설정과 `include` 선언이 전부다.
* `servers/(사이트별 설정)` : 가상 호스트 하나 = 파일 하나. `blog.example.conf`, `api.example.conf`처럼 도메인 단위로 `server` 블록을 분리한다.
* `conf.d/(공통 설정)` : 여러 사이트가 공유하는 설정 조각(snippet). 보안 헤더 세트(security.conf), 압축 설정(gzip.conf), 프록시 공통 헤더(proxy.conf) 등을 둔다.

```nginx
# nginx.conf — 뼈대만 남긴다
worker_processes auto;

events {
    worker_connections 1024;
}

http {
    include mime.types;
    include conf.d/*.conf;      # 공통 설정 조각
    include servers/*.conf;     # 사이트별 server 블록
}
```

* 이 구조가 주는 것은 결국 변경의 격리다.
  * 사이트 추가 = `servers/`에 파일 하나 추가. 기존 파일은 건드리지 않으므로 다른 사이트가 깨질 일이 없다.
  * 사이트 제거 = 파일 삭제(또는 확장자를 `.conf` 밖으로 바꿔 비활성화).
  * 공통 정책 변경(보안 헤더 추가 등) = `conf.d/`의 파일 하나만 수정하면 `include`한 모든 곳에 일괄 반영된다.
* 파일 단위라서 git으로 변경 이력을 추적하기도 좋다.
* 배열형 디렉티브 문제와도 연결된다. `add_header` 같은 공통 세트를 `conf.d/security.conf`로 빼두고 필요한 `location`마다 명시적으로 `include`하면, 상속에 기대다 통째로 날리는 사고 없이 항상 전체 세트가 적용된다.

#### Nginx 처리량 관점에서의 Process Model Setting

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8308ec3a-8aea-4609-b503-e8e52163d364.png)

* 처리량을 결정하는 설정은 결국 두 개다. 워커를 몇 개 띄울 것인가(`worker_processes`)와 워커 하나가 연결을 몇 개까지 물 것인가(`worker_connections`). 앞에서 본 프로세스 구조 그대로, Master는 설정·워커 관리만 하고 실제 연결은 전부 워커들이 나눠 갖는다.
* `worker_processes`는 CPU 코어 수에 맞추는 것이 정석이고, `auto`가 그 역할을 한다. 워커는 이벤트 루프라서 코어 하나를 온전히 쓰며 논다는 개념이 없다. 코어보다 워커를 많이 띄워봐야 실행 시간을 나눠 갖느라 컨텍스트 스위칭 비용만 늘어난다. Apache의 "연결 수만큼 프로세스" 모델이 겪던 문제를 스스로 재현하는 꼴이 된다.
* `worker_connections`는 워커 하나의 동시 연결 한도다(기본 1024, events 컨텍스트에 선언). 연결 하나가 프로세스가 아니라 이벤트 루프에 등록된 소켓 하나일 뿐이므로, 이 숫자를 올리는 비용은 연결당 수 KB 수준의 메모리뿐이다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7f677215-9259-470c-830f-7bee9e3b2fbc.png)

```nginx
# main context
worker_processes auto;          # CPU 코어 수만큼 (예: 4코어 → 워커 4개)

events {
    worker_connections 1024;    # 워커당 동시 연결 한도
}
```
* 론상 최대 동시 연결은 곱셈으로 나온다. 4코어 × 1024 = 4,096 동시 연결. 단, 이 숫자를 그대로 믿으면 안 되는 이유가 두 가지 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/b4fcb65f-3cbe-4f9e-931e-35715cbfc6d4.png)

* 첫째, 리버스 프록시 구조에서는 요청 하나가 연결을 2개 소비한다. 클라이언트 ↔ NGINX 연결(연결 1)에 더해 NGINX ↔ 백엔드 연결(연결 2)도 같은 워커의 `worker_connections` 한도에서 차감된다. 즉 프록시로 쓸 때 실제 감당 가능한 동시 클라이언트는 **계산값의 절반(4,096 → 약 2,048)**이다.
* 둘째, OS의 파일 디스크립터 한도가 진짜 상한이다. 연결 하나 = fd 하나라서, `worker_connections`를 아무리 올려도 프로세스의 fd 한도(ulimit -n)를 넘을 수 없다. 그래서 연결 수를 올릴 때는 `worker_rlimit_nofile(main context)`로 fd 한도를 함께 올려야 하며, 관례상 `worker_connections`의 2배 이상으로 잡는다.

```nginx
worker_processes auto;
worker_rlimit_nofile 8192;      # fd 한도: worker_connections의 2배 이상

events {
    worker_connections 4096;
}
```
> 처리량 튜닝의 순서는 "워커 수는 코어에 고정, 연결 수는 fd 한도와 함께 증설"이다. 워커 수를 늘리는 것은 답이 아니고, 프록시라면 요청당 연결 2개를 계산에 넣어야 실측과 이론이 맞아떨어진다.

#### 성능을 최적화하는 방법과 설정하기 위한 기본 Nginx Template format

* 성능 관련 기본 템플릿은 사실상 네 줄로 정리된다. 각각이 커널·TCP 레벨의 낭비 하나씩을 제거하는 스위치다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/9e470235-7458-4809-be43-721ce16c2746.png)

* `sendfile on` — 복사 자체를 없앤다. 기본 방식은 디스크 → 커널 버퍼 → NGINX(사용자 공간) → 커널 버퍼 → 네트워크로 4단계를 거치는데, 정적 파일은 NGINX가 내용을 볼 필요가 없으므로 사용자 공간을 오가는 2·3단계 복사는 순수 낭비다. `sendfile`은 커널의 `sendfile()` 시스템 콜로 디스크에서 소켓으로 **커널 안에서 직송(zero-copy)**한다. 복사 횟수와 컨텍스트 스위칭이 절반으로 줄어, 정적 서빙 성능의 출발점이 된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a3a44bb3-c944-436a-bba2-b6441ceecfa4.png)

* `tcp_nopush on` — 패킷을 가득 채워 보낸다. 기본 동작은 준비되는 대로 조각조각 내보내서 HTTP 헤더 하나가 패킷 하나를 차지한다. `tcp_nopush(리눅스의 TCP_CORK)`는 패킷이 가득 찰 때까지 모았다가 전송해서, 헤더와 파일 첫 부분이 한 패킷에 실린다. 같은 데이터를 더 적은 패킷으로 보내므로 패킷당 고정 비용(헤더 40바이트 + 처리)이 줄어든다. `sendfile on`일 때만 동작하는 세트 옵션이다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ed19012a-6fd6-4339-a20f-19138a932689.png)

* `tcp_nodelay on` — 마지막 조각은 기다리지 않는다. TCP의 Nagle 알고리즘은 작은 데이터를 바로 보내지 않고 더 모이면 보내자며 기다린다. 대역폭에는 유리하지만 응답의 마지막 조각이 수십 ms 지연되는 대가가 있다. `tcp_nodelay`는 Nagle을 꺼서 작은 데이터도 즉시 내보낸다. API 응답처럼 작은 페이로드가 많은 서버에서 지연 시간을 줄이는 핵심이다.
* `tcp_nopush`와 `tcp_nodelay`는 모순처럼 보이지만, NGINX가 상황별로 나눠 쓴다. 응답 본문을 보내는 동안은 `nopush`로 패킷을 채우고, 마지막 불완전한 패킷 하나가 남으면 `nopush`를 풀고 `nodelay`로 즉시 보낸다. 그래서 셋을 모두 `on`으로 두는 것이 표준 템플릿이다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/cdb9a310-95d5-4a74-bbb3-989af5db4dcc.png)

* `keepalive_timeout 65` — 연결을 재사용한다. `Keep-Alive`가 없으면 요청마다 TCP 3-way handshake를 반복한다(HTTPS면 TLS 핸드셰이크까지 얹힌다). 켜두면 하나의 연결에서 여러 요청을 처리하고, 마지막 요청 후 65초가 지나면 워커가 연결을 닫아 `worker_connections` 슬롯을 회수한다. 값이 너무 크면 유휴 연결이 슬롯을 오래 점유하므로(앞 섹션의 동시 연결 한도와 트레이드오프), 60~75초 관례를 따른다.

> 역할 분담 : `sendfile`은 복사를, `tcp_nopush`는 패킷 수를, `tcp_nodelay`는 마지막 지연을, `keepalive`는 핸드셰이크를 줄인다. 각자 다른 계층의 낭비를 제거하므로 함께 켜는 것이 기본값이다.

#### 네트워크 전송 관점의 최적화를 위한 Compression Settings Template

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4fa8048c-58a3-4f8a-994e-bee66f1a74d1.png)

* 압축의 본질은 CPU 시간으로 전송 시간을 사는 거래다. 100KB HTML을 gzip으로 30KB까지 줄여 보내면(텍스트는 통상 60~80% 절감), 압축·해제에 드는 CPU 비용을 지불하는 대신 회선을 지나는 시간이 1/3로 준다. 회선이 느릴수록(모바일) 이득이 커지고, 내부망처럼 대역폭이 남는 구간에서는 이득이 거의 없다.

```nginx
http {
    gzip on;
    gzip_types text/css application/javascript application/json
               application/xml image/svg+xml;   # text/html은 기본 포함
    gzip_min_length 1024;    # 1KB 미만은 압축 안 함
    gzip_comp_level 5;       # 압축 강도 1~9
    gzip_vary on;            # Vary: Accept-Encoding 헤더 추가
}
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/228ed6b5-fb39-4c18-8649-ed70f246136f.png)

* `gzip_types` — 텍스트 계열만 지정한다. 기본값은 `text/html` 하나뿐이라 `CSS/JS/JSON`은 직접 추가해야 한다. 반대로 `JPEG/PNG/MP4/ZIP/WOFF2`처럼 이미 자체 압축된 포맷은 절대 넣지 않는다. 다시 압축해도 거의 줄지 않고 CPU만 태우며, 오히려 커지는 경우도 있다. 텍스트는 넣고, 바이너리는 빼는 것이 기준이다.
* `gzip_min_length` — 작은 응답은 건드리지 않는다. `gzip` 자체의 헤더/사전 오버헤드 때문에 수백 바이트짜리 응답은 압축해도 이득이 없거나 오히려 커진다. 1KB(1024) 근처가 관례다.
* `gzip_comp_level` — 높일수록 손해다. 19 중 6을 넘어가면 크기는 몇% 더 줄지 않는데 CPU는 가파르게 더 쓴다. 실시간 압축에는 46이 관례이고, 최고 압축이 필요하면 미리 압축해둔 `.gz` 파일을 서빙하는 `gzip_static` 모듈이 따로 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fee949be-7596-4a06-80fb-5aad6f768b34.png)

* `gzip_vary on` — 중간 캐시를 위한 안전장치다. 같은 URL이라도 응답이 `gzip` 버전과 원본 두 가지로 존재하게 되는데, 중간의 프록시/CDN이 이를 모르면 `gzip` 버전을 캐시해뒀다가 `gzip`을 해석 못 하는 클라이언트에게 그대로 내주는 사고가 난다. `Vary: Accept-Encoding` 헤더는 캐시에게 " 응답은 `AcceptEncoding` 값에 따라 다른 버전이 있으니 구분해서 저장하라"고 알려준다. NGINX 앞뒤로 캐시 계층이 있다면 반드시 켜야 한다.

> 압축 튜닝의 3요소는 대상(`gzip_types`), 하한(`gzip_min_length`), 강도(`gzip_comp_level`)다. 텍스트만, 1KB부터, 중간 강도로. 그리고 캐시가 끼는 순간 `gzip_vary`는 선택이 아니라 필수다.
