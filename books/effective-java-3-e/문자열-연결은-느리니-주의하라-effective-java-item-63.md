# (Effective Java Item 63) Java ‐ 문자열 연결은 느리니 주의하라

* 문자열 연결 연산자(+)는 여러 문자열을 하나로 합쳐주는 편리한 수단이다.
* 허나 본격적으로 사용하기 시작하면 성능 저하를 감내하기 어렵다.
* 문자열 연결 연산자로 문자열 N개를 잇는 시간은 `N^2`에 비례한다.
* 문자열은 불변이기 때문에 두 문자열을 연결할 경우 양쪽의 내용을 모두 복사해야 하므로 성능 저하는 피할 수 없는 결과이다.
```java
// Bad
public String statement() {
    String result = "";
    for (int i=0; i<numItems(); i++) {
        result += lineForItem(i); // 문자열 연결
    }
    return result;
}
```
* 성능을 포기하고 싶지 않다면 `String` 대신 `StringBuilder`를 사용하라.

```java
public String statement2() {
    StringBuilder sb = new StringBuilder(numItems() * LINE_WIDTH); // 전체 결과를 담기에 충분한 크기로 초기화
    for (int i=0; i<numItems(); i++) {
        sb.append(lineForItem(i));
    }
    return sb.toString();
}
```
* 자바 6 이후 문자열 연결 성능을 다방면으로 개선했지만 이 두 메서드 성능 차이는 여전히 크다.
* 만약 정말 긴 값이라면 전체 결과를 담기에 충분한 크기로 초기화하는 것도 고려하자. 하지만 기본값을 사용하더라도 문제가 되지 않는다.
