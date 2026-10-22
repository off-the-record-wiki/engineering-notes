# Spring - Bean Scope

#### 빈 스코프

* 스프링의 빈 스코프는 빈이 존재할 수 있는 범위를 뜻한다. 스프링 빈이 기본적으로 싱글톤으로 생성되지만, 상황에 따라 다른 생존 범위를 가지는 경우가 많다.

#### 싱글톤(Singleton) - 기본값

* 범위 : 스프링 컨테이너 시작부터 종료까지 유지되는 가장 넓은 범위
* 특징 : 컨테이너에 요청할 때마다 항상 동일한 인스턴스를 반환한다.
* 관리 : 컨테이너가 생성, 의존관계 주입, 초기화, 소멸까지 모든 생명주기를 관리한다.

#### 프로토타입(Prototype)

* 범위 : 빈의 생성과 의존관계 주입까지만 관여하는 매우 짧은 범위
* 특징 : 컨테이너에 요청할 때마다 항상 새로운 인스턴스를 생성해서 반환한다.
* 관리 : 스프링 컨테이너는 빈을 생성하고 의존관계 주입, 초기화까지만 처리한 뒤 클라이언트에게 던지고 더 이상 관리하지 않는다. 따라서 `@PreDestroy` 같은 소멸 콜백이 호출되지 않으며, 종료 처리는 클라이언트의 책임이다.
>❗프로토타입 스코프의 핵심
>
> * 싱글톤 빈 A가 프로토타입 빈 B를 주입받으면, A는 싱글톤이므로 주입 시점에 딱 한 번만 B를 생성해서 보관한다. 이후 사용자가 A를 통해 B를 사용하려 하면, 의도와 달리 항상 과거에 생성된 B를 사용하게 된다.
> * `Provider` 사용 : `ObjectProvider`를 사용하여 필요할 때마다 컨테이너에서 프로토타입 빈을 새로 조회하도록 한다.

#### 웹 관련 스코프(Web Scopes)

* 웹 환경에서만 동작하며, 컨테이너가 해당 스코프의 종료 시점까지 관리한다.
* `request` : HTTP 요청 하나가 들어오고 나갈 때까지 유지되는 스코프이다. 각각의 HTTP 요청마다 별도의 빈 인스턴스가 생성된다.
* `session` : HTTP Session과 동일한 생명주기를 가진다.
* `application` : 서블릿 컨텍스트(ServletContext)와 동일한 생명주기를 가진다.
* `websocket` : 웹 소켓과 동일한 생명주기를 가진다.
>❗웹 관련 스코프의 핵심
>
> * request 스코프 빈은 실제 HTTP 요청이 들어와야 생성된다. 그런데 스프링 컨테이너가 뜰 때 다른 빈이 이 빈을 주입받는 시도를 하게 되면 아직 요청이 들어온 것이 없기 때문에 에러가 발생한다.
> * `proxyMode = ScopedProxyMode.TARGET_CLASS`를 설정한다. 이렇게 하면 CGLIB가 가짜 프록시 객체를 만들어 먼저 주입해두고 나중에 실제 메서드가 호출되는 시점에 이 프록시 객체가 실제 요청을 확인해 진짜 빈을 찾아 호출해준다.

> 기본 요청 순서 : HTTP 요청 → 필터 → 인터셉터 → 디스패처 서블릿 → 핸들러(컨트롤러) → HTTP 응답

> [📚Bean Scope - The Singleton Scope](https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html#beans-factory-scopes-singleton)
>
> **1. The Singleton Scope 개념**
> * 스프링 컨테이너에서 특정 빈 정의당 오직 하나의 인스턴스만 생성하여 관리하는 방식이다.
> * 공유 인스턴스 : 해당 빈을 요청할 때마다 컨테이너는 항상 동일한 객체를 반환한다.
> * 캐싱 메커니즘 : 컨테이너는 싱글톤 빈을 생성한 후 내부 캐시에 보관하며, 이후의 모든 요청은 이 캐시된 객체를 참조한다.
>
> ![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/dd6c38d5-8830-44a6-ac1e-0950af9a3737.png)
> 
> **2. 주요 특징 및 설정**
> * 기본값(Default) : 스프링에서 스코프를 별도로 지정하지 않으면 모두 싱글톤으로 동작한다.
> * XML은 다음과 같이 지정한다.
```xml
<bean id="accountService" class="com.something.DefaultAccountService"/>

<bean id="accountService" class="com.something.DefaultAccountService" scope="singleton"/>
```

#### 싱글톤 스코프 vs 프로토타입 스코프

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/dfb52813-e7e6-44a4-938b-a375312ad2b7.png)

* 스프링 컨테이너는 기본적으로 싱글톤 스코프가 적용된다.
* 여러 클라이언트 요청이 와도 같은 객체 인스턴스가 반환된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7c81d5d1-310e-4e93-8bea-d01902457104.png)

* 여러 클라이언트 요청이 오면 프로토타입 빈이 생성되고 의존관계 주입 후 반환하는데 이 때, 의존관계 주입시까지만 스프링 컨테이너에서 관리된다.
* 반환한 후에는 스프링 컨테이너에서 이 프로토타입 빈을 관리하지 않는다.
* 이전 시간에 빈 생명주기 콜백에서 배웠던 `@PreDestroy` 어노테이션은 스프링 컨테이너에서 관리가 되는 빈에 대해서만 적용이 되기 때문에 프로토타입 빈의 경우에는 이 어노테이션을 쓰더라도 적용이 되지 않는다.
