# Java - Default Method

#### 디폴트 메서드 개념

* 자바 8부터 하위 호환성 문제를 해결하기 위해 디폴트 메서드가 추가되었다.
* 인터페이스에 메서드를 새로 추가하면서 기본 구현을 제공할 수 있는 기능이다.

```java
public interface Notifier {

   // 알림 보내는 기본 기능
   void notify(String message);

   // 신규 기능 추가
   default void scheduleNotification(String message, LocalDateTime localDateTime) {
      System.out.println("기본 스케줄링 message: " + message + ", time: " + localDateTime);
   }
}
```

* 이렇게 되면 새로운 메서드가 추가되었음에도 불구하고 해당 인터페이스를 구현하는 기존 클래스들이 큰 수정 없이도 동작을 계속 유지할 수 있게 된다.

#### 디폴트 메서드 도입 이유

* 하위 호환성 보장
* 라이브러리 확장성
* 람다와 스트림 API 연계 - Collection 인터페이스에 `stream()` 디폴트 메서드 추가 Iterable 인터페이스 `forEach()` 디폴트 메서드 추가
* 설계 유연성 향상

> 이런 정도의 기능이 있다는 것으로만 알아두되 아무 곳에서나 디폴트 메서드를 쓰는 것을 권장하진 않는다.
