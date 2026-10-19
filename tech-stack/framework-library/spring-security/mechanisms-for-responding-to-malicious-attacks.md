# Spring Security - Mechanisms for responding to Malicious Attacks

### CORS(Cross Origin Resource Sharing)

* 웹에서는 보안을 위해 기본적으로 한 웹 페이지(출처A)에서 다른 웹 페이지(출처B)의 데이터를 직접 불러오는 것을 제한하는데 이를 동일 출처 정책이라고 한다.
* 만약 다른 출처 리소스를 안전하게 사용하고자 하려면 `CORS`가 필요하며 `CORS`는 특별한 HTTP 헤더를 통해 웹 페이지가 다른 출처의 리소스에 접근할 수 있도록 허가를 구하게 된다.
* 웹 애플리케이션이 다른 출처 데이터를 사용하고자 할 때, 브라우저가 그 요청을 대신해서 해당 데이터를 사용해도 되는지를 다른 출처에게 물어보는 것이라 할 수 있으며 출처를 비교하는 로직은 서버에 구현된 스펙이 아닌 브라우저에 구현된 스펙 기준으로 처리된다. 브라우저는 클라이언트 요청 헤더와 서버 응답 헤더를 비교해 최종 응답을 결정한다.
* 2개의 출처를 비교하는 방법은 URL 구성 요소 중 Protocol, Host, Port 이 3가지가 동일한지를 확인하면 되고 나머지는 달라도 무방하다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a85f9dcb-dac4-4543-a892-465f9b6a69d1.png)

#### CORS 종류

* `SimpleRequest` - 예비 요청 없이 자동으로 CORS가 작동하여 서버에 본 요청을 한 후, 서버가 응답 헤더에 `Access-Control-Allow-Origin`과 같은 값을 전송하면 브라우저가 서로 비교 후 CORS 정책 위반 여부를 검사하는 방식이다.
* 제약사항 3가지 정리
  * `GET`, `POST`, `HEAD` 중 한 가지 메서드를 사용해야 한다.
  * 헤더는 `Accept`, `Accept-Language`, `Content-Language`, `Content-Type`, `DPR`, `Downlink`, `Save-Data`, `Viewport-Width`만 가능하고 Custom Header는 허용하지 않는다.
  * `Content-Type`은 `application/x-www-form-urlencoded`, `multipart/form-data`, `text/plain`만 가능하다. 

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/eaaaae37-2ab8-4479-a1f4-5b879033d5b0.png)

* `Preflight Request` - 브라우저는 요청을 한 번에 보내지 않고 예비 요청과 본 요청으로 나누어 서버에 전달하는데 브라우저가 예비 요청을 보내는 것을 `Preflight`라고 하며 이 예비 요청 메서드에선 OPTIONS가 사용된다.
* 예비 요청의 본 역할은 본 요청을 보내기 전에 브라우저 스스로 안전한 요청인지를 확인하는 것으로 요청 사양이 `SimpleRequest`에 해당하지 않을 경우 브라우저가 `Preflight Request`를 실행한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/997bca21-a15a-471d-9464-f8798d9040fe.png)

* `Access-Control-Allow-Origin` : 헤더에 작성된 출처만 브라우저가 리소스를 접근할 수 있도록 허용한다.
* `Access-Control-Allow-Methods` : preflight request에 대한 응답으로 실제 요청 중 사용할 수 있는 메서드를 나타낸다.
* `Access-Control-Allow-Headers` : preflight request에 대한 응답으로 실제 요청 중 사용할 수 있는 헤더 이름을 나타낸다.
* `Access-Control-Allow-Credentials` : 실제 요청에 쿠키나 인증 등 사용자 자격 증명이 포함될 수 있음을 나타낸다.
* `Access-Control-Max-Age` : preflight request 요청 결과를 캐시할 수 있는 시간을 나타낸 것으로 해당 시간동안은 preflight 요청을 다시 하지 않게 된다.
* CORS 사전 요청(Preflight Request)에는 쿠키(JSESSIONID)가 포함되어 있지 않기 때문에 Spring Security 이전에 처리되어야 한다.
* 사전 요청에 쿠키가 없고 Spring Security가 먼저 처리되면 요청은 사용자가 인증되지 않았다고 판단하고 거부할 수 있다.
* `CORS`가 먼저 처리되도록 하기 위해서 `CorsFilter`를 사용할 수 있으며 `CorsFilter`에 `CorsConfigurationSource`를 제공함으로써 Spring Security와 통합할 수 있다.

