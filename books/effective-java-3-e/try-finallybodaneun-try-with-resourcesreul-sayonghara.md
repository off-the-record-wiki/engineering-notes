# (Effective Java Item 9) Java - try‐finally보다는 try‐with‐resources를 사용하라

#### try-finally와 close() 메서드

* 자바 라이브러리에는 `close()` 메서드를 호출해 직접 닫아줘야 하는 자원이 많은데 대표적으로 `InputStream`, `OutputStream`, `java.sql.Connection` 등이 좋은 예시다.
* 자원 닫기는 클라이언트가 놓치기 쉬워서 예측할 수 없는 성능 문제로 직결되기도 한다.
* 전통적으로 자원이 제대로 닫힘을 보장하기 위한 수단으로 `try-finally`가 쓰였다.
* 사용되는 자원이 많아질수록 그에 비례해서 `try-finally`가 사용되는데 이렇게 된다면 코드 품질이 저하될 수 있다.

#### try-with-resources

* 해당 구조를 사용하려면 해당 자원이 `AutoClosable` 인터페이스를 구현해야 한다.
```java
// Good
static String firstLineOfFile(String path) throws IOException {
    // try-with-resources : 괄호 안에 선언된 자원은 블록을 나갈 때 자동으로 close()됨
    try (BufferedReader br = new BufferedReader(new FileReader(path))) {
        return br.readLine();
    }
}
```
* 꼭 회수해야 하는 자원을 다룰 때는 `try-with-resources`를 사용하도록 하자.
* `try-finally`에 비해 코드는 더 짧고 분명해지고 만들어지는 예외 정보도 훨씬 유용하다.
* `try-finally`로 작성하면 반드시 자원 회수 코드를 작성해주어야 하나 `try-with-resources`를 사용하면 쉽게 자원을 회수할 수 있다.
