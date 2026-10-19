# (Effective Java Item 5) Java - 자원을 직접 명시하지 말고 의존 객체 주입을 사용하라

```java
public class SpellChecker {

    private final Lexicon dictionary;         // final로 불변을 보장하자

    public SpellChecker(Lexicon dictionary) { // 생성자에 필요한 자원을 넘겨준다(한 번만 호출됨을 보장)
        this.dictionary = Objects.requireNonNull(dictionary);
    }
    
    public boolean isValid(String word) {...}
    public List<String> suggestions(String typo) {...}
}
```

* 사용하는 자원에 따라 동작이 달라지는 클래스는 정적 유틸리티나 싱글턴 방식이 적합하지 않다.
* "자원을 직접 명시하지 말라"는 것은 객체가 사용할 구체적인 대상(Concrete Class)을 스스로 결정하지 못하게 하라는 뜻이다.
* 의존 객체 주입 기법은 클래스의 유연성, 재사용성, 테스트 용이성을 개선해준다.
