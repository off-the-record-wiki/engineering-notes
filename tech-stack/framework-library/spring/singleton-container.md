# Spring - Singleton Container

#### 싱글톤 패턴

* 클래스의 인스턴스가 오직 1개만 생성되는 것을 보장하는 디자인 패턴이다.
* 객체 인스턴스를 2개 이상 생성하지 못하도록 막아야 한다. 그래서 생성자를 `private` 접근 제어자로 지정해 외부에서 임의로 객체를 생성할 수 없도록 막아야 한다.

```java
public class SingletonService {

    // 1. static 영역에 객체를 딱 1개만 생성해둔다.
    private static final SingletonService instance = new SingletonService();

    // 2. public으로 열어서 객체 인스턴스가 필요하면 이 static 메서드를 통해서만 조회하도록 허용한다.
    public static SingletonService getInstance() {
        return instance;
    }

    // 3. 생성자를 private으로 선언해서 외부에서 new 키워드를 사용한 객체 생성을 못하게 막는다.
    private SingletonService() {
    }

    public void logic() {
        System.out.println("싱글톤 객체 로직 호출");
    }
}
```

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c707dc57-b110-4caf-a8e2-6714cdd48fad.png)

* 싱글톤 패턴의 단점
  * 위와 같이 접근 제어자로 생성자를 제한하는 등 싱글톤 패턴을 구현하는 코드 자체가 많다.
  * 의존관계상 클라이언트가 구체 클래스에 의존한다. 결국 적절한 추상화를 사용하지 못하므로 DIP를 위반하게 된다.
  * 테스트하기가 어렵다.
  * 내부 속성을 변경하거나 초기화하기 어렵다.
  * 유연성이 떨어진다.

> [Java ‐ private 생성자나 열거 타입으로 싱글턴임을 보증하라[Effective Java Item 3]](https://github.com/off-the-record-wiki/study/wiki/Java-%E2%80%90-private-%EC%83%9D%EC%84%B1%EC%9E%90%EB%82%98-%EC%97%B4%EA%B1%B0-%ED%83%80%EC%9E%85%EC%9C%BC%EB%A1%9C-%EC%8B%B1%EA%B8%80%ED%84%B4%EC%9E%84%EC%9D%84-%EB%B3%B4%EC%A6%9D%ED%95%98%EB%9D%BC%5BEffective-Java-Item-3%5D)

#### 싱글톤 컨테이너

* 스프링 컨테이너는 싱글톤 패턴을 적용하지 않아도 객체 인스턴스를 싱글톤으로 관리한다.
* 스프링 컨테이너는 싱글톤 컨테이너 역할을 한다. 이렇게 싱글톤 객체를 생성하고 관리하는 기능을 싱글톤 레지스트리라고 한다.
* 고객의 요청이 올 때마다 객체를 생성하는 것이 아니라 아래와 같이 이미 만들어진 객체를 공유해서 객체를 재사용할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/d9ac10a7-883a-4b36-9452-3ea060eba75e.png)

