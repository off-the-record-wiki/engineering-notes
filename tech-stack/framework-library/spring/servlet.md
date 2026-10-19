# Spring ‐ Servlet

#### HttpServletRequest 개요

* HttpServletRequest는 서블릿 컨테이너(WAS)가 HTTP 요청 메시지를 파싱하여 개발자가 자바 객체 형태로 편리하게 다룰 수 있도록 제공하는 인터페이스이다.
* HTTP 프로토콜의 모든 정보를 추상화하여 담고 있으며, 요청 시작부터 응답이 나갈 때까지 유지되는 생명주기를 가진다.

> [📚HttpServletRequest](https://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html)
> 
> * HttpServletRequest는 서블릿 컨테이너(WAS)가 HTTP 요청 메시지를 파싱하여 개발자가 자바 객체 형태로 편리하게 다룰 수 있도록 제공하는 인터페이스이다.
> 
> **1. HTTP 요청 메시지 정보 조회**
> * HTTP 메시지의 모든 구성 요소를 메서드 호출만으로 조회할 수 있다.
> * Start Line : 메서드, URI, 프로토콜, 쿼리 스트링
> * Header : 모든 헤더 정보 조회
> * Body : HTML Form 데이터 및 쿼리 파라미터 조회
>
> **2. 임시 저장소 기능**
> * 해당 HTTP 요청이 시작되어 응답이 나갈 때까지 유효한 메모리 저장소를 제공한다.
>
> **3. 세션 관리 기능**
> * 사용자의 상태를 유지하기 위한 세션 생성 및 조회 기능을 제공한다.
>
> **4. 동작 원리**
> * 요청 수신 : 클라이언트가 HTTP 요청을 보내면 WAS가 이를 수신한다.
> * 객체 생성 : WAS는 요청 메시지를 기반으로 `HttpServletRequest`와 `HttpServletResponse` 객체를 새롭게 생성한다.
> * 서블릿 호출 : 생성된 객체를 인자로 전달하며 서블릿의 `service()` 메서드를 호출한다.
> * 로직 수행 : request 객체에서 데이터를 꺼내 비즈니스 로직을 수행한다.
> * 객체 소멸 : HTTP 응답이 완료되면 두 객체는 바로 소멸된다.

#### HttpServletResponse 개요

* HttpServletResponse는 서블릿 컨테이너(WAS)가 클라이언트에 보낼 HTTP 응답 메시지를 생성하기 위해 제공하는 인터페이스이다. 
* 개발자가 HTTP 프로토콜의 복잡한 규격을 일일이 맞출 필요 없이, 자바 메서드 호출만으로 응답 코드, 헤더, 바디를 설정할 수 있도록 돕는다.

> [📚HttpServletResponse](https://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletResponse.html)
> 
> * HttpServletResponse는 서블릿 컨테이너(WAS)가 클라이언트에 보낼 HTTP 응답 메시지를 생성하기 위해 제공하는 인터페이스이다.
> 
> **1. HTTP 응답 메시지 생성**
> * HTTP 응답의 3요소(Status Line, Header, Body)를 설정한다.
> * Status Line : HTTP 상태 코드 설정
> * Header : 응답 헤더 값 설정
> * Body : HTML 페이지, JSON 데이터 등 실제 콘텐츠 작성
>
> **2. 다양한 편의 기능 제공**
> * Content 설정
> * Cookie 설정
> * 리다이렉트 설정 - 302와 함께 Location 헤더를 설정
>
> **3. 데이터 전송 스트림**
> * 문자 데이터 : `getWriter()`를 통해 `PrinterWriter` 획득 후 HTML 등을 출력한다.
> * 바이너리 데이터 : `getOutputStream()`을 통해 `ServletOutputStream`을 획득한 후 파일 등을 전송한다.
>
> **4. 동작 원리**
> * 객체 생성 : WAS는 `HttpServletRequest`를 만들 때 응답을 위한 빈 `HttpServletResponse`객체도 함께 생성된다.
> * 전달 : Servlet의 `service()` 메서드 인자로 이 객체들을 전달한다.
> * 응답 작성 : 개발자가 비즈니스 로직을 마친 이후 response 객체에 상태 코드나 데이터를 채운다.
> * 메시지 변환 : 서블릿 메서드가 종료되면 WAS는 response 객체에 담긴 내용을 바탕으로 표준 HTTP 응답 메시지를 물리적으로 생성해 클라이언트에게 전송한다.
