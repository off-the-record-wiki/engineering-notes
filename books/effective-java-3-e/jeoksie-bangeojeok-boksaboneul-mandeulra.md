# (Effective Java Item 50) Java ‐ 적시에 방어적 복사본을 만들라

* 클라이언트가 불변식을 깨뜨리려 혈안이 되어 있다고 가정하고 방어적으로 프로그래밍을 해야 한다.
```java
// Bad
public final class Period {
    private final Date start;
    private final Date end;

   /**
    * @param start 시작 시각
    * @param end 종료 시각
    * @throws IllegalArgumentException 시작 시각이 종료 시각보다 늦을 때 발생한다.
    * @throws NullPointerException start 혹은 end가 null이면 발생한다.
    */
    public Period(Date start, Date end) {
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException(start + "가 " + end + "보다 늦다.");
        }
        this.start = start;
        this.end = end;
    }

    public Date start() {
        return start;
    }

    public Date end() {
        return end;
    }
}
```
* 외부 공격으로부터 인스턴스 내부를 보호하려면 생성자에서 받은 가변 매개변수 각각을 방어적으로 복사해야 한다.
* 매개변수 유효성을 검사하기 전에 방어적 복사본을 만들고 이 복사본으로 유효성을 검사한다.

```java
public Period(Date start, Date end) {
    this.start = new Date(start.getTime());  // (1). 방어적 복사본을 만든다.
    this.end = new Date(end.getTime());

    if (start.compareTo(end) > 0) {          // (2). 유효성을 검증한다.
        throw new IllegalArgumentException(start + "가 " + end + "보다 늦다.");
    }
}
```
* 그러나 한 가지 주의할 점은 방어적 복사에는 성능 저하가 따르고 항상 쓸 수 있다는 것이 아니라는 것을 명심해야 한다.
