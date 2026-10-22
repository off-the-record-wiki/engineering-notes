# Spring Security - Authentication Status Persistence Processing Mechanism

### SecurityContextRepository

* 스프링 시큐리티에서 사용자가 인증을 한 이후 요청에 대해 계속 사용자 인증 상태를 유지하기 위해 사용되는 클래스이다.
* 인증 상태 영속 메커니즘은 사용자가 인증을 하게 되면 해당 사용자 인증 정보와 권한이 `SecurityContext`에 저장되고 `HttpSession`을 통해 요청 간 영속성이 이루어지는 방식이다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/dddca966-bb52-4eda-aa98-552cb63dfe72.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e41020c6-d5b6-4af3-b54f-0e2f6583043f.png)

* `HttpSesssionSecurityContextRepository` : 요청 간 `HttpSession`에 보안 컨텍스트 저장, 후속 요청 시 컨텍스트 영속성 유지
* `RequestAttributeSecurityContextRepository` : `ServletRequest`에 보안 컨텍스트 저장, 후속 요청 시 컨텍스트 영속성 유지 불가
* `NullSecurityContextRepository` : 세션을 사용하지 않는 `Stateless` 인증(JWT, OAuth2)일 경우 사용하며 컨텍스트 관련한 아무런 처리를 하지 않는다.
* `DelegatingSecurityContextRepository` : `HttpSesssionSecurityContextRepository`와 `RequestAttributeSecurityContextRepository`를 동시에 사용할 수 있도록 위임된 클래스로 초기화 시 이 클래스가 기본으로 설정된다.

#### SecurityContextHolderFilter

* `SecurityContextRepository`를 사용하여 `SecurityContext`를 얻고 이를 `SecurityContextHolder`에 설정하는 필터 클래스이다.
* 이 필터 클래스는 `SecurityContextRepository`의 `saveContext()`를 강제로 실행시키지 않고 사용자가 명시적으로 호출되어야 `SecurityContext`를 저장할 수 있는데 이는 `SecurityContextPersistenceFilter`와 다르다.
* 인증이 지속되어야 하는가를 각 인증 메커니즘이 독립적으로 선택할 수 있게 해 더 나은 유연성을 제공하고 `HttpSession`에 필요할 때만 저장함으로써 성능을 향상시킨다.

* **SecurityContext 생성과 저장, 삭제**
  * 익명 사용자 : `SecurityContextRepository`를 사용해 새로운 `SecurityContext` 객체를 생성해 `SecurityContextHolder`에 저장 후 다음 필터로 전달하면 `AnonymousAuthenticationFilter`에서 `AnonymousAuthenticationToken` 객체를 `SecurityContext`에 저장한다.
  * 인증 요청 : `SecurityContextRepository`를 사용해 새로운 `SecurityContext` 객체를 생성해 `SecurityContextHolder`에 저장 후 다음 필터로 전달하면 `UsernamePasswordAuthenticationFilter`에서 인증 성공 후 `SecurityContext`에 `UsernamePasswordAuthentication` 객체를 `SecurityContext`에 저장한다. 그 다음에 `SecurityContextRepository`를 사용하여 `HttpSession`에 `SecurityContext`를 저장한다.
  * 인증 후 요청 : `SecurityContextRepository`를 사용해 `HttpSession`에서 `SecurityContext`를 꺼내어 `SecurityContextHolder`에서 저장 후 다음 필터로 전달한 후 `SecurityContext`안에 `Authentication`객체가 존재하면 계속 인증을 유지한다.
  * 클라이언트 응답시 공통 : `SecurityContextHolder.clearContext()`로 컨텍스트를 삭제한다.(쓰레드 풀의 쓰레드라면 해당 과정은 반드시 필요하다.)
>❗`SecurityContextHolder`는 내부적으로 쓰레드 로컬을 사용하기 때문에 만약 WAS 내부의 쓰레드 풀에서 쓰레드가 재사용될 경우 쓰레드 로컬은 쓰레드가 종료될 때까지 값이 유지되기 때문에 이전 요청의 인증 정보가 그대로 남아있을 수 있는 가능성이 있다.<br> 
>❗만약 명시적으로 비워주지 않게 된다면 권한 오염 문제가 발생한다.<br>

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8dc56b4e-5cdf-4a59-906c-cafd128816f1.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/43ec3be2-c72d-46a2-842a-3be9dfba3929.png)