### CSRF(Cross Site Request Forgery)

* 웹 애플리케이션의 보안 취약점으로 공격자가 사용자로 하여금 이미 인증된 다른 사이트에 대해 원치 않는 작업을 수행하게 만드는 기법을 말한다.
* 이 공격은 사용자 브라우저가 자동으로 보낼 수 있는 인증 정보, 예를 들어 쿠키나 기본 인증 세션을 이용해 사용자가 의도치 않은 요청을 서버로 전송하게 만든다.
* 이는 사용자가 로그인한 상태에서 악의적인 웹 사이트를 방문하거나 이메일 등을 통해 악의적인 링크를 클릭할 때 발생할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/176848d3-37e4-4755-9eac-c1c476c44c41.png)

* `CSRF` 토큰은 서버에 의해 생성되며 클라이언트 세션에 저장되고 폼을 통해 서버로 전송되는 모든 변경 요청에 포함되어야 하며 서버는 이 `CSRF` 토큰을 검증하여 요청 유효성을 확인한다.
* 기본 설정은 GET, HEAD, TRACE, OPTIONS와 같은 안전한 메서드를 무시하고 POST, PUT, DELETE 등과 같은 변경이 수반되는 HTTP 메서드에 대해서만 `CSRF` 토큰 검사를 수행한다.
* 중요한 점은 실제 `CSRF` 토큰이 브라우저에 의해 자동으로 포함되지 않는 요청 부분에 위치해야 한다는 것으로서 HTTP 매개변수나 헤더에 실제 CSRF 토큰을 요구하는 것이 `CSRF` 토큰 공격을 방지하는데 효과적이라 할 수 있다.
* 반면 쿠키에 토큰을 요구하는 것은 브라우저가 쿠키를 매 요청마다 자동으로 포함시키기 때문에 효과적이지 않다고 볼 수 있다.

### CSRF 토큰 유지 및 검증

* `CSRF` 토큰은 `CsrfTokenRepository`를 사용해 영속화하며 `HttpSessionCsrfTokenRepository`와 `CookieCsrfTokenRepository`를 지원한다.
* `HttpSessionCsrfTokenRepository`는 기본적으로 HTTP 요청 헤더인 `X-CSRF-TOKEN` 또는 요청 매개변수인 `_csrf`에서 토큰을 읽는다.

#### CsrfTokenRequestHandler

* `Csrf Token`은 `CsrfTokenRequestHandler`를 사용해 토큰을 생성 및 응답하고 HTTP 헤더 또는 요청 매개변수로부터 토큰 유효성을 검증하도록 한다.
* `XorCsrfTokenRequestAttributeHandler`와 `CsrfTokenRequestAttributeHandler`를 제공하며 사용자 정의 핸들러를 구현할 수 있다.
  * `_csrf`및 `CsrfToken.class.getName()` 명으로 `HttpServletRequest` 속성에 `CsrfToken`을 저장하고 꺼내서 참조할 수 있다.
  * 토큰 값을 요청 헤더 또는 요청 매개변수 중 하나를 사용해 토큰 유효성 비교 및 검증을 해결한다.
  * 클라이언트 매 요청마다 `CSRF` 토큰 값에 난수를 인코딩해 변경한 `CsrfToken`이 반환되도록 보장한다. 세션에 저장된 원본 토큰 값은 그대로 유지한다.
  * 헤더 값 또는 요청 매개변수로 전달된 인코딩된 토큰은 원본 토큰을 얻기 위해 디코딩되며, 그런 다음 세션 혹은 쿠키에 저장된 영구적인 `CsrfToken`과 비교한다.
