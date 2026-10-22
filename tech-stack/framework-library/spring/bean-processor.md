# Spring ‐ Bean Processor

#### 빈 후처리기

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/98baf1e2-2db1-4591-b04b-b73b848c391d.png)

* `@Bean` 어노테이션을 명시해주거나 컴포넌트 스캔으로 스프링 빈을 등록하게 되면 스프링은 대상 객체를 생성하고 스프링 컨테이너의 빈으로 등록한다. 
* 이 때, `@Bean`의 경우 빈 이름은 메서드 이름이 되고, 컴포넌트 스캔의 경우 클래스 이름의 첫 글자를 소문자로 바꾼 이름이 된다. 빈 객체는 생성된 객체이다.
* 스프링 컨테이너의 빈으로 등록하기 직전에 조작을 하고 싶다면 빈 후처리기를 사용하면 된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/99400449-9210-4222-aa19-fdfaa64633da.png)

* 생성 : 스프링 빈 대상이 되는 객체를 생성한다.
* 전달 : 생성된 객체를 스프링 컨테이너의 빈으로 등록하기 전에 빈 후처리기에 전달한다.
* 후처리 작업 : 빈 후처리기는 전달된 스프링 빈 객체를 조작하거나 다른 객체로 바꿔치기 할 수 있다.
* 등록 : 후처리 작업 후 빈 후처리기는 빈을 반환한다. 전달된 빈을 그대로 반환하면 해당 빈이 등록되고 바꿔치기 하면 다른 객체가 빈으로 등록된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/09f958a6-65cb-461c-a3a5-66966384f7a3.png)

```java
@Slf4j
static class AToBPostProcessor implements BeanPostProcessor {
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    log.info("beanName={} bean={}", beanName, bean);
    
    // A 객체일 경우 B 객체로 바꿔치기하여 컨테이너에 등록
    if (bean instanceof A) {
        return new B();
    }
    
    return bean;
}
```

* 이렇게 빈 후처리기를 거치게 되면 기존의 A 타입의 스프링 빈이 B 타입으로 바꿔치기 된다.
* 결과적으로 스프링 컨테이너에는 A 타입의 스프링 빈이 존재하지 않고 B 타입의 스프링 빈이 존재하게 된다.
* 빈 후처리기는 빈을 조작하고 변경할 수 있는 후킹 포인트이다.

#### 스프링이 제공하는 빈 후처리기

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c754b2c3-7257-4c34-b382-0d64f68ecf85.png)

1. 스프링이 스프링 빈 대상이 되는 객체를 생성한다.
2. 생성된 객체를 스프링 컨테이너에 등록하기 전에 빈 후처리기에 전달한다.
3. 빈 후처리기는 스프링 컨테이너에서 모든 Advisor를 조회한다.
4. Advisor에는 Pointcut과 Advice가 포함되어 있다. 포인트컷을 사용해서 해당 객체가 프록시를 적용할 대상인지 아닌지 판단한다. 조건이 하나라도 만족하면 프록시 적용 대상이 된다.
5. 프록시 적용 대상이면 프록시를 생성하고 반환해서 프록시를 스프링 빈으로 등록한다. 만약 프록시 적용 대상이 아니면 실제 객체를 반환해서 실제 객체를 스프링 빈으로 등록한다.
6. 반환된 객체는 스프링 빈으로 등록된다.

