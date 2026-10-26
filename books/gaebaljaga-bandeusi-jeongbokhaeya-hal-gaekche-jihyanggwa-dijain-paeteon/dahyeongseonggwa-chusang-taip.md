# 개발자가 반드시 정복해야 할 객체 지향과 디자인 패턴 - 다형성과 추상 타입

#### 다형성과 상속

![](https://github.com/user-attachments/assets/ba4b9bc6-7dc1-4e11-951f-e342f9004c9d)

* 다형성(Polymorphism)은 한 객체가 여러 가지 모습을 갖는다는 것을 의미한다.
* 여기서 모습이란 타입을 뜻하는데, 즉, 다형성이란 하나의 객체가 여러 타입을 가질 수 있다는 것을 뜻한다.

#### 추상 타입과 유연함

* 추상화(abstraction)는 데이터나 프로세스 등을 의미가 비슷한 개념이나 표현으로 정의하는 과정이다.
* 추상화된 타입은 오퍼레이션의 시그니처만 정의할뿐 실제 구현을 제공하지는 못한다.
* 추상 타입은 구현을 제공할 수 없기 때문에 보통 구현을 제공하지 않는 타입 - 자바의 인터페이스나 C++ 추상 메서드로만 구성된 추상 클래스를 이용해서 추상 타입을 정의한다.

#### 추상 타입과 실제 구현체의 연결

![](https://github.com/user-attachments/assets/bc7bbc20-a8c8-4fe3-94c6-dac9e6436752)

* `collector.collect(log)` 하나의 호출이, 실제 객체가 무엇이냐에 따라 파일, 소켓, DB 테이블로 기록된다.

```java
public class ByteSourceFactory {

    public ByteSourceFactory create() {
        if (useFile()) {
            return new FileDataReader();
        } else {
            return new SocketDataReader();
        }
    }

    private boolean useFile() {
        String useFileVal = System.getProperty("useFile");
        return useFileVal != null && Boolean.valueOf(useFileVal);
    }

    private static ByteSourceFactory instance = new ByteSourceFactory();
    public static ByteSourceFactory getInstance() {
        return instance;
    }

    private ByteSourceFactory() {
    }
}
```

#### 인터페이스에 대고 프로그래밍하기(program to interface)

* 여기서 말하는 인터페이스란 자바나 C#의 인터페이스가 아니라, 오퍼레이션을 정의한 인터페이스이다.
* 즉, 이 말은 실제 구현을 제공하는 콘크리트 클래스를 사용해서 프로그래밍하지 말고, 기능을 정의한 인터페이스를 사용해서 프로그래밍하라는 뜻이다.
* 인터페이스는 최초 설계에서 바로 도출되기 보다는 요구 사항의 변화와 함께 점진적으로 도출이 되는 경우가 부지기수이다. 즉, 인터페이스는 새롭게 발견된 추상 개념을 통해서 도출된다.
* 추상 타입을 사용하면 기존 코드를 건드리지 않고서 콘크리트 클래스를 교체할 수 있는데 이 때, 주의할 점이 있다. 유연함을 얻는 과정에서 타입이 증가하고 구조가 복잡해지기 때문에 모든 곳에서 인터페이스를 사용해선 안 된다. 이 경우 불필요하게 프로그램의 복잡도만 증가시킬 수 있기 때문이다. 인터페이스를 사용할 때는 변화 가능성이 높은 경우에 한해서만 사용해야 한다는 것을 명심해야 한다.
* 인터페이스라는 것은 추상화 과정을 통해 도출되는데 이 추상화 과정은 변화가 발생하는 곳에서부터 시작된다. 따라서 변화 가능성이 높은 콘크리트 클래스 대신 이를 추상화한 인터페이스를 사용하면 변경의 유연함이라는 효과를 얻을 수 있지만 변경 가능성이 매우 희박한 클래스에 대해 인터페이스를 만든다면 오히려 프로그램의 구조만 복잡해지고 유연함의 효과를 누릴 수 없는 상황이 연출된다.

#### 인터페이스는 인터페이스 사용자 입장에서 만들기

* 인터페이스를 작성할 때에는 그 인터페이스를 사용하는 코드 입장에서 작성해야 한다.
* 즉, 사용하는 코드 입장에서 쓴 인터페이스로 클라이언트가 진짜 하고 싶은 것을 정의해야 한다.

```java
// ❌ Bad
interface LogStore {
    fun openConnection()
    fun executeInsert(sql: String)
    fun flushBuffer()
    fun close()
}
```
```java
// ⭕ Good
interface LogCollector {
    fun collect(log: LogEntry)
}
```

#### 인터페이스와 테스트

* 실제 콘크리트 클래스 대신 진짜처럼 행동하는 객체를 `Mock` 객체라 한다.
* `Mock` 객체를 사용함으로써 실제 사용할 콘크리트 클래스의 구현 없이도 테스트를 할 수 있다.
* `Mock` 객체를 만드는 방법은 다양하지만 사용할 대상을 인터페이스로 추상화하면 좀 더 쉽게 `Mock` 객체를 만들 수 있게 되며, 이는 사용할 코드의 완성을 기다릴 필요 없이 코드를 먼저 빠르게 테스트할 수 있다.
> [Mockito를 이용한 MockTest](https://javacan.tistory.com/entry/MocktestUsingMockito)
