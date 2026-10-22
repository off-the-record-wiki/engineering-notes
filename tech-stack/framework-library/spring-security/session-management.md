# Spring Security - Session Management

### 동시 세션 제어

* 동시 세션 제어는 사용자가 동시에 여러 세션을 생성하는 것을 관리하는 전략이다.
* 해당 전략은 사용자 인증 후 활성화된 세션 수가 설정된 `maximumSessions`값과 비교해 제어 여부를 결정한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e5f6a42a-6f7c-40e3-a072-a0188113a138.png)

### 세션 고정 보호

* 세션 고정 공격은 악의적인 공격자가 사이트에 접근해 세션을 생성한 다음 다른 사용자가 같은 세션으로 로그인하도록 유도하는 위험을 말한다.
* 스프링 시큐리티는 사용자가 로그인할 때 새로운 세션을 생성하거나 세션 ID를 변경함으로써 이러한 공격에 자동으로 대응한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4072145d-8b38-4b82-9d26-cc64143b803b.png)

* **세션 고정 보호 전략**
  * `changeSessionId()` : 기존 세션을 유지하면서 세션 ID만 변경해 인증 과정에서 세션 고정 공격을 방지하도록 한다. 이는 기본 값으로 설정되어있다.
  * `newSession()` : 새로운 세션을 생성하고 기존 세션 데이터를 복사하지 않도록 하는 방식이다.
  * `migrateSession()` : 새로운 세션을 생성하고 모든 기존 세션 속성을 새로운 세션에 복사한다.
  * `none()` : 기존 세션을 그대로 사용한다.

### 세션 생성 정책

* 스프링 시큐리티에서는 인증된 사용자에 대한 세션 생성 정책을 설정해 어떻게 세션을 관리할 것인지를 결정하며 이 정책은 `SessionCreationPolicy`로 설정할 수 있으며 여러 전략들이 존재한다.
  * `SessionCreationPolicy.ALWAYS` : 인증 여부와 관계없이 항상 새로운 세션을 생성한다.
  * `SessionCreationPolicy.NEVER` : 스프링 시큐리티에서는 세션을 생성하지 않으나 애플리케이션이 이미 생성한 세션은 사용할 수 있다.
  * `SessionCreationPolicy.IF_REQUIRED` : 필요한 경우에만 세션을 생성한다.
  * `SessionCreationPolicy.STATELESS` : 세션을 전혀 사용하지 않는다. 인증 필터는 인증 완료 후 `SecurityContext`를 세션에 저장하지 않으며 JWT와 같이 세션을 사용하지 않는 Stateless 방식으로 인증 관리를 할 때 불필요한 세션을 생성하지 않도록 할 수 있다. 허나 `SecurityContextHolderFilter`는 세션 단위가 아닌 요청 단위로 항상 새로운 `SecurityContext`를 생성하기 때문에 컨텍스트 영속성이 유지되지 않는다.

>❗STATELESS(상태를 유지하지 않는 설계)라고 하더라도 세션은 생성될 수 있다.
* 스프링 시큐리티에서는 `CSRF` 기능이 활성화되어있다. `CSRF` 기능이 활성화되면 사용자 세션을 생성해서 `CSRF` 토큰을 저장하게 된다.
* 이 때, 세션은 생성되나 `CSRF` 기능을 위해서 사용될 뿐 인증 프로세스의 `SecurityContext` 영속성에는 영향을 미치지 않는다.
* 스프링 시큐리티에서 기본적으로 `CSRF` 공격에 대한 대처 기능이 활성화되어있어서 `CSRF` 토큰이 생성될 것이고 그 토큰을 저장하기 위한 세션이 생길 수 있다.

### SessionManagementFilter / ConcurrentSessionFilter

#### [SessionManagementFilter](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html#the-sessionmanagementfilter)

* 요청이 시작된 이후 사용자가 인증되었는가를 감지하고 인증된 경우에는 세션 고정 보호 메커니즘을 활성화하거나 동시 다중 로그인을 확인하는 등 세션 관련 활동을 수행하기 위해 설정된 세션 인증 전략을 호출하는 필터 클래스이다.
* 스프링 시큐리티 6이상에서는 `SessionManagementFilter`가 기본적으로 설정되지않으며 세션관리 API 설정을 별도로 구성해야한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/398253fa-88d9-42d7-9ab9-ccf71822a199.png)

#### [ConcurrentSessionFilter](https://docs.spring.io/spring-security/reference/servlet/appendix/namespace/http.html#nsa-concurrency-control)

* 각 요청에 대해 `SessionRegistry`에서 `SessionInformation`을 검색하고 세션이 만료로 표시되었는가를 확인하고 만료로 표시된 경우라면 로그아웃 처리를 수행한다.(세션 무효화 처리)
* 각 요청에 대해 `SessionRegistry.refreshLastRequest(String)`를 호출해 등록된 세션이 가장 최근에 "마지막 업데이트"된 날짜/시간을 가지도록 한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4c1b7a74-9fbf-4976-9eb4-a1fe5eb94f19.png)
