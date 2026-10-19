# Spring - Cookie & Session

#### 쿠키(Cookie)

> [📚Cookie](https://en.wikipedia.org/wiki/HTTP_cookie)
>
> **1. HTTP 쿠키의 정의 및 목적**
> * 웹 서버가 사용자의 브라우저에 전송하는 작은 데이터 조각으로 브라우저는 이를 저장해 두었다가 동일한 서버에 재요청시 함께 전송한다.
> * HTTP의 Stateless(무상태) 특성을 보완하여 서버가 클라이언트를 식별하고 상태를 유지할 수 있게 한다.
> * 세션 관리
> * 개인화
> * 트래킹
>
> **2. 쿠키의 주요 종류**
> * 세션 쿠키 : 만료 날짜가 지정되지 않은 쿠키로, 브라우저 메모리에 상주하다가 브라우저 종료 시 삭제된다.
> * 영속 쿠키 : 특정 만료 날짜가 지정된 쿠키로, 브라우저를 닫아도 디스크에 저장되어 지정된 시간까지 유지된다.
> * 써드파티 쿠키 : 사용자가 방문한 도메인이 아닌, 다른 도메인에서 생성한 쿠키로 주로 개인 추적 및 마케팅에 활용된다.
>
> **3. 보안 및 속성(핵심 설정)**
> * `HttpOnly` : 자바스크립트를 통한 접근을 차단해 XSS(Cross-Site Scripting) 공격으로부터 쿠키 탈취를 방지합니다.
> * `Secure` : HTTPS 프로토콜을 통해서만 쿠키를 전송하도록 제한해 네트워크 스니핑을 방지한다.
> * `SameSite` : 사이트 간 요청 시 쿠키 전송 여부를 결정해 CSRF(Cross-Site Request Forgery) 공격을 방어한다.
> 
> **4. 쿠키의 한계와 대안**
> * 용량 제한 : 브라우저당, 도메인당 개수와 크기에 제한이 있다.
> * 보안 취약성 : 클라이언트에 저장되므로 탈취 및 변조 위험이 항상 존재한다.
> * 대안 기술 : 더 큰 저장 공간과 보안을 위해 Web Storage, Indexed DB를 사용하기도 한다.

#### 세션(Session)

```java
@PostMapping("/login")
public String loginV4(@Valid @ModelAttribute LoginForm form, BindingResult bindingResult,
                      @RequestParam(defaultValue = "/") String redirectURL,
                      HttpServletRequest request) {

    // 검증 오류 발생 시 로그인 폼으로 리턴
    if (bindingResult.hasErrors()) {
        return "login/loginForm";
    }

    // 서비스 계층을 통한 로그인 시도
    Member loginMember = loginService.login(form.getLoginId(), form.getPassword());

    // 로그인 실패 처리 (글로벌 오류 생성)
    if (loginMember == null) {
        bindingResult.reject("loginFail", "아이디 또는 비밀번호가 맞지 않습니다.");
        return "login/loginForm";
    }

    // 로그인 성공 처리
    // 세션이 있으면 있는 세션 반환, 없으면 신규 세션을 생성
    HttpSession session = request.getSession();
    
    // 세션에 로그인 회원 정보 보관
    session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember);

    // 로그인이 필요한 페이지에서 튕겨서 온 경우, 원래 가려던 페이지로 리다이렉트
    return "redirect:" + redirectURL;
}
```

> [📚Session](https://en.wikipedia.org/wiki/Session_(computer_science))
>
> * 세션은 둘 이상의 통신 장치 간의 상호작용적인 정보 교환을 가능하게 하는 시간 제한적인 양방향 연결을 의미한다.
>
> **1. 상태 유지(Stateful)의 본질**
> * HTTP는 기본적으로 독립적인 요청과 응답으로 이루어진 무상태 프로토콜이다. 세션은 적어도 서버가 현재 상태 정보를 유지하고 세션 이력을 저장함으로써 이 한계를 극복하는 상태 유지 통신을 가능하게 한다.
> * `session.setAttribute()`를 호출하는 순간, 서버는 HTTP 요청에 상태 정보를 담는다. 
>
> **2. 세션의 생명 주기(Establishment & Teardown)**
> * 성립(Establishment) : `request.getSession()` 호출 시 서버는 기존 세션이 없다면 새로운 `HttpSession` 객체를 생성하고, 추측 불가능한 고유값인 JSESSIONID를 발급한다.
> * 유지(Maintenance) : 서버는 응답 헤더에 `Set-Cookie: JESSSIONID=...`를 포함하며 이후 클라이언트는 모든 요청의 Cookie 헤더에 이 ID를 자동으로 포함시킨다.
> * 소멸(TearDown) : `session.invalidate()` 메서드 호출 시 즉시 소명, 마지막 요청으로부터 일정 시간동안 활동이 없으면 WAS가 가비지 컬렉션 대상으로 간주하여 제거한다.
>
> **3. 서버 측 세션 관리와 확장성**
> * 세션 불일치 문제 : 서버를 여러 대 공유하는 경우 세션 정보가 유실될 수 있다.
> * Sticky Session : L4/L7 스위치를 이용해 특정 클라이언트를 특정 서버에 고정할 수 있다.
> * Session Clustering : 서버 간 세션 데이터를 실시간 복제
> * External Session Store : Redis나 DB같은 별도 저장소에 세션을 저장해 모든 서버가 공유

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/aeb974ae-b091-410b-a6a4-e02309ef4726.png)

* 세션ID를 응답 쿠키로 만들어 반환하면 클라이언트 측 쿠키 저장소에서 이를 보관한다.
* 서버 측에서는 만든 세션ID에 대한 것을 세션 저장소에 저장한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/faab6405-eb1e-4ac7-a1a3-ebe481d089a0.png)

