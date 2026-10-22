# (Effective Java Item 82) Java - 쓰레드 안전성 수준을 문서화하라

#### 스레드 안전성 수준의 5가지 단계

**1. 불변(Immutable)**
* 상수와 같아서 내부 상태가 변하지 않는다.
* 외부 동기화가 필요 없는 가장 완벽한 안전 상태
* ex : `String`, `BigInteger`, `Integer`
* 전역 변수를 final로 선언하고 수정자(setter)를 두지 않는다.

**2. 무조건적 쓰레드 안전(Unconditionally Thread-Safe)**
* 인스턴스 내부 상태가 가변이지만 내부에서 정교하게 동기화를 처리하여 외부에서 별도 락 없이 동시 사용이 가능하다.
* ex : `ConcurrentHashMap`, `AtomicInteger`
* 별도 주석 없이 타입 자체나 구조로 안전함을 드러낸다.

**3. 조건부 쓰레드 안전(Conditionally Thread-Safe)**
* 일부 메서드는 그냥 써도 안전하지만, 특정 메서드를 연달아 호출할 때는 외부에서 수동으로 락을 쥐어야 한다.
* ex : `Collections.synchronizedMap()`이 반환하는 컬렉션
* 반드시 어떤 락을 얻어야 하는지 문서화를 해 둘 필요가 있다.

**4. 쓰레드 안전하지 않음(Not Thread-Safe)**
* 내부 상태가 가변이며 내부 동기화가 전혀 없다.
* 멀티쓰레드 환경에서 쓰려면 외부에서 호출 시점에 동기화 블록으로 감싸야 한다.
* ex : `ArrayList`, `HashMap`
* 싱글톤 빈의 멤버 변수로 쓰지 않거나 지역 변수로만 활용한다.

**5. 쓰레드 적대적(Thread-Hostile)**
* 외부에서 아무리 `synchronized`로 락을 걸어도 멀티 쓰레드 환경에서 오동작하거나 고장난다.
* 내부 동기화 없이 공유 전역 변수를 수정하는 레거시 메서드
* 실무에서 발견 즉시 리팩토링해서 제거해야하는 대상

#### 서비스 안정성을 높이는 비공개 락 객체 (Private Lock Object) 패턴

```java
// Good
public class SubSystem {
    // 1. 외부에서 절대 접근할 수 없는 비공개 락 객체 선언 (final 필수)
    private final Object lock = new Object();

    public void process() {
        // 2. 클래스 내부에서만 이 객체를 조준하여 동기화 수행
        synchronized(lock) {
            // 비즈니스 로직 진행
        }
    }
}
```

* 클라이언트가 객체 동기화에 관여할 수 없다.
* 무조건적 쓰레드 안전 클래스에서만 사용할 수 있다. 조건부 쓰레드 안전 클래스에서는 사용할 수 없다.