> [📚Singleton Registry Pattern](https://stackoverflow.com/questions/16744424/whats-the-difference-between-singleton-and-registry-design-pattern)
> 
> * 스프링 컨테이너는 싱글톤 레지스트리로서의 역할을 수행한다. 직접 구현한 싱글톤 패턴의 치명적 단점들을 해결하면서 객체를 싱글톤으로 관리해주는 기능을 한다.
>
> **1. 왜 필요한가?(직접 구현한 싱글톤의 문제점)**
> * 유연성 저하 : `private` 생성자 때문에 상속이 어렵고 테스트하기가 까다롭다.
> * 의존관계 문제 : `getInstance()`를 호출해야 하므로 클라이언트가 구체 클래스에 의존하게 되어 DIP를 위반하기 쉽다.
> * 안티패턴 : 내부 상태를 변경하기 어렵고, 초기화 시점이 제각각일 수 있다.
>
> **2. 싱글톤 레지스트리의 특징**
> * 관리의 주체 : 스프링 컨테이너가 직접 객체를 생성하고 관리한다.
> * POJO 활용 : 생성자를 `private`으로 막지 않아도 순수 자바 클래스(POJO)를 싱글톤으로 유지해준다.
> * DIP/OCP 준수 : 클라이언트 코드는 싱글톤 패턴을 위한 별도의 로직 없이 의존관계를 주입받아 사용할 수 있다.
>
> **3. 스프링의 싱글톤 관리 방식**
> * 스프링 컨테이너는 빈을 등록할 때 기본적으로 하나의 인스턴스만 생성해 내부 저장소에 보관한다.
> * 사용자가 빈을 요청하면 컨테이너는 자신이 관리하는 레지스트리에서 해당 빈이 있는지 확인 후 있다면 보관 객체를 반환하고 없다면 생성해서 보관 후 반환하게 된다.
> 
> **4. 주의사항: 무상태(Stateless) 설계**
> * 싱글톤 레지스트리를 사용할 때 가장 주의해야 할 점은 객체를 무상태(Stateless)로 설계해야 한다는 점이다.
> * 특정 클라이언트에 의존적인 필드가 있으면 안 된다.
> * 가급적 읽기 전용으로 설계하며, 가변적인 상태를 필드에 저장하지 않는다.
> * 지역변수, 파라미터, 쓰레드 로컬 등을 사용해 데이터 혼선이 없도록 해야 한다. 

#### 싱글톤 방식의 주의점

* 싱글톤 패턴은 객체 인스턴스를 하나만 공유해서 사용하기 때문에 상태를 유지하는 설계를 취해서는 안 된다.
* 무상태로 설계해야 한다. → 특정 클라이언트에 의존적인 필드가 있으면 안되고 특정 클라이언트가 값을 변경할 수 있는 필드가 있어선 안 된다.
* 필드 대신에 자바에서 공유되지 않는 스택 영역의 지역 변수나 파라미터, 쓰레드 로컬 등을 사용해야 한다.

```java
public class StatefulService {

    private int price; // 상태를 유지하는 필드 (공유되는 필드)

    public void order(String name, int price) {
        System.out.println("name = " + name + " price = " + price);
        this.price = price; // 여기가 문제!
    }

    public int getPrice() {
        return price;
    }
}
```
```java
public class StatefulServiceTest {

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(SingletonConfig.class);

    @Test
    @DisplayName("싱글톤은 상태를 보존하는 설계를 하면 장애가 발생한다.")
    void singletonStatefulService() {
        StatefulService statefulService1 = ac.getBean("statefulService", StatefulService.class);
        StatefulService statefulService2 = ac.getBean("statefulService", StatefulService.class);

        // ThreadA: A 사용자가 1000원 주문
        statefulService1.order("userA", 1000);
        // ThreadB: B 사용자가 2000원 주문 (이 시점에 필드값이 2000으로 덮어씌워짐)
        statefulService2.order("userB", 2000);

        // ThreadA: 사용자 A가 주문 금액 조회
        int price = statefulService1.getPrice();

        // 기대값은 1000원이었으나, 실제로는 2000원이 출력됨
        System.out.println("price = " + price);
        assertThat(price).isEqualTo(2000); // 테스트가 실패하거나, assert를 2000으로 잡아야 통과함
    }

    @Configuration
    static class SingletonConfig {
        @Bean
        public StatefulService statefulService() {
            return new StatefulService();
        }
    }
}
```

> 테스트 코드 실행 결과

```
상태가 있는(stateful) 서비스: userA - 1000
상태가 있는(stateful) 서비스: userB - 2000

Expected :1000
Actual   :2000
```

* userA가 주문한 금액을 조회해서 나오길 기대했는데 userB가 주문한 금액이 조회해서 나오는 것을 볼 수 있다.
* 아래와 같이 금액 필드를 공유하지 않도록 price를 파라미터로 사용하면 공유되는 문제를 해결할 수 있다.

```java
public class StatefulService {

    // price를 필드에 저장하지 않고 바로 반환하여 무상태 설계
    public int order(String name, int price) {
        System.out.println("name = " + name + " price = " + price);
        return price;
    }
}
```

#### @Configuration과 싱글톤

* 구성 파일에 `@Configuration` 어노테이션을 붙이면 포함된 메서드에 붙어 있는 `@Bean`을 호출해서 스프링 빈을 생성한다.
* CGLIB라는 바이트 코드 조작 라이브러리를 통해 구성 파일 클래스를 상속받은 임의의 다른 클래스를 만들고 그 클래스를 스프링 빈으로 등록하여 싱글톤으로 등록하는 것이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/7764112a-6239-4ce8-8ff5-962bd46cfe18.png)

* 만약에 `@Bean`만 사용하게 되면 싱글톤으로 관리되지 않는다.
* 설정 정보는 항상 `@Configuration` & `@Bean`을 같이 사용해 싱글톤으로 관리하자.

> [📚CGLIB](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/cglib/package-summary.html)
>
> **1. CGLIB의 정의**
> * CGLIB는 자바의 바이트 코드를 조작하여 동적으로 클래스를 생성해주는 라이브러리이다.
> * 주로 인터페이스가 없는 일반 클래스의 동적 프록시를 생성할 때 사용된다. 대상 클래스를 상속받는 하위 클래스를 런타임에 생성하여 기능을 확장하는 방식을 취한다.
>
> **2. 핵심 동작 원리 : 상속(Inheritance)**
> * JDK Dynamic Proxy가 인터페이스를 구현하여 프록시를 만드는 것과 달리, CGLIB는 대상 클래스를 상속받는다.
> * 구동 방식 : 대상 클래스의 바이트 코드를 읽어 이를 상속받는 프록시 객체를 생성한다.
> * 메서드 인터셉트 : `MethodInterceptor`를 통해 타겟 메서드 호출을 가로채고, 전후에 공통 로직을 실행한다.
>
> **3. 스프링에서의 결정적 역할 : @Configuration**
> * 스프링에서 CGLIB가 가장 빛을 발하는 순간은 바로 설정 정보(@Configuration)를 관리할 때이다.
> * 싱글톤 보장 : `@Configuration`이 붙은 설정 클래스 내에서 여러 번 호출되는 빈 등록 메서드들이 항상 동일한 객체(싱글톤)를 반환하도록 보장한다.
> * 작동 방식 : 스프링은 CGLIB를 이용해 개발자가 작성한 `AppConfig`를 그대로 사용하지 않고 `AppConfig`를 상속받은 `AppConfig@CGLIB` 객체를 만들어 컨테이너에 등록한다.
>
> **4. CGLIB의 특징 및 제약 사항**
> * 인터페이스가 없는 구체 클래스도 프록시로 만들 수 있고 일반적인 JDK Dynamic Proxy보다 성능이 뛰어나다.
> * `final` 클래스의 경우 상속이 불가능하므로 프록시를 생성할 수 없다.
> * `final` 메서드의 경우 오버라이딩이 불가능하므로 프록시 로직을 입힐 수 없다.
> * 기본 생성자 : CGLIB의 경우 상속을 활용하므로 대상 클래스에 기본 생성자가 필요하다. 
