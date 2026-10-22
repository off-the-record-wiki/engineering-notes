# (Effective Java Item 6) Java ‐ 불필요한 객체 생성을 피하라

* 똑같은 기능의 객체를 매번 생성하기보다 객체 하나를 재사용하는 편이 나을 때가 있다.
* 재사용은 빠르고 세련되다. 불변 객체의 경우 언제든 재사용할 수 있다.

```java
// Bad
String s = new String("test"); 
```
```java
// Good
String s = "test";
```
* 새로운 인스턴스를 매번 만드는 대신 하나의 String 인스턴스를 사용한다.
* 이 방식을 사용하면 같은 가상 머신 안에서 똑같은 문자열 리터럴을 사용하는 모든 코드가 같은 객체를 재사용함을 보장한다.
* 생성자 대신 정적 팩토리 메서드를 제공하는 불변 클래스에서는 정적 팩토리 메서드를 사용해 불필요한 객체 생성을 피할 수 있다.
* 생성자는 호출할 때마다 새로운 객체를 만들지만, 팩터리 메서드는 전혀 그렇지 않다.
* 생성 비용이 아주 비싼 객체도 있는데 이런 비싼 객체의 경우 캐싱하여 재사용하길 권한다.
```java
// Bad
static boolean isRomanNumberal(String s) {
    return s.matches();
}
```
```java
// Good
// 값비싼 객체를 재사용해 성능을 개선한다.
public class RomanNumerals {
    private static final Pattern ROMAN = Pattern.compile(); // static으로 선언해 클래스 영역에 단 하나만 생성됨을 보장하도록

    static boolean isRomanNumeral(String s) {
        return ROMAN.matcher(s).matches();
    }
}

// Ex
RomanNumerals.isRomanNumeral("test");
```
* `String.matches` 메서드는 정규표현식으로 문자열 형태를 확인하는 가장 쉬운 방법이지만, 성능이 중요한 상황에서 반복해 사용하기엔 적합하지 않다.
* 이 메서드가 내부에서 만드는 정규표현식용 `Pattern` 타입의 인스턴스는 한 번 쓰고 버려져서 곧바로 GC 대상이 된다.
* `Pattern`은 입력받은 정규표현식에 해당하는 유한 상태 머신을 만들기 때문에 인스턴스 생성 비용이 높다.
* 성능을 개선하려면 필요한 정규표현식을 표현하는 `Pattern` 인스턴스 클래스 초기화 과정에서 직접 캐싱하고 나중에 메서드가 호출될 때마다 인스턴스를 재사용하는 것이 좋다.

#### 오토 박싱과 오토 언박싱 개념

* 오토 박싱(Auto-Boxing) : 자바 컴파일러가 기본 타입(Primitive Type)을 그에 대응하는 참조 타입(Reference Type) 객체로 자동 변환해주는 과정이다.
* 오토 언박싱(Auto-UnBoxing) : 반대로 참조 타입(Reference Type)을 다시 기본 타입(Primitive Type)으로 자동 변환해주는 과정이다.
* 해당 기능은 자바 5부터 도입되었으며, 개발자가 명시적으로 변환 코드를 작성하지 않아도 컬렉션에 기본 타입을 담을 수 있게 해준다.

#### 오토 박싱과 오토 언박싱 - 왜 주의해서 사용해야 하는가?(성능과 예외)

```java
private static long sum() {
    Long sum = 0L;
    for (long i = 0; i <= Integer.MAX_VALUE; i++)
        sum += i;
    return sum;
}
```
* 박싱된 기본 타입보다 기본 타입을 사용하고, 의도치 않은 오토 박싱이 숨어들지 않도록 주의해야 한다.
* 기본 타입은 스택 메모리에 값이 바로 저징되지만, 래퍼 클래스는 힙 영역에 객체를 생성한다. 그렇기 때문에 불필요한 객체가 수만 개 생성되어 GC 부하를 유발한다.
* 추가로 기본 타입은 `null`을 가질 수 없지만, 참조 타입은 null`이 가능하다. `null`인 래퍼 객체를 오토 언박싱해서 기본 타입 변수에 할당하려고 하면 NPE가 발생하기 때문에 주의해야 한다.
