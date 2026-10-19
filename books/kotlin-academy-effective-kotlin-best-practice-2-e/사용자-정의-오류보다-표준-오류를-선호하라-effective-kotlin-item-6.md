# (Effective Kotlin Item 6) Kotlin - 사용자 정의 오류보다 표준 오류를 선호하라

#### 사용자 정의 오류보다 표준 오류를 선호하라

* 예외는 예상치 못한 상황을 나타내기 위해 사용한다.
* 표준 라이브러리에 이 상황을 나타내는 적절한 오류가 없는 경우라면 사용자 정의 오류를 사용할 수도 있다.
* 그러나 가능하면 사용자 정의 오류를 정의하는 대신 표준 라이브러리 예외를 사용해야 한다.
* 표준 라이브러리 예외는 잘 알려져 있으므로 이를 재사용하는 것이 좋다.

#### IllegalArgumentException

* 메서드에 전달될 인수가 잘못되었음을 나타낸다.
* 일반적으로 `require` 또는 `requireNotNull`을 사용하여 예외를 던진다.

#### IllegalStateException

* 프로그램의 상태가 잘못되었음을 나타낸다.
* 일반적으로 `check` 또는 `checkNotNull` 또는 `error`를 사용하여 오류를 던진다.

#### UnsupportedOperationException

* 선언된 메서드가 객체에서 지원하지 않음을 나타낸다.

#### IndexOutOfBoundsException

* 인덱스 매개변수 값이 범위를 벗어났음을 나타낸다.
* 특히 컬렉션과 배열에서 주로 사용된다.

#### ConcurrentModificationException

* 동시 수정이 금지된 상황에서 동시 수정이 감지되었음을 나타낸다.

#### NoSuchElementException

* 요청된 요소가 존재하지 않음을 나타낸다.
