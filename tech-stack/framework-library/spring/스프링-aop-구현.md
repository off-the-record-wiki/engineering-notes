# Spring ‐ Spring AOP Application

#### 스프링 AOP 구현

```java
@Slf4j
@Aspect
public class AspectV1 {

    // 포인트컷 표현식을 @Around 어노테이션에 직접 기재
    @Around("execution(* com.jwj.aop..*(..))")
    public Object doLog(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("[log] {}", joinPoint.getSignature()); // 실행되는 메서드 정보 출력
        
        return joinPoint.proceed(); // 실제 타겟(또는 다음 어드바이스) 호출
    }
}
```
```java
@Slf4j
@Aspect
public class AspectV2 {

    /**
     * @Pointcut 어노테이션을 사용해서 포인트컷 표현식을 별도로 분리합니다.
     * 메서드 이름인 'allOrder()'가 포인트컷의 시그니처가 됩니다.
     * 내부 로직은 비워두며, 접근 제어자를 통해 재사용 범위를 조절할 수 있습니다.
     */
    @Pointcut("execution(* com.jwj.aop.order..*(..))")
    private void allOrder() {}

    /**
     * 분리된 포인트컷 시그니처를 참조하여 어드바이스를 적용합니다.
     */
    @Around("allOrder()")
    public Object doLog(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("[log] {}", joinPoint.getSignature());
        return joinPoint.proceed();
    }
}
```
```java
@Slf4j
@Aspect
public class AspectV3 {

    /**
     * com.jwj.aop.order 패키지와 그 하위 패키지를 대상으로 하는 포인트컷
     */
    @Pointcut("execution(* com.jwj.aop.order..*(..))")
    private void allOrder() {}

    /**
     * 클래스 이름 패턴이 'Service'로 끝나는 대상을 포인트컷으로 지정
     */
    @Pointcut("execution(* *..*Service.*(..))")
    private void allService() {}

    /**
     * 주문 관련 모든 로직에 로그를 남기는 어드바이스
     */
    @Around("allOrder()")
    public Object doLog(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("[log] {}", joinPoint.getSignature());
        return joinPoint.proceed();
    }

    /**
     * 주문 패키지 내부에 있으면서, 동시에 클래스 이름이 Service로 끝나는 대상에만 
     * 트랜잭션을 적용하는 어드바이스 (포인트컷 조합: &&)
     */
    @Around("allOrder() && allService()")
    public Object doTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            log.info("[트랜잭션 시작] {}", joinPoint.getSignature());
            Object result = joinPoint.proceed();
            log.info("[트랜잭션 커밋] {}", joinPoint.getSignature());
            return result;
        } catch (Exception e) {
            log.info("[트랜잭션 롤백] {}", joinPoint.getSignature());
            throw e;
        } finally {
            log.info("[리소스 릴리즈] {}", joinPoint.getSignature());
        }
    }
}
```
```java
/**
 * 포인트컷을 공용으로 사용하기 위해 별도의 외부 클래스에 모아둡니다.
 * 다른 Aspect에서 참조할 수 있도록 메서드 접근 제어자를 public으로 설정합니다.
 */
public class Pointcuts {

    // 주문 패키지 내의 모든 로직
    @Pointcut("execution(* com.jwj.aop.order..*(..))")
    public void allOrder(){}

    // 모든 서비스 계층 로직
    @Pointcut("execution(* *..*Service.*(..))")
    public void allService(){}
    
    // 주문 패키지이면서 서비스 계층인 로직 (조합 포인트컷)
    @Pointcut("allOrder() && allService()")
    public void orderAndService() {}
}
```
```java
@Slf4j
@Aspect
public class AspectV4 {

    /**
     * 외부 클래스(Pointcuts)에 정의된 공용 포인트컷을 참조합니다.
     * 이때 반드시 패키지명을 포함한 전체 경로(Full Qualifier)를 적어주어야 합니다.
     */
    @Around("com.jwj.aop.order.Pointcuts.allOrder()")
    public Object doLog(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("[log] {}", joinPoint.getSignature());
        return joinPoint.proceed();
    }

    /**
     * 주문 패키지 내의 서비스 계층에만 적용되는 조합 포인트컷을 참조합니다.
     */
    @Around("com.jwj.aop.order.Pointcuts.orderAndService()")
    public Object doTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            log.info("[트랜잭션 시작] {}", joinPoint.getSignature());
            Object result = joinPoint.proceed();
            log.info("[트랜잭션 커밋] {}", joinPoint.getSignature());
            return result;
        } catch (Exception e) {
            log.info("[트랜잭션 롤백] {}", joinPoint.getSignature());
            throw e;
        } finally {
            log.info("[리소스 릴리즈] {}", joinPoint.getSignature());
        }
    }
}
```
```java
@Slf4j
public class AspectV5 {

    /**
     * 실행 순서를 제어하기 위해 내부 static 클래스로 Aspect를 분리합니다.
     * @Order의 숫자가 낮을수록 먼저 실행됩니다. (Tx가 Log보다 먼저 시작됨)
     */
    @Aspect
    @Order(1) // 가장 먼저 실행
    public static class TxAspect {

        @Around("com.jwj.aop.order.Pointcuts.orderAndService()")
        public Object doTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
            try {
                log.info("[트랜잭션 시작] {}", joinPoint.getSignature());
                Object result = joinPoint.proceed();
                log.info("[트랜잭션 커밋] {}", joinPoint.getSignature());
                return result;
            } catch (Exception e) {
                log.info("[트랜잭션 롤백] {}", joinPoint.getSignature());
                throw e;
            } finally {
                log.info("[리소스 릴리즈] {}", joinPoint.getSignature());
            }
        }
    }

    @Aspect
    @Order(2) // TxAspect 다음에 실행
    public static class LogAspect {

        @Around("com.jwj.aop.order.Pointcuts.allOrder()")
        public Object doLog(ProceedingJoinPoint joinPoint) throws Throwable {
            log.info("[log] {}", joinPoint.getSignature());
            return joinPoint.proceed();
        }
    }
}
```