* 클라이언트 측의 쿠키 저장소에서 보관하는 쿠키를 서버 측 요청을 보낼 때 함께 포함해서 보내게 된다.
* 그렇게 되면 서버 측에서 해당 쿠키 정보를 열람해 세션 ID를 서버 측 세션 저장소와 비교해 조회한다.

```java
@PostMapping("/logout")
public String logoutV3(HttpServletRequest request) {
    // 세션이 있으면 기존 세션을 반환하고, 없으면 새로운 세션을 생성하지 않고 null을 반환
    HttpSession session = request.getSession(false);

    if (session != null) {
        // 세션을 제거하여 서버 측의 세션 저장소 및 데이터를 삭제
        session.invalidate();
    }

    return "redirect:/";
}
```

#### 서블릿 HTTP 세션

* 서블릿은 세션을 위해 `HttpSession`이라는 기능을 제공한다.

```java
@GetMapping("/session-info")
public String sessionInfo(HttpServletRequest request) {
    // 세션이 없으면 새로 생성하지 않도록 false 설정
    HttpSession session = request.getSession(false);
    
    if (session == null) {
        return "세션이 없습니다.";
    }

    // 세션 데이터(Key-Value) 전체 출력
    session.getAttributeNames().asIterator()
            .forEachRemaining(name -> 
                log.info("session name={}, value={}", name, session.getAttribute(name))
            );

    // 세션 메타데이터 출력
    log.info("sessionId={}", session.getId()); // 세션의 고유 식별자
    log.info("getMaxInactiveInterval={}", session.getMaxInactiveInterval()); // 세션 유효 시간 (초)
    log.info("creationTime={}", new Date(session.getCreationTime())); // 세션 생성 일시
    log.info("lastAccessedTime={}", new Date(session.getLastAccessedTime())); // 마지막 접속 일시
    log.info("isNew={}", session.isNew()); // 방금 생성된 세션인지 여부

    return "세션 정보 출력 완료";
}
```

* 세션은 사용자가 로그아웃을 호출해야 세션이 삭제되면서 관리가 된다. 
* 허나 대부분의 사용자는 로그아웃을 하지 않고 브라우저를 종료하기 때문에 세션이 그대로 남아있게 된다.
* 세션도 결국 자원이기에 타임아웃을 설정해 세션 자원을 관리할 필요가 있다.

#### 다중 서버에서의 세션 관리

* 하나의 서버를 운영할 때 문제가 없으나 만약 다중 서버로 운영할 경우 세션 저장소는 하나의 서버에 하나의 세션 저장소로 대응되기 때문에 문제가 생긴다.
* 여러 서버를 띄워 사용하는 경우 세션 저장소를 별도로 구성해 여러 서버가 단 하나의 세션 저장소만을 가리키도록 해야 한다.

[세션 공유 참고문서](https://hyuntaeknote.tistory.com/6?category=867120)