* Spring Security는 `CsrfToken`을 필요로 할 때까지 지연 로딩 전략을 사용한다. 그러므로 매 요청마다 세션으로부터 `CsrfToken`을 로드할 필요가 없어져 성능 향상이 가능하다.
* `CsrfToken`은 POST, PUT, PATCH, DELETE 등과 같은 안전하지 않은 HTTP 메서드를 사용해 요청이 발생할 때와 `CsrfToken`을 응답에 렌더링하는 모든 요청에서 필요하기 때문에 그 외 요청에는 지연로딩하는 것이 권장된다.

#### CSRF 토큰 유지 방법 (1) - HttpSessionCsrfTokenRepository

* `CsrfToken`은 `CsrkTokenRepository`를 사용해 영속화하며 원하는 위치에 토큰을 저장하도록 할 수 있다.
* 토큰을 세션에 저장하는 방법이다.
* `HttpSessionCsrfTokenRepository`는 기본적으로 HTTP 요청 헤더인 `X-CSRF-TOKEN` 또는 요청 매개변수인 `_csrf`에서 토큰을 읽는다.

#### CSRF 토큰 유지 방법 (2) - CookieCsrfTokenRepository

* Javascript 기반 애플리케이션을 지원하기 위해 `CsrfToken`을 세션만이 아니라 쿠키에도 저장할 수 있다.
* `CookieCsrfTokenRepository` 는 기본적으로 `XSRF-TOKEN` 명을 가진 쿠키에 작성하고 HTTP 요청 헤더인 `X-XSRF-TOKEN` 또는 요청 매개변수인 `_csrf`에서 토큰을 읽는다.
* JavaScript 에서 쿠키를 읽을 수 있도록 `HttpOnly`를 명시적으로 `false`로 설정할 수 있다.
* 허나 JavaScript로 직접 쿠키를 읽을 필요가 없는 경우 보안을 개선하기 위해 HttpOnly 를 생략하는 것이 좋다.

#### [CSRF 내용 추가 정리](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)

* `CSRF` 공격을 방지하기 위한 토큰 패턴을 사용하려면 실제 `CSRF` 토큰을 HTTP 요청에 포함해야 한다.
* 그래서 브라우저에 의해 HTTP 요청에 자동으로 포함되지 않는 요청 부분 중 하나에 포함이 되어야 한다.
* [Thymeleaf와 같은 SSR 방식의 백엔드에서 CSRF 토큰을 처리하는 방법](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-form)
* [SPA 방식에서 CSRF 토큰을 처리하는 방법](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/43c84a8b-4ce3-46f1-aa9f-de56a9297fbc.png)

* `CookieCsrfTokenRepository`의 설정 중에서 `HttpOnly` 설정을 비활성화해 클라이언트가 서버에서 발행한 쿠키로부터 `CSRF` 토큰을 읽을 수 있도록 한다.
* `CsrfTokenRequestHandler`를 만들어 클라이언트가 요청 헤더나 요청 파라미터로 CSRF 토큰을 제출할 경우 이를 검증하도록 구현한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b6a300cd-61da-4628-a54e-e1d9ef496887.png)

### SameSite

* `SameSite`는 최신 방식의 `CSRF` 공격 방어 방법 중 하나로서 서버가 쿠키를 설정할 때 `SameSite` 속성을 지정해 크로스 사이트 간 쿠키 전송에 대한 제어를 핸들링할 수 있다.
* Spring Security는 세션 쿠키 생성을 직접 제어하지 않기 때문에 `SameSite`에 대한 속성 지원은 제공하지 않으나 Spring Session에 대한 SameSite 속성은 지원한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9c23bec4-00e4-440f-a928-c617b334925d.png)
