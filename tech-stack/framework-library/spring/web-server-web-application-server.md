# Spring ‐ Web Server, Web Application server

#### 웹 서버, 웹 애플리케이션 서버

**1. Web Server**

* 주요 기능 : HTTP 프로토콜을 통한 통신
* 정적 파일(HTML, CSS, Image 등) 서빙
* Reverse Proxy : 클라이언트 요청을 뒤쪽의 WAS로 전달하고 결과를 받아 전달
* Load Balancing : 여러 대의 WAS로 요청을 분산

**2. Web Application Server**

* 주요 기능 : 프로그램 실행 환경 및 DB 접속 기능 제공
* 여러 개의 트랜잭션 관리 및 비즈니스 로직 수행
* 동적 HTML 생성 및 REST API 데이터 반환 

>❓Web Server와 Web Application Server를 분리하는 이유(실무적 관점)
>
> **1. 책임의 분리(Separation of Concerns)**
> * 웹 서버는 단순 정적 리소스 처리에 집중하여 WAS 과부하를 방지한다.
> * 비즈니스 로직은 WAS가 전담하여 효율을 높인다.
>
> **2. 서버 효율성 및 성능 최적화**
> * 정적 리소스(이미지, CSS 등)는 웹 서버가 빠르게 처리하고, WAS는 중요한 비즈니스 로직만 수행하도록 리소스를 배분한다.
>
> **3. 보안 강화**
> * WAS에는 DB 접속 정보 등 민감한 데이터의 로직이 들어있다.
> * 웹 서버를 앞에 두어 실제 애플리케이션 서버 IP를 숨기고 외부와의 직접적인 연결을 차단하는 역할을 수행한다.

#### 서블릿

* 스프링 부트 서버가 실행되면 내장 톰켓 서버(WAS)가 실행된다.
* 이 내장 톰켓 서버가 서블릿 컨테이너에 서블릿을 생성한다.
* 이 서블릿 컨테이너가 종료되면 서블릿 역시 함께 종료된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/7fa14010-c0f9-4b5d-bd0e-00ac1dddd460.png)

<br>

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/32cb1ca6-1695-464e-8ef0-5623e481d237.png)

> [📚Servlet Container](https://en.wikipedia.org/wiki/Web_container)
>
> * 서블릿 컨테이너는 서블릿 생명주기를 관리하고 특정 URL을 특정 서블릿에 매핑하며, URL 요청자가 적절한 접근 권한을 가지고 있는지 확인하는 역할을 담당한다.
> * 서블릿 컨테이너는 서블릿, JSP, 그리고 서버 측 코드를 포함하는 기타 파일들에 대한 요청을 처리한다. 서블릿 컨테이너는 서블릿 인스턴스를 생성하고, 서블릿을 로드 및 언로드하며, 요청 및 응답 객체를 생성/관리하고 그 외의 다양한 서블릿 관리 작업을 수행한다.
> ![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a1adddf3-dc8f-4fc3-9df2-5d77387d83c6.png)
> 
> * 쓰레드(OS & JVM) : OS 스케줄러와 JVM이 쓰레드 생성, CPU 할당, 컨텍스트 스위칭을 관리하고 자바에서 Thread 객체는 JVM 메모리 영역을 차지하며 실행된다.
> * 서블릿(Servlet Container & WAS) : 서블릿 컨테이너가 관리한다. 컨테이너는 서블릿의 `init()`, `service()`, `destroy()`라는 생명주기를 관리하며, 개발자는 비즈니스 로직에만 집중하게 해준다.
> * 결과적으로 쓰레드는 리소스(CPU, 메모리)를 소모하여 실제로 움직이는 것이라면 서블릿은 쓰레드 위에서 특정한 비즈니스 목적을 달성하는 것이라고 이해할 수 있다.

#### CSR, SSR

* SSR(서버 사이드 렌더링) : 서버 사이드 렌더링을 사용하면 서버에서 클라이언트로 정적 HTML을 전송할 수 있으며, 이후 클라이언트 측 자바스크립트가 HTML 요소에 이벤트 핸들러를 추가해 웹 페이지를 동적으로 만든다.
* CSR(클라이언트 사이드 렌더링) : 클라이언트 사이드 렌더링은 서버로부터 최소한의 구조만 갖춘 빈 HTML과 자바스크립트 파일을 전달받은 뒤, 브라우저(클라이언트)에서 자바스크립트를 실행하여 실시간으로 화면을 그려내는 방식이다. 데이터가 필요하면 서버에 API 요청을 보내 JSON 형태로 받아와 화면에 동적으로 끼워 넣는다.
