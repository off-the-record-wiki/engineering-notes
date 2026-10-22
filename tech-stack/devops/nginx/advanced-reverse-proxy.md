# Nginx - Advanced Reverse Proxy

#### HTTP1.0/HTTP1.1을 기반으로 하는 Dynamic Reverse Proxy

**1. 리버스 프록시**

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d5afe233-9cf0-442d-9393-304ec0bdcb24.png)

```nginx
location /api/ {
    proxy_pass http://localhost:3000;
}

# proxy_pass = http://localhost:3000;
# proxy_pass = http://localhost:3000/;

# /api/users -> http://localhost:3000/api/users
# /api/users -> http://localhost:3000/users

upstream backend {
    server 127.0.0.1:3000;
    server 127.0.0.1:3001;
}

server {
    location /api/ {
        proxy_pass http://backend
    }
}

location /api/ {
    proxy_pass http://backend;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    proxy_buffering on;         # 버퍼링 활성화
    proxy_buffer_size 4k;       # 응답 헤더 버퍼
    proxy_buffers 8 4k;         # 응답 본문 버퍼
    proxy_busy_buffers_size 8k; # 클라이언트에 보내는 중인 버퍼
}

upstream backend {
    server 127.0.0.1:3000;
    server 127.0.0.1:3001;

    keepalive 32;
}

location /api/ {
    proxy_pass http://backend;

    proxy_http_version 1.1;
    proxy_set_header Connection "";
}
```

* 프록시가 서버 측에 서서 클라이언트의 모든 요청을 먼저 받고, 뒤에 있는 백엔드 A·B 중 하나로 전달한 뒤 응답을 대신 돌려준다.
* 클라이언트는 백엔드가 몇 대인지, 어떤 주소인지 전혀 알 수 없으므로 서버의 존재가 숨겨지고 내부 구조를 자유롭게 바꿀 수 있다.
* 로드 밸런싱, TLS 종료, 캐싱, WAF 같은 공통 관심사를 nginx·ALB 같은 한 지점에 모아 처리한다.

**2. 포워드 프록시**

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f760c745-b9e6-446d-8781-e54c9024925d.png)

* 프록시가 클라이언트 측에 서서 클라이언트 A·B의 요청을 대신 외부 서버로 보내고, 받은 응답을 각 클라이언트에게 돌려준다.
* 외부 서버 입장에서는 프록시의 IP만 보이기 때문에 이번에는 서버가 아니라 클라이언트의 존재가 숨겨진다.
* 사내 아웃바운드 통제, 접근 차단·감사 로그, 공용 캐시처럼 나가는 트래픽을 관리하는 용도로 쓰인다.

#### WebSocket을 기반으로 하는 Dynamic Reverse Proxy 및 서버 기능 테스트

```nginx
location /ws/ {
    proxy_pass http://backend;

    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;

    proxy_read_timeout 86400s; # 24시간
}
```
```nginx
worker_processes 1;

events {
    worker_connections 1024;
}

http {
    upstream api_backend {
        server 127.0.0.1:3000;
        server 127.0.0.1:3001;
        keepalive 32;
    }

    server {
        listen 80;
        server_name example.com;
    
        
        # API 프록시
        location /api/ {
            proxy_pass http://api_backend;
            
            # 헤더 설정
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            
            # keepalive
            proxy_http_version 1.1;
            proxy_set_header Connection "";
        }
        
        # WebSocket
        location /ws/ {
            proxy_pass http://api_backend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_read_timeout 86400s;
        }
    }
}
```
* WebSocket은 HTTP/1.1 Upgrade 핸드셰이크로 시작하므로 리버스 프록시가 Upgrade와 Connection 헤더를 그대로 넘겨줘야 연결이 성립하고, 이 때 `proxy_http_version` 1.1이 반드시 함께 있어야 한다. 백엔드 keepalive용으로 쓴 `proxy_set_header Connection "";`와 정면으로 충돌하므로, WebSocket 경로는 별도 location으로 분리하거나 map으로 값을 분기해야한다.
* 핸드셰이크가 끝나면 그 연결은 프록시가 해석하지 않고 그냥 TCP를 양방향으로 흘려보내는 터널이 되는데, nginx의 `proxy_read_timeout` 기본값 60초는 이 터널에도 그대로 적용된다. 트래픽이 뜸한 채팅·알림 소켓은 1분마다 조용히 끊기므로 타임아웃을 늘리거나 애플리케이션 레벨에서 ping/pong 하트비트를 보내야 한다.
* 로드 밸런싱은 핸드셰이크 시점 단 한 번만 일어나고 이후 연결은 그 백엔드에 고정되기 때문에, 라운드로빈이어도 연결 수가 인스턴스별로 불균등해질 수 있고 한 인스턴스가 죽으면 거기 붙은 소켓이 전부 끊긴다. 또 서로 다른 인스턴스에 붙은 클라이언트끼리는 메시지가 오가지 않으므로 Redis 같은 pub/sub 어댑터가 필요하고, Socket.IO처럼 HTTP 폴링으로 폴백하는 경우엔 `ip_hash` 등 sticky 설정이 필요하다.

