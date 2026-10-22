# (Effective Java Item 25) Java ‐ 톱레벨 클래스는 한 파일에 하나만 담으라

#### 톱레벨 클래스는 한 파일에 하나만 담으라

* 아주 간단한 내용이다. 단순히 톱레벨 클래스들은 서로 다른 소스 파일롭 분리하라는 내용이다.
* 굳이 여러 톱레벨 클래스들이 한 파일에 담겨야 한다면 정적 멤버 클래스를 사용하는 방법을 고민하자.

```java
public class Test {

    public static class Utensil {
        static final String NAME = "pan";
    }

    public static class Dessert {
        static final String NAME = "pot";
    }
}
```