#### 어드바이스 종류(시점 중요)

* `@Around` : 메서드 호출 전후 수행
* `@Before` : 조인 포인트 실행 이전에 수행
* `@AfterReturning` : 조인 포인트가 정상 완료된 후 실행
* `@AfterThrowing` : 메서드가 예외를 던지는 경우 실행
* `@After` : 조인 포인트가 정상 또는 예외와 관계없이 실행

> [📚Advice](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/advice.html)
>
> **1. @Around**
> * 메서드 호출 전후를 모두 제어하는 가장 강력한 어드바이스이다
> * JoinPoint 제어 : `ProceedingJoinPoint.proceed()`를 호출하여 실제 비즈니스 로직의 실행 여부를 결정할 수 있다.
> * `proceed()`를 호출하지 않으면 타겟 메서드가 실행되지 않으므로 반드시 호출 흐름을 관리해야 한다.
>
> **2. @Before**
> * 조인 포인트 실행 이전에 부가 기능을 수행한다.
> * 로직 실행을 막을 권한은 없지만, 예외를 던져서 흐름을 중단시킬 수는 있다.
> * 주로 입력 파라미터 검증, 보안 체크, 단순 로그 기록에 적합하다.
>
> **3. @AfterReturning**
> * 메서드가 예외 없이 정상적으로 반환된 경우에만 실행된다.
> * `returning` 속성을 통해 메서드의 리턴값에 접근할 수 있다.
> * 단, 리턴값 자체를 조작하는 것은 불가능하며, 리턴값의 내부 상태를 변경하거나 로그를 남기는 용도로 사용한다.
>
> **4. @AfterThrowing**
> * 메서드 실행 중 예외가 발생하여 던져지는 경우 실행된다.
> * `throwing` 속성을 통해 발생한 예외 객체에 접근할 수 있다.
>
> **5. @After**
> * 메서드의 성공/실패 여부와 상관없이 항상 실행된다.
> * 로직의 결과와 관계없이 반드시 수행되어야 하는 리소스 해제(커넥션 반납 등)에 사용된다.

```java
@Slf4j
@Aspect
public class AspectV6 {

    /**
     * @Around: 가장 강력한 어드바이스. 조인 포인트의 실행 여부 선택, 
     * 전달 값 변조, 예외 처리 등 모든 생명주기를 직접 제어합니다.
     */
    @Around("com.jwj.aop.order.Pointcuts.orderAndService()")
    public Object doTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            log.info("[around][트랜잭션 시작] {}", joinPoint.getSignature());
            Object result = joinPoint.proceed();
            log.info("[around][트랜잭션 커밋] {}", joinPoint.getSignature());
            return result;
        } catch (Exception e) {
            log.info("[around][트랜잭션 롤백] {}", joinPoint.getSignature());
            throw e;
        } finally {
            log.info("[around][리소스 릴리즈] {}", joinPoint.getSignature());
        }
    }

    /**
     * @Before: 조인 포인트 실행 직전에 실행됩니다.
     * 작업이 끝나면 자동으로 타겟을 호출하므로 proceed()를 호출할 필요가 없습니다.
     */
    @Before("com.jwj.aop.order.Pointcuts.orderAndService()")
    public void doBefore(JoinPoint joinPoint) {
        log.info("[before] {}", joinPoint.getSignature());
    }

    /**
     * @AfterReturning: 메서드가 성공적으로 결과를 반환했을 때 실행됩니다.
     * 'returning' 속성에 명시된 이름으로 결과값에 접근할 수 있습니다.
     */
    @AfterReturning(value = "com.jwj.aop.order.Pointcuts.orderAndService()", returning = "result")
    public void doReturn(JoinPoint joinPoint, Object result) {
        log.info("[return] {} return={}", joinPoint.getSignature(), result);
    }

    /**
     * @AfterThrowing: 메서드 실행 중 예외가 발생했을 때 실행됩니다.
     * 'throwing' 속성에 명시된 이름으로 발생한 예외 객체에 접근할 수 있습니다.
     */
    @AfterThrowing(value = "com.jwj.aop.order.Pointcuts.orderAndService()", throwing = "ex")
    public void doThrowing(JoinPoint joinPoint, Exception ex) {
        log.info("[ex] {} message={}", joinPoint.getSignature(), ex.getMessage());
    }

    /**
     * @After: 메서드 실행이 끝나면(정상/예외 무관) 항상 실행됩니다. (finally와 유사)
     */
    @After("com.jwj.aop.order.Pointcuts.orderAndService()")
    public void doAfter(JoinPoint joinPoint) {
        log.info("[after] {}", joinPoint.getSignature());
    }
}
```

> `@Around` 어노테이션 하나만 있어도 되지만 범위를 줄여가며 제약을 두는 명확한 코드가 더 좋기에 각 상황에 맞는 어노테이션을 활용하는 것을 추천한다.