#### Load Balancing Using Nginx

**1. L4 로드 밸런싱**

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/faf07227-ac63-4572-ada8-aa310ebc1704.png)

* L4는 TCP/UDP 헤더의 출발지·목적지 IP와 Port만 보고 연결을 통째로 한 서버에 넘기기 때문에, 패킷 내용을 파싱하지 않아 오버헤드가 거의 없고 HTTP든 gRPC든 DB 프로토콜이든 가리지 않고 분산할 수 있다.

**2. L7 로드 밸런싱**

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d7eb236b-a078-4992-8bb9-7a58193d8172.png)

* L7은 TLS를 종료하고 HTTP 메시지를 해석해 URL 경로·Host·헤더·쿠키까지 보고 분배하므로, `/api`는 API 서버로 `/static`은 정적 서버로 보내거나 쿠키로 sticky session을 잡는 식의 정교한 라우팅과 캐싱·압축·WAF 같은 부가 기능이 가능하다.
* 대신 L7은 요청마다 파싱과 암복호화 비용을 치르고 TLS 인증서를 로드밸런서가 들고 있어야 하며, 앞서 본 nginx 리버스 프록시가 바로 이 L7에 해당된다. 실제로 L4(NLB)로 트래픽을 받고 뒤에 L7(Nginx·ALB)을 두는 조합도 흔하다.

#### Load Balancing 5가지 알고리즘

* Round Robin(nginx 기본값)
  * 요청을 서버 목록 순서대로 하나씩 돌아가며 배정하는 가장 단순한 방식이고 `weight=`3을 주면 그 서버가 3배 자주 선택되도록 가중치를 걸 수 있다.
  * 모든 요청의 처리 비용이 비슷하다는 전제가 깔려 있어서, 어떤 요청은 10ms에 끝나고 어떤 요청은 5초씩 걸리는 환경에서는 특정 서버만 계속 밀리는 쏠림이 생긴다.
* Least Connections(least_conn)
  * 현재 활성 연결 수가 가장 적은 서버로 보내기 때문에, 요청마다 처리 시간이 들쑥날쑥한 워크로드에서 Round Robin보다 훨씬 고르게 부하가 나뉜다.
  * WebSocket처럼 연결이 오래 유지되는 경우엔 연결 수 = 실제 부하에 가까워서 특히 잘 맞는다. nginx의 연결 수는 워커 프로세스별로 집계되므로 완벽히 전역 최적은 아니다.
* IP Hash(ip_hash)
  * 클라이언트 IP(IPv4는 앞 3옥텟)를 해시해 항상 같은 서버로 보내므로, 세션을 서버 메모리에 들고 있을 때 sticky session을 손쉽게 만들 수 있다.
  * NAT나 회사망 뒤의 수많은 사용자가 같은 IP로 묶여 한 서버에 몰릴 수 있고, 서버를 추가·제거하면 해시 분포가 바뀌면서 상당수 클라이언트의 매핑이 재배치될 수 있다.
* Generic Hash(hash $key)
  * 해시 키를 직접 고를 수 있어서 `hash $request_uri(캐시 적중률 최적화)`, `hash $cookie_sid(세션 고정)`, `hash $arg_userid`처럼 도메인에 맞는 고정 규칙을 만들 수 있다.
  * `hash $key consistent;`를 붙이면 컨시스턴트 해싱이 적용돼, 서버가 늘거나 줄어도 재배치되는 키가 1/N 수준으로 제한된다. 캐시 서버 계층이면 사실상 필수다.
* Random(random)
  * 무작위로 고르는 방식이며, 로드밸런서가 여러 대라 서로의 상태를 모르는 분산 환경에서 Round Robin보다 오히려 쏠림이 덜하다.
  * `random two least_conn;` 형태의 "두 개 뽑아 더 나은 쪽 선택(power of two choices)"이 실질적인 사용법으로, 전역 상태를 보지 않으면서도 Least Connections에 가까운 균형을 얻을 수 있다.

#### [Nginx 성능 최적화 방법](https://blog.nginx.org/blog/performance-tuning-tips-tricks)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e2b559bc-b0d1-445c-a363-047421a2240e.png)

