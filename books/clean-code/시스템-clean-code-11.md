# (Clean Code 11) Clean Code - 시스템

#### 시스템 제작과 시스템 사용을 분리하라

* 제작(construction)과 사용(use)은 아주 다르다.
* 소프트웨어 시스템은 애플리케이션 객체를 제작하고 의존성을 서로 연결하는 준비 과정과 준비 과정 이후에 이어지는 런타임 로직을 분리해야 한다.
* 시작 단계는 모든 애플리케이션이 풀어야 할 관심사(concern)다.
* 관심사 분리는 가장 오래되고 가장 중요한 설계 기법 중 하나이다.

#### Main 분리

* 시스템 생성과 시스템 사용을 분리하는 한 가지 방법으로, 생성과 관련한 코드는 모두 `main`이나 `main`이 호출하는 모듈로 옮기고, 나머지 시스템은 모든 객체가 생성되었고 모든 의존성이 연결되었다고 가정한다.

```java
// main: 생성과 조립만 담당
public class Main {
    public static void main(String[] args) {
        Config config = Config.load(args);
        PaymentGateway gateway = new TossPaymentGateway(config.apiKey(), config.timeout());
        OrderRepository repo = new JdbcOrderRepository(dataSource(config));
        OrderService service = new OrderService(gateway, repo);

        new Application(service).run();  // 다 만들어 넘기고 손 뗌
    }
}

// 나머지 시스템: 사용만 담당
public class OrderService {
    private final PaymentGateway gateway;
    private final OrderRepository repo;

    public OrderService(PaymentGateway gateway, OrderRepository repo) { // 받기만 함
        this.gateway = gateway;
        this.repo = repo;
    }

    public void placeOrder(Order order) {
        gateway.pay(order.getAmount());
        repo.save(order);
    }
}
```

#### 팩토리

* 때로는 객체가 생성되는 시점을 애플리케이션이 결정할 필요도 생긴다.

```java
// Product
public interface Button {
    void render();
    void onClick(Runnable handler);
}

// ConcreteProduct
public class WindowsButton implements Button {
    @Override public void render() { /* Win32 스타일로 그림 */ }
    @Override public void onClick(Runnable handler) { /* ... */ }
}

public class MacButton implements Button {
    @Override public void render() { /* Cocoa 스타일로 그림 */ }
    @Override public void onClick(Runnable handler) { /* ... */ }
}
```

#### [의존성 주입](https://ko.wikipedia.org/wiki/%EC%9D%98%EC%A1%B4%EC%84%B1_%EC%A3%BC%EC%9E%85)

* 사용과 제작을 분리하는 강력한 메커니즘 중 하나가 의존성 주입(Dependency Injection)이다.
* 의존성 주입은 제어의 역전(Inversion of Control, IoC) 기법을 의존성 관리에 적용한 메커니즘이다.
* 제어 역전에서는 한 객체가 맡은 보조 책임을 새로운 객체에 전적으로 떠넘긴다. 새로운 객체는 넘겨받은 책임만 맡으므로 단일 책임 원칙도 지키게 된다.
* Spring Framework는 가장 널리 알려진 자바 DI 컨테이너를 제공한다. 객체 사이 의존성은 XML 파일에 정의한다. 그리고 자바 코드에서 이름으로 특정한 객체를 요청한다.
* 대다수 DI 컨테이너는 필요할 때까지는 객체를 생성하지 않고, 대부분은 계산 지연이나 비슷한 최적화에 쓸 수 있도록 팩토리를 호출하거나 프록시를 생성하는 방법을 제공한다. 즉, 계산 지연 기법이나 이와 유사한 최적화 기법에서 이런 메커니즘을 사용할 수 있다.

#### 자바 프록시

* 자바 프록시는 단순한 상황에 적합하다. 개별 객체나 클래스에서 메서드 호출을 감싸는 경우가 좋은 예시이다.
* 하지만 JDK에서 제공하는 동적 프록시는 인터페이스만 지원한다. 클래스 프록시를 사용하려면 [CGLIB](https://github.com/cglib/cglib), ASM, Javassist 등과 같은 바이트 코드 처리 라이브러리가 필요하다.

#### 결론

* 시스템은 깨끗해야 한다.
* 깨끗하지 못한 아키텍처는 도메인 논리를 흐리며 기민성을 저하시킨다. 도메인 논리가 흐려지면 제품 품질이 저하된다.
* 버그가 숨어들기 쉬워지고 스토리를 구현하기 어려워지는 탓이다. 기민성이 떨어지면 생산성이 낮아져 TDD가 제공하는 장점이 사라진다.
* **모든 추상화 단계에서 의도가 명확해야한다.** POJO를 작성하고 관점 혹은 관점과 유사한 메커니즘을 사용해 각 구현 관심사를 분리해본다.
* 시스템을 설계하든 개별 모듈을 설계하든, **실제로 돌아가는 가장 단순한 수단을 사용해야 한다는 사실을 명심한다.**
