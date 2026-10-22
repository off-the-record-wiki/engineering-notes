# (Effective Java Item 72) Java ‐ 표준 예외를 사용하라

* 예외도 마찬가지로 재사용하는 것이 좋으며, 자바 라이브러리는 대부분 API에서 쓰기에 충분한 수의 예외를 제공한다.
* `Exception`, `RuntimeException`, `Throwable`, `Error`는 직접 재사용하지 말아야 한다.
* 위에 나열한 클래스들은 "추상 클래스"라고 생각하자.

|예외|주요 쓰임|
|:----:|:----:|
|`IllegalArgumentException`|허용하지 않는 값이 인수로 건네졌을 때|
|`IllegalStateException`|객체가 메서드를 수행하기에 적절하지 않은 상태일 때|
|`NullPointerException`|null을 허용하지 않는 메서드에 null을 건넸을 때|
|`IndexOutOfBoundsException`|인덱스가 범위를 넘어섰을 때|
|`ConcurrentModificationException`|허용하지 않는 동시 수정이 발견되었을 때|
|`UnsupportedOperationException`|호출한 메서드를 지원하지 않을 때|

* 상황에 부합한다면 항상 표준 예외를 재사용하자.
* 단, 예외는 직렬화할 수 있다는 사실을 기억하자. 이 사실만으로도 자신만의 예외를 새로 만들지 말아야 할 근거로 충분할 수 있다.