* 리눅스에서는 열려 있는 파일뿐 아니라 네트워크 연결(소켓)도 전부 "파일 디스크립터(FD)"라는 번호표를 하나씩 받아가며, 프로세스가 동시에 가질 수 있는 번호표 개수에는 상한이 있다.
* 리버스 프록시는 클라이언트와 연결 하나, 백엔드와 연결 하나를 동시에 열어두기 때문에 요청 한 건에 FD를 2개 쓴다. `worker_connections 4096`은 동시 요청 4096개가 아니라 실질적으로 약 2048건이다.
* 트래픽이 몰릴 때 로그에 Too many open files가 찍히면 코드 문제가 아니라 이 상한에 걸린 것이다. 그럴 경우에 nginx 설정(`worker_connections`, `worker_rlimit_nofile`)과 OS 설정(`ulimit -n`)을 올려야 한다.

#### HTTPS SSL/TLS CA 인증서 적용 및 HTTP/2 Protocol 구현

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/860bf267-3f94-4b6d-8276-d408a69b5852.png)

* HTTP는 요청과 응답을 평문으로 보내기 때문에 같은 와이파이에 있는 사람, ISP, 경유하는 프록시 등 경로상의 누구나 비밀번호·쿠키·토큰을 그대로 읽을 수 있고, 내용을 바꿔치기해도 양쪽이 알아채지 못한다.
* HTTPS는 TLS로 감싸서 기밀성(내용 암호화), 무결성(변조 시 검증 실패), 인증(인증서로 상대가 진짜 그 서버인지 확인) 세 가지를 동시에 제공하므로, 흔히 "암호화"로만 설명되지만 실제로는 인증서 검증이 중간자 공격을 막는 핵심이다.
* 다만 암호화되는 것은 URL 경로·헤더·본문이고 접속한 도메인(SNI)과 IP는 여전히 노출되며, TLS는 전송 구간만 보호하므로 앞서 다룬 리버스 프록시에서 TLS를 종료하면 그 뒤 백엔드 구간은 다시 평문이 된다. 신뢰할 수 없는 네트워크를 지난다면 그 구간도 따로 암호화해야한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/0a044556-3498-4b37-ad3a-e2f61678e2c0.png)

* 핸드셰이크의 목적은 암호화 자체가 아니라 상대가 진짜 그 서버인지 확인하고, 둘만 아는 세션키를 만드는 것이다. ClientHello/ServerHello로 사용할 TLS 버전과 암호 스위트를 맞추고, 서버 인증서로 신원을 확인한 뒤 키 교환을 마친다.
* 브라우저의 인증서 검증이 실제 보안의 핵심이라서 CA 체인·도메인 일치·유효기간 중 하나라도 어긋나면 그 자리에서 연결을 끊는다. 자체 서명 인증서를 쓸 때 나오는 경고가 바로 이 단계이고, 이 검증이 없다면 중간자가 자기 공개키를 끼워 넣어 통신을 통째로 가로챌 수 있다.
* 신원 확인과 키 합의에는 느린 비대칭키 연산을 쓰고, 핸드셰이크가 끝난 뒤 실제 데이터는 빠른 대칭키(세션키)로 암호화한다. 이 왕복 비용 때문에 keepalive로 연결을 재사용하는 것이 HTTPS 환경에서 특히 중요하며, TLS 1.3에서는 이 과정이 1-RTT로 줄어들었다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/53fdfcad-6331-453c-b9ff-28815cae51a7.png)

* HTTP/1.1은 한 연결에서 앞 요청의 응답이 끝나야 다음 요청이 나가는 구조라, 느린 응답 하나가 뒤의 모든 요청을 붙잡는 HOL 블로킹이 생긴다. 브라우저가 도메인당 연결을 6개까지 여는 것은 이 제약을 우회하려는 임시방편이고, "연결 여러 개"는 원인이 아니라 결과다.
* HTTP/2는 요청·응답을 프레임 단위로 쪼개 하나의 연결 안에서 여러 스트림으로 뒤섞어 주고받으므로 서로를 기다리지 않으며, 여기에 바이너리 프레이밍·HPACK 헤더 압축·스트림 우선순위가 더해져 연결 수와 헤더 낭비가 동시에 줄어든다.
* 다만 HTTP/2가 해결한 것은 HTTP 레벨뿐이라 그 아래 TCP는 여전히 순서를 지키고, 패킷 하나가 유실되면 그 연결의 모든 스트림이 함께 멈춘다. 이 TCP 레벨 HOL 블로킹을 UDP 기반 QUIC으로 푼 것이 HTTP/3이다.
