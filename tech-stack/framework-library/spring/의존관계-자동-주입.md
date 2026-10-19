# Spring - Dependency Injection

#### 다양한 의존관계 자동 주입 방법

* 생성자 주입 방법(가장 권장되는 방식)
* 수정자 주입 방법(setter)
* 필드 주입 방법

#### 의존관계 자동 주입 방법 1 - 생성자 주입 방법

* 생성자 호출 시점에 딱 1번만 호출되는 것이 보장된다.
* 불변, 필수 의존관계에 사용한다.

```java
public class OrderServiceImpl implements OrderService {

    private final MemberRepository memberRepository;
    private final DiscountPolicy discountPolicy;

    // 생성자가 딱 1개만 있으면 @Autowired를 생략해도 스프링이 자동으로 주입해줍니다.
    public OrderServiceImpl(MemberRepository memberRepository, DiscountPolicy discountPolicy) {
        this.memberRepository = memberRepository;
        this.discountPolicy = discountPolicy;
    }

    // ...
}
```

#### 의존관계 자동 주입 방법 2 - 수정자 주입 방법

* 선택, 변경 가능성이 있는 의존 관계에 사용한다.
* 자바빈 프로퍼티 규약의 수정자 메서드 방식을 사용한다.

```java
@Component
public class OrderServiceImpl implements OrderService {

    private MemberRepository memberRepository;
    private DiscountPolicy discountPolicy;

    @Autowired
    public void setMemberRepository(MemberRepository memberRepository) {
        System.out.println("memberRepository = " + memberRepository);
        this.memberRepository = memberRepository;
    }

    @Autowired
    public void setDiscountPolicy(DiscountPolicy discountPolicy) {
        System.out.println("discountPolicy = " + discountPolicy);
        this.discountPolicy = discountPolicy;
    }
}
```

#### 필드 주입

* 필드에 바로 주입하는 방법이다.
* 코드가 간결하나 DI 프레임워크가 없다면 아무것도 할 수 없다.

```java
@Component
public class OrderServiceImpl implements OrderService {

    @Autowired 
    private MemberRepository memberRepository;
    
    @Autowired 
    private DiscountPolicy discountPolicy;

    // 생성자나 Setter 없이 필드에 바로 @Autowired를 붙이는 방식입니다.
}
```

> ❓가장 좋은 방식은?
> 
> * 결론부터 말하자면 생성자 주입 방법을 선택하는 것을 강력히 권장한다.
> * 대부분의 의존관계 주입은 한 번 일어나면 애플리케이션 종료 시점까지 의존관계를 변경할 일이 없다. 오히려 대부분의 의존관계는 애플리케이션 종료 전까지 변하면 안 된다.
> * 수정자 주입을 사용하면 결국 public으로 열어두어야 하는데 이렇게 열어두게 되면 누군가 실수로 변경할 수 있기 때문에 좋은 설계가 아니다.
> * 생성자 주입은 객체를 생성할 때, 딱 1번만 호출되므로 이후에 호출되는 일이 없다. 따라서 불변하게 설계할 수 있다.

#### 조회되는 빈이 2개 이상인 경우

* 조회되는 빈이 2개 이상인 경우 `NoUniqueBeanDefinitionException` 예외가 발생한다.

#### 조회되는 빈이 2개 이상인 경우 해결 방법 1 - @Autowired 필드명 매칭

```java
@Autowired
private DiscountPolicy discountPolicy; // 특정 빈 이름으로 변경
```
```java
@Autowired
private DiscountPolicy rateDiscountPolicy; // 특정 빈 이름으로 변경
```

#### 조회되는 빈이 2개 이상인 경우 해결 방법 2 - @Qualifier

* 추가 구분자를 붙여주는 방법이다. 

```java
@Component
@Qualifier("mainDiscountPolicy")
public class RateDiscountPolicy implements DiscountPolicy {}
```

```java
@Component
@Qualifier("fixDiscountPolicy")
public class FixDiscountPolicy implements DiscountPolicy {}
```

#### 조회되는 빈이 2개 이상인 경우 해결 방법 3 - @Primary

* 우선순위를 정하는 방법이다.

```java
@Component
@Primary
public class RateDiscountPolicy implements DiscountPolicy {}
```

```java
@Component
public class FixDiscountPolicy implements DiscountPolicy {}
```

#### 어노테이션 커스텀 방법

```java
/**
 * @Qualifier의 기능을 포함하면서, 오타 방지 및 컴파일 타임 체크를 위해 
 * 커스텀 애노테이션을 정의할 때 참조하는 @Qualifier 소스코드 구조입니다.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Qualifier("mainDiscountPolicy") // 예시: 실제 커스텀 애노테이션을 만들 때 이런 식으로 이름을 부여합니다.
public @interface MainDiscountPolicy {
    // ...
}
```
