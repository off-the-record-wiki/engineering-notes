# (Effective Java Item 4) Java - 인스턴스화를 막으려거든 private 생성자를 사용하라

* 정적 멤버만 만든 유틸리티 클래스의 경우 인스턴스로 만들어 쓰려고 설계한 것이 아니다.
* 하지만 생성자를 명시하지 않으면 컴파일러가 자동으로 기본 생성자를 만들어준다.
* 즉, 매개변수를 받지 않는 public 생성자가 만들어지며, 사용자는 이 생성자가 자동 생성된 것인지 구분할 수 없다.
* 이런 문제를 방지하고자 추상 클래스로 만든다고 하더라도 인스턴스화를 막을 순 없다. 하위 클래스를 만들어 인스턴스화하면 의미가 퇴색된다.
* 컴파일러가 기본 생성자를 만드는 경우는 오직 명시된 생성자가 없을 때뿐이니 private 생성자를 추가하면 클래스 인스턴스화를 막을 수 있다.

```java
public final class RegularExpressions {

    // 기본 생성자가 만들어지는 것을 막는다(인스턴스화 방지용)
    private RegularExpressions() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    public static final String NICKNAME = "^[a-zA-Z0-9가-힣]{2,10}$";
    public static final String ENTERPRISE_EMAIL = "^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,30}$";
    public static final String EMAIL = "^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,30}$";
    public static final String PASSWORD = "^.{10,}$";
    public static final String PHONE = "^010\\d{8}$";
    public static final String VERIFICATION_CODE = "^\\d{6}$";
}
```
* `final`로 만들어 상속도 불가능하여 완전한 불변 유틸리티 클래스로 설계한다.
* `private` 생성자와 `AssertionError`를 던져 리플렉션 API 사용을 방지해 인스턴스화를 방지한다.
* 생성자가 분명 존재하는데 호출할 수는 없으니 직관적이지 않다. 그렇기에 적절한 주석을 달아두는 것이 다른 사람의 이해를 도울 수 있다.