```java
public interface BeanPostProcessor {

    /**
     * 빈 초기화 콜백(InitializingBean의 afterPropertiesSet 또는 커스텀 init-method)이 
     * 호출되기 '전'에 이 BeanPostProcessor를 적용합니다.
     * 빈은 이미 속성 값들로 채워진 상태입니다.
     * 반환되는 빈 인스턴스는 원본을 감싼 래퍼(wrapper)일 수 있습니다.
     */
    default @Nullable Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * 빈 초기화 콜백(InitializingBean의 afterPropertiesSet 또는 커스텀 init-method)이 
     * 호출된 '후'에 이 BeanPostProcessor를 적용합니다.
     * 빈은 이미 속성 값들로 채워진 상태입니다.
     * 반환되는 빈 인스턴스는 원본을 감싼 래퍼(wrapper)일 수 있습니다.
     */
    default @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}
```
```java
@FunctionalInterface
public interface BeanFactoryPostProcessor {

    /**
     * 표준 초기화가 완료된 후, 애플리케이션 컨텍스트의 내부 빈 팩토리를 수정합니다.
     * 모든 빈 정의(Bean Definition)는 이미 로드되었지만, 아직 어떤 빈도 인스턴스화되지 않은 상태입니다.
     * 이 단계에서는 이른 초기화(eager-initializing) 빈을 포함하여 모든 빈의 속성을 재정의하거나 추가할 수 있습니다.
     * * @param beanFactory 애플리케이션 컨텍스트에서 사용하는 빈 팩토리
     * @throws org.springframework.beans.BeansException 에러 발생 시
     */
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
```

#### 포인트 컷

**1. 프록시 생성 단계(컴파일/빈 등록 시점)**

* 목적 : 불필요한 프록시 객체 생성 방지 및 메모리 최적화
* 상세 : 빈 후처리기는 모든 빈을 전수 조사한다. 이 때, 포인트컷을 사용해 이 빈의 클래스나 메서드 중 단 하나라도 어드바이스를 적용할 대상이 있는지 찾게 된다.
```java
@FunctionalInterface
public interface ClassFilter {

    /**
     * 포인트컷을 주어진 인터페이스나 타겟 클래스에 적용할지 여부를 결정합니다.
     * @param clazz 대상 후보 클래스
     * @return 해당 타겟 클래스에 어드바이스를 적용해야 하는지 여부
     */
    boolean matches(Class<?> clazz);

    /**
     * 모든 클래스에 매칭되는 기본 ClassFilter 인스턴스입니다.
     */
    ClassFilter TRUE = TrueClassFilter.INSTANCE;

}
```
```java
public interface MethodMatcher {

    /**
     * 지정된 메서드가 매칭되는지 '정적(static)'으로 확인합니다.
     * 이 메서드가 false를 반환하거나 isRuntime()이 false를 반환하면,
     * 런타임 체크(인자가 포함된 matches 호출)는 수행되지 않습니다.
     */
    boolean matches(Method method, Class<?> targetClass);

    /**
     * 이 MethodMatcher가 '동적(dynamic)'인지 여부를 반환합니다.
     * true를 반환하면 static matches가 통과하더라도 런타임에 인자값을 포함하여
     * 다시 한번 최종 체크(matches(Method, Class, Object[]))를 수행해야 합니다.
     */
    boolean isRuntime();

    /**
     * 메서드에 대한 '런타임(동적)' 매칭 여부를 확인합니다.
     * 정적 matches가 true를 반환하고, isRuntime()이 true인 경우에만 호출됩니다.
     * 실제 메서드 실행 직전에 인자(args)를 보고 판단합니다.
     */
    boolean matches(Method method, Class<?> targetClass, @Nullable Object... args);

    /**
     * 모든 메서드에 매칭되는 기본 MethodMatcher 인스턴스입니다.
     */
    MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;

}
```
* 판단 로직 : `ClassFilter` 혹은 `MethodMatcher`를 모두 동원해 모든 메서드를 대조한다. 매칭되면 프록시를 생성해 컨테이너에 등록하고 매칭되는 것이 없다면 원본 객체를 그대로 등록한다.

**2. 프록시 실행 단계(런타임 호출 시점)**

* 목적 : 실제 메서드 호출 시 부가 기능 적용 점검
* 상세 : 프록시가 생성되었더라도 모든 메서드 호출에 어드바이스가 실행되는 것은 아니다.
* 판단 로직 : 클라이언트가 메서드를 호출하면 프록시가 다시 한 번 포인트컷에 요청해 매칭 성공 시 어드바이스를 먼저 실행한 후 타겟 메서드를 호출하고 매칭 실패 시 부가 기능 없이 실제 타겟의 비즈니스 로직을 바로 호출한다.
