# Spring ‐ Spring Proxy

#### 프록시 팩토리

* 스프링은 동적 프록시를 통합해서 편리하게 만들어주는 프록시 팩토리라는 기술을 제공한다.
* 프록시 팩토리는 인터페이스가 있으면 JDK 동적 프록시를 사용하고 구체 클래스만 있다면 CGLIB를 사용한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/5e3d12f6-7ea8-4b3c-9d58-f25d3485f114.png)

* 프록시 팩토리는 내부적으로 어댑터들을 가지고 있다.
  * JDK 동적 프록시 : 프록시 팩토리가 내부적으로 `InvocationHandler`를 하나 만들고 이 핸들러가 `Advice`를 호출하도록 연결한다.
  * CGLIB : 프록시 팩토리가 내부적으로 `MethodInterceptior`를 하나 만들고 이 인터셉터가 `Advice`를 호출하도록 연결한다.
* 스프링은 `MethodInterceptor`가 아닌 다른 형태의 `Advice`들을 `MethodInterceptor`로 변환해주는 어댑터들을 가지고 있다.
* `AfterReturningAdviceAdapter`, `MethodBeforeAdviceAdapter`와 같은 어댑터들이 있기 때문에 개발자가 `BeforeAdvice`만 구현해도 프록시 팩토리가 이를 `MethodInterceptor` 규격으로 감사서 JDK Proxy나 CGLIB 양쪽 어디에서든지 사용할 수 있게 되는 것이다.

```java
public interface AdvisorChainFactory {

	/**
	 * Determine a list of {@link org.aopalliance.intercept.MethodInterceptor} objects
	 * for the given advisor chain configuration.
	 * @param config the AOP configuration in the form of an Advised object
	 * @param method the proxied method
	 * @param targetClass the target class (may be {@code null} to indicate a proxy without
	 * target object, in which case the method's declaring class is the next best option)
	 * @return a List of MethodInterceptors (may also include InterceptorAndDynamicMethodMatchers)
	 */
	List<Object> getInterceptorsAndDynamicInterceptionAdvice(Advised config, Method method, @Nullable Class<?> targetClass);

}
```
```java
public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
        Advised config, Method method, @Nullable Class<?> targetClass) {
    
    AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
    // ... 중략 ...
    for (Advisor advisor : config.getAdvisors()) {
        // 여기서 AdvisorAdapterRegistry를 통해 
        // 우리가 만든 Advice를 MethodInterceptor(공통 규격)로 변환(Adapt)합니다.
        MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
        interceptorsList.addAll(Arrays.asList(interceptors));
    }
    return interceptorsList;
}
```
```java
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

    @Override
    @Nullable
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
        
        // 체인이 비어있으면 실제 타겟 메서드 직접 호출
        if (chain.isEmpty()) {
            retVal = AopUtils.invokeJoinpointUsingReflection(target, method, args);
        } else {
            // 체인이 있으면 MethodInvocation을 생성하여 Advice 실행 (Advice 호출 지점)
            MethodInvocation invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
            retVal = invocation.proceed();
        }
        return retVal;
    }
}
```
```java
public class DynamicAdvisedInterceptor implements MethodInterceptor {

    private final AdvisedSupport advised; // 프록시 설정 정보 (Advice, Pointcut 등)

    public DynamicAdvisedInterceptor(AdvisedSupport advised) {
        this.advised = advised;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        Object target = advised.getTargetSource().getTarget();
        List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, target.getClass());

        Object retVal;
        if (chain.isEmpty()) {
            // 1. 적용할 Advice(인터셉터)가 없으면 바로 타겟 호출
            retVal = proxy.invoke(target, args);
        } else {
            // 2. Advice가 있으면 MethodInvocation을 생성하여 체인 실행
            // 여기서 우리가 만든 Advice(MethodInterceptor)들이 순차적으로 실행됨
            MethodInvocation invocation = new ReflectiveMethodInvocation(proxy, target, method, args, target.getClass(), chain);
            retVal = invocation.proceed();
        }
        
        return retVal;
    }
}
```

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/fbbc1ada-ddbe-4844-8f22-ca6bb110df26.png)

#### 포인트컷, 어드바이스, 어드바이저

* 포인트컷(PointCut) : 어디에 부가 기능을 적용할지, 어디에 부가 기능을 적용하지 않을지 판단하는 필터링 로직
* 어드바이스(Advice) : 프록시가 호출하는 부가 기능
* 어드바이저(Advisor) : 포인트컷1 + 어드바이스1 구조

> [📚AOP(Aspect-oriented programming)](https://en.wikipedia.org/wiki/Aspect-oriented_programming)
> 
> ![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/56f6cacf-f134-45ea-9508-64cfd4a0e59f.png)
>
> **1. AOP란?**
> * AOP는 애플리케이션의 핵심 비즈니스 로직과 프로그램 전체에 걸쳐 공통적으로 적용되는 부가 기능을 분리하여 모듈화하는 프로그래밍의 패러다임이다.
> * 기존의 OOP가 객체를 기준으로 모듈화한다면, AOP는 기능을 기준으로 모듈화하여 코드의 중복을 제거하고 응집도를 높이는 것을 목표로 한다.
>
> **2. AOP의 핵심 용어**
> * Aspect : 횡단 관심사를 모듈화한 것이다. Advice + Pointcut의 결합체이다.
> * Advice : 언제, 무엇을 할 것인지에 대한 로직이다.
> * Pointcut : Advice가 적용될 대상(메서드)을 선정하는 지점이다. 정규표현식 등을 사용하여 특정 패키지나 메서드 패턴을 지정한다.
> * Join Point : Advice가 적용될 수 있는 모든 지점을 말한다.
> * Target : 핵심 로직을 가지고 있어 Advice를 적용받는 원본 객체이다.
>
> **3. AOP의 구현 방식**
> * 컴파일 시점 위빙 : 자바 소스코드를 컴파일할 때 바이트 코드에 로직을 삽입한다.
> * 클래스 로딩 시점 위빙 : 클래스 로더가 바이트 코드를 메모리에 올릴 때 로직을 삽입한다.
> * 런타임 위빙 : 프로그램이 실행 중일 때 프록시 객체를 동적으로 생성해 부가 기능을 끼워 넣는다.
>
> ![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/0b31327e-640b-421e-b63f-725d9903fd2b.png)


#### Pointcut

```java
public interface Pointcut {

    /**
     * 클래스가 일치하는지 확인하는 필터를 반환합니다.
     * @return the ClassFilter (never {@code null})
     */
    ClassFilter getClassFilter();

    /**
     * 메서드가 일치하는지 확인하는 매처를 반환합니다.
     * @return the MethodMatcher (never {@code null})
     */
    MethodMatcher getMethodMatcher();

    /**
     * 항상 True를 반환하는 기본 포인트컷 인스턴스입니다.
     * 모든 클래스와 메서드에 매칭하고자 할 때 사용됩니다.
     */
    Pointcut TRUE = TruePointcut.INSTANCE;

}
```

* 포인트컷은 클래스 레벨 필터링, 메서드 레벨 필터링을 지원한다.
  * 클래스 레벨 필터링 : 클래스가 맞는지?
  * 메서드 레벨 필터링 : 메서드가 맞는지?

#### 프록시 팩토리

* 프록시 팩토리 덕분에 개발자는 매우 편리하게 프록시를 생성할 수 있게 되었다.
* 추가로 어드바이저, 어드바이스, 포인트컷이라는 개념 덕분에 어떤 부가 기능을 어디에 적용할지 명확하게 이해할 수 있게 되었다.
