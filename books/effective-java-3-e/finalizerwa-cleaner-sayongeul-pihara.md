# (Effective Java Item 8) Java - finalizer와 cleaner 사용을 피하라

#### 자바의 객체 소멸자 finalizer와 cleaner

* `finalizer` : 예측할 수 없고, 상황에 따라 위험할 수 있어 일반적으로 불필요하다.
* `cleaner` : `finalizer`보다 덜 위험하지만, 여전히 예측할 수 없고, 느리고 일반적으로 불필요하다.

#### 왜 사용을 피해야 하는지?

**1. 즉시 수행된다는 보장이 없다.**

* `finalizer`나 `cleaner`를 얼마나 신속히 수행할지는 전적으로 가비지 컬렉션에 달렸다.

**2. 수행 여부도 보장하지 않는다.**

* 접근할 수 없는 일부 객체에 딸린 종료 작업을 수행하지 못한 채로 프로그램이 중단될 수 있기 때문이다.
* 즉, 상태를 영구적으로 수정하는 작업에서는 절대 `finalizer`나 `cleaner`에 의존해선 안 된다.

**3. 심각한 성능 문제도 동반한다.**

* `AutoCloseable 인터페이스를 사용하여 처리했을 때보다 약 50배 느리다.

**4. `finalizer`를 사용한 클래스는 `finalizer` 공격에 노출되어 심각한 보안 문제를 일으킬 수도 있다.** 

* 생성자나 직렬화 과정에서 예외가 발생하면 이 생성되다만 객체에서 악의적인 하위 클래스의 `finalizer`가 수행될 수 있기 때문에 가비지 컬렉터가 수집하지 못하게 막을 수 있다.

#### 왜 finalizer와 cleaner가 살아남았는지?

* 자원의 소유자가 close 메서드를 호출하지 않는 것에 대비한 안전망 역할을 하기 위함이다.
* 네이티브 피어를 회수하기 위해서이다. 왜냐하면 네이티브 피어는 자바 객체가 아니기 때문에 가비지 컬렉터에서 회수하지 못하기 때문이다.

#### finalizer나 cleaner를 대체하는 방안은?

* `AutoCloseable`을 구현해주고, 클라이언트에서 인스턴스를 다 쓰고 나면 `close()` 메서드를 호출하도록 한다.
* `AutoCloseable`에서 Cleaner를 사용하여 해주는 경우, `try-with-resources`을 사용한다.

```java
// Bad
public class ResourceWithFinalizer {
    
    @Override
    protected void finalize() throws Throwable {
        try {
            System.out.println("Finalizer 실행: 자원을 해제합니다.");
            // 여기서 파일 닫기나 네트워크 연결 해제를 시도함
        } finally {
            super.finalize();
        }
    }
}
```
```java
// Good
public class Room implements AutoCloseable {
    
    private boolean isClosed = false;

    // 자원을 사용하는 핵심 로직
    public void performAction() {
        if (isClosed) {
            throw new IllegalStateException("이미 닫힌 방입니다.");
        }
        System.out.println("방 청소 중...");
    }

    @Override
    public void close() {
        if (!isClosed) {
            System.out.println("Close 메서드 호출: 안전하게 자원을 해제합니다.");
            isClosed = true;
        }
    }
}
```
