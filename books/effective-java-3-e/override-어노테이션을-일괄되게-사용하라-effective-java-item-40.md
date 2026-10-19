# (Effective Java Item 40) Java ‐ @Override 어노테이션을 일괄되게 사용하라

* `@Override`는 메서드 선언에만 달 수 있으며, 이 어노테이션이 달려 있다는 것은 상위 타입의 메서드를 재정의했음을 뜻한다.
* 상위 클래스의 메서드를 재정의하려는 모든 메서드에 `@Override` 어노테이션을 달자.
* 구체 클래스에서 상위 클래스의 추상 메서드를 재정의할 때는 굳이 `@Override`를 달아두지 않아도 된다.
* 구체 클래스인데 아직 구현하지 않은 추상 메서드가 남아있다면 컴파일 과정에서 문제를 알려주기 때문이다. 허나, 재정의 메서드 모두에 `@Override`를 일괄로 붙여두는 것이 좋아보인다면 그래도 상관없다.

```java
@Override
public boolean equals(Bigram bigram) {
    return bigram.first == this.first &&
        bigram.second == this.second;
}
```
