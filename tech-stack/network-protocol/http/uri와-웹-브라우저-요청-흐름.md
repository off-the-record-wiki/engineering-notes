# HTTP - URI & Browser Request Flow

## 📚 URI(Uniform Resource Identifier)

![스크린샷 2025-01-06 오후 1 52 27](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/70136170-0517-4934-8d93-c3baf7c06b44.png)

* Uniform : 리소스를 식별하는 통일된 방식
* Resource : 자원, URI로 식별할 수 있는 모든 것
* Identifier : 다른 항목과 구분하는데 필요한 정보

> URL(Uniform Resource Locator), URN(Uniform Resource Name)

* URL - Locator : 리소스가 있는 위치 지정
* URN - Name : 리소스에 이름을 부여

`https://www.google.com/search?q=hello`

* scheme : 프로토콜 지정(어떤 방식으로 자원에 접근할 것인지를 정하는 규칙)
  * HTTP는 80번 포트, HTTPS는 443번 포트(HTTPS = HTTP Secure) 
* host : 호스트명, 도메인명 또는 IP 주소를 사용
* port : 접속 포트
* path : 리소스 경로, 계층적 구조
* query : ?로 시작, &로 추가 가능, 쿼리 파라미터 혹은 쿼리 스트링

## 📚 웹 브라우저 요청 흐름 정리🛠️

> 면접 질문 : 브라우저 주소창에 네이버를 입력할 경우 일어나는 변화에 대해 서술해보세요.

①. 브라우저 검색창에 `https://www.google.com:443/search?q=hello&hl=ko`를 입력한다.<br>
②. host명에 해당하는 주소에 대해 DNS를 조회해 IP 주소를 질의한다.<br>
③. 브라우저는 얻은 IP 주소로 TCP 3-way handshake를 시작한다.<br>
④. HTTP 요청 메시지를 생성하여 구글 서버로 GET 요청을 보낸다.<br>
⑤. 구글 서버는 요청을 처리하고 리소스를 응답한다.<br>
⑥. 렌더링 과정이 이루어진다.<br>
 

 
