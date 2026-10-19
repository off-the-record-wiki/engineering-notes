# Spring ‐ Spring Transaction

#### 스프링 트랜잭션 사용 방식

* 트랜잭션 매니저를 사용하는 방법은 크게 2가지가 있다.
  * 선언적 트랜잭션 관리
    * `@Transactional` 어노테이션이 있으면 AOP에 의해 트랜잭션 처리 로직과 실제 서비스 실행 로직이 명확히 분리된다.
    * 프록시 덕분에 트랜잭션 처리 로직과 순수한 비즈니스 로직을 분리할 수 있다.
  * 프로그래밍 방식 트랜잭션 관리
    * 트랜잭션 매니저 또는 트랜잭션 템플릿 등을 사용해서 트랜잭션 관련 코드를 직접 작성하는 방식이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/be6b6f45-1c75-42d3-bed3-9be80a86ac30.png)

* `@Transactional` 어노테이션이 특정 클래스나 메서드에 하나라도 있으면 트랜잭션 AOP는 프록시를 만들어 스프링 컨테이너에 등록한다.
* 실제 객체 대신에 프록시인 CGLIB를 스프링 빈에 등록한다.

#### 트랜잭션 적용 위치

* 스프링에서 우선순위는 항상 더 구체적이고 자세한 것이 높은 우선순위를 가진다.
* 클래스에 적용하면 메서드는 자동 적용이 된다.
* 인터페이스에도 `@Transactional` 어노테이션이 적용할 수 있다. 하지만 우선순위를 잘 고려해보면 아래와 같다.
  * 클래스 메서드(우선순위가 가장 높다)
  * 클래스 타입
  * 인터페이스 메서드
  * 인터페이스 타입(우선순위가 가장 낮다)

#### 트랜잭션 AOP 주의 사항

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9de1590e-b813-4e31-ae48-155d861ecccf.png)

> [📚@Transactional](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
> 
> **1. 프록시 기반 AOP의 핵심 원리**
> * 스프링에서 `@Transactional`이 붙은 메서드나 클래스는 실행 시점에 프록시 객체가 생성되어 원본 객체 대신 스프링 빈으로 등록된다.
> * 프록시 역할 : 클라이언트 요청을 가로채 트랜잭션 매니저를 통해 `set autocommit false`를 호출하고 트랜잭션을 시작한다.
> * 비즈니스 로직 호출 : 트랜잭션 준비가 끝나면 실제 대상 객체의 메서드를 호출한다.
> * 로직이 성공하면 commit, 예외가 발생하면 rollback을 수행한 뒤 응답을 돌려준다.
>
> **2. 직접 호출 문제(Internal Call Issues)**
> * 외부 호출 : 클라이언트가 주입받은 빈을 호출하면 프록시를 거치기 때문에 트랜잭션이 정상 작동한다.
> * 내부 호출(Self Invocation) : 같은 클래스 내의 다른 메서드를 호출하면 프록시를 거치지 않고 실제 객체 메서드를 호출하기 때문에 AOP가 적용되지 않는다.
>
> **3. 트랜잭션 관리 방식 비교**

| 구분 | 선언적 트랜잭션 (`@Transactional`) | 프로그래밍 방식 트랜잭션 |
| :--- | :--- | :--- |
| **적용 방법** | 애노테이션 하나로 설정 끝 | `TransactionTemplate`이나 매니저 직접 호출 |
| **코드 가독성** | **매우 높음.** 비즈니스 로직과 트랜잭션 로직이 완전히 분리됨 | **낮음.** 비즈니스 로직 사이에 트랜잭션 코드가 섞임 |
| **유지보수성** | 설정 변경만으로 트랜잭션 속성(격리 수준 등) 제어 가능 | 소스 코드를 일일이 수정해야 함 |
| **구현 메커니즘** | **스프링 AOP (프록시 방식)** | 직접적인 메서드 호출 및 제어 |
| **장점** | 실무 표준이며, 개발자는 비즈니스 로직에만 집중 가능 | 트랜잭션의 시작과 끝을 코드 단위로 아주 세밀하게 제어 가능 |

```java
@Slf4j
@SpringBootTest
public class InternalCallV1Test {

    @Autowired 
    CallService callService;

    @Test
    void printProxy() {
        // 실제 객체가 아닌 프록시 객체가 주입되었는지 확인
        log.info("callService class={}", callService.getClass());
    }

    @Test
    void externalCall() {
        // 외부에서 호출: 프록시를 거치므로 트랜잭션 로직이 수행되어야 할 것 같지만...
        callService.external();
    }

    @Test
    void internalCall() {
        // 직접 호출: 프록시를 거치므로 트랜잭션이 정상 작동함
        callService.internal();
    }

    @TestConfiguration
    static class InternalCallV1Config {
        @Bean
        CallService callService() {
            return new CallService();
        }
    }

    @Slf4j
    static class CallService {

        // 1. 외부에서 호출하는 메서드 (트랜잭션 없음)
        public void external() {
            log.info("call external");
            printTxInfo();
            // 내부 호출 발생! (this.internal()과 동일)
            internal(); 
        }

        // 2. 트랜잭션이 적용된 내부 메서드
        @Transactional
        public void internal() {
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);
        }
    }
}
```

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ca4e3c3f-beeb-4209-9a6c-e3080adb8fe4.png)

<br>

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/cb2c463c-49fd-48b3-bf72-9b9077c81baf.png)

* 스프링의 트랜잭션 AOP 기능은 public 메서드에만 트랜잭션을 적용하도록 기본 설정이 되어 있다. 

#### 예외와 트랜잭션 커밋, 롤백

* 스프링은 체크 예외는 커밋을 하고 언체크 예외는 롤백을 한다.
* 비즈니스 상황에 따라 체크 예외의 경우에도 트랜잭션을 커밋하지 않고 롤백하고 싶을 수도 있다. 이 때는 `rollbackFor` 옵션을 사용한다.
* 런타임 예외는 항상 롤백된다. 
