# HTTP - HTTP Basic

## 📚 클라이언트 서버 구조

* Request - Response 구조
* 클라이언트는 서버에 요청을 보내고 응답을 대기한다.
* 서버가 요청에 대한 결과를 만들어서 응답한다.

## 📚 Stateful, Stateless

* 서버가 클라이언트 상태를 보존하지 않는다.
* 상태 유지 : 항상 같은 서버가 유지되어야 한다. 서버 장애가 발생할 경우 클라이언트의 상태 역시 모두 소실된다.
* 무상태 : 아무 서버나 호출해도 무방하다. 
* 모든 것을 무상태로 설계할 수 있는 경우도 있고 없는 경우도 있다.
* 로그인한 사용자의 경우 로그인 했다는 상태를 서버에 유지해야하는데 이 과정에서 소모되는 리소스는 만만치 않다.
* 로그인을 무상태로 처리하는 대표적인 케이스가 바로 JWT(Json Web Token)이다.

## 📚 비연결성(Stateless)

* HTTP는 기본이 연결을 유지하지 않는 모델
* 일반적으로 초 단위 이하의 빠른 속도로 응답
* 서버 자원을 효율적으로 사용할 수 있다.

## 📚 비연결성의 한계와 극복

* 매번 TCP/IP 연결을 새로 맺어야 한다. 이 과정에서 TCP 3-way handshake를 매번 거쳐야 하고 이 과정에서 시간이 추가된다.
* 웹 브라우저로 사이트를 요청하면 HTML 뿐만 아니라 자바스크립트, CSS, 추가 이미지 등 수많은 자원이 함께 다운로드된다.
* 지금은 HTTP 지속 연결을 통해 문제를 해결했다.
* HTTP/2, HTTP/3에서 더 많은 최적화가 이루어지고 있다.

![스크린샷 2025-01-06 오후 3 32 52](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b2a22ee2-a0c4-4eb5-ab57-f8bb2ce109a3.png)

![스크린샷 2025-01-06 오후 3 33 23](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/2d610fe7-cfca-4464-8730-e62b0bcb6308.png)

## 📚 HTTP 메시지

![스크린샷 2025-01-06 오후 3 38 38](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f6f37fc4-3da0-4473-b252-85c9fb5ab341.png)

* 요청 메시지 - HTTP 메서드
* 요청 메시지 - 요청 대상
* 요청 메시지 - HTTP 버전

![스크린샷 2025-01-06 오후 3 38 52](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/30057da2-8559-47b0-8b8a-a71b6a05d73d.png)

* 응답 메시지(HTTP 헤더) - HTTP 전송에 필요한 모든 부가정보
* 응답 메시지(HTTP 메시지 바디) - 실제 전송할 데이터

![스크린샷 2025-01-06 오후 3 39 07](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/37e589f1-7653-4055-873a-7eb8d79b12e5.png)
