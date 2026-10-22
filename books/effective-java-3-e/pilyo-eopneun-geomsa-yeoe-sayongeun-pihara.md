# (Effective Java Item 71) Java ‐ 필요 없는 검사 예외 사용은 피하라

* API 호출자가 예외 상황에서 복구할 방법이 없다면 비검사 예외를 던져야 한다.
* 만약 복구가 가능하고 호출자가 그 처리를 해주길 원한다면 옵셔널 반환을 고려해보자.
* 옵셔널로 상황 처리하기에 충분한 정보가 제공되지 않을 때만 검사 예외를 던져야 한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/849308e0-adb4-4cfa-bd5b-8dd9c4838125.png)

#### 검사 예외(Checked Exception)
* 애플리케이션 수행 중에 발생할 수 있는 예외를 검사하고 대비하라는 목적으로 사용해야 한다.
* 반드시 예외 처리를 해야 하며 컴파일러가 발견해서 컴파일 오류를 발생시킨다.
* `IOException`, `NoSuchFieldException`, `NoSuchMethodException` 등이 있다.

#### 비검사 예외(UnChecked Exception)
* `Error`는 시스템적인 예외를 의미하며, 심각한 상황에서 발생하는 예외를 말한다.
* 컴파일러가 예외 처리 여부를 확인하지 않는다. 따라서 개발자가 예외 처리 코드를 추가할지 말지를 결정한다.
* `NullPointerException`, `IndexOutOfBoundsException`, `NoSuchElementExcetpion` 등이 있다.

#### 검사 예외를 제대로 사용한다면?

* 검사 예외를 제대로 활용하면 API와 프로그램의 질을 높일 수 있다. 발생한 문제를 개발자가 처리해 안정성을 높이게 해준다.

#### 그렇다면 검사 예외를 과하게 사용한다면?

* 하지만 검사 예외를 과도하게 사용하면 오히려 쓰기 불편한 API가 된다.
* 검사 예외를 던질 수 있다고 선언하면 호출하는 코드에서 `try~catch` 블록을 사용해 예외를 잡아 처리하거나 `throws`를 통해 예외를 밖으로 던져야 한다.
* 검사 예외를 던지는 메서드는 스트림 안에서 직접 사용할 수 없다.

#### 겸사 예외를 사용하지 않고 해결한다면?

* Optional을 반환한다.
```java
// Bad
User user;
try {
    user = userService.findUserById(1L);
} catch (Exception e) {
    user = new User(1L);
    e.printStackTrace("유저가 존재하지 않습니다.");
}
```

```java
// Good
User user = userService.findUserById(1L)
              .orElseGet(() -> new User(1L));
```

* 기존 메서드를 2개로 쪼개 비검사 예외로 바꾼다.
```java
// Bad
try {
	obj.action(args);
} catch (TheCheckedException e) {
	... // 예외 처리
}
```
```java
if(obj.actionPermitted(args)){
	obj.action(args);
} else {
	... // 예외 처리
}
```
