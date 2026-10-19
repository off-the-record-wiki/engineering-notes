# (Effective Java Item 70) Java ‐ 복구할 수 있는 상황에는 검사 예외를, 프로그래밍 오류에는 런타임 예외를 사용하라

#### 검사 예외(Checked Exception)
* 반드시 예외처리를 해야 한다.
  * `throw`, `try~catch`를 이용해 예외 처리를 강제한다.
  * 호출하는 측에서 복구할 수 있으리라고 판단되는 상황이라면 검사 예외를 사용해야한다.
* 복구에 필요한 정보를 알려주는 메서드를 정의하는 것이 좋다.
  * 예를 들어, 물건을 구입하는데 잔액이 부족한다면 잔액이 "얼마나 부족한지를" 알려줘야한다.

#### 비검사 예외(UnChecked Exception)
* 런타임 예외와 에러가 있다.
* 해당 타입의 예외는 프로그램에서 잡을 필요가 없다.
  * 복구가 불가능하거나 더 실행해봤자 의미가 없기 때문에 예외 처리를 강제하지 않는다.
  * 프로그래밍 오류를 나타낼 때에는 비검사 예외를 사용해야 한다.
* 개발자가 구현하는 비검사 예외는 모두 `RuntimException`을 상속받아야 한다.
  * `Error`는 상속받지 말아야 할 뿐만 아니라 직접 `throw`를 해서도 안 된다.
  * `Exception`, `RuntimeException`, `Error`를 상속하지 않은 `throwable`은 만들지 않는 것이 좋다.

> [Checked/Unchecked Exception 차이란?](https://cheese10yun.github.io/checked-exception/#checked-unchecked-exception-cai)
