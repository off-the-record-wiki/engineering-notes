# (Effective Java Item 52) Java ‐ 다중정의는 신중히 사용하라

* 다중정의(오버로딩)이란, 서로 다른 시그니처를 갖는 여러 메서드를 같은 이름으로 정의하는 것을 말한다.
* 이 때, 시그니처가 같다는 말은 메서드명, 매개변수의 개수, 타입, 순서까지 모두 같다는 것을 말한다.
* 오버로딩은 어떤 메서드가 실행될지 컴파일 시점에 정해진다.
```java
public class CollectionClassifier {
    public static String classify(Set<?> s) {
        return "집합";
    }

    public static String classify(List<?> lst) {
        return "리스트";
    }

    public static String classify(Collection<?> c) {
        return "그 외";
    }
}
```
```java
Collection<?>[] collections = {
        new HashSet<String>(),
        new ArrayList<BigInteger>(),
        new HashMap<String, String>().values()
};

for (Collection<?> c : collections) {
    System.out.println(CollectionClassifier.classify(c));
}
```
* 오버라이딩은 어떤 메서드가 실행될지가 런타임에 정해진다.
```java
public class 동물 {
    String name() {
        return "동물";
    }
}
```
```java
public class 포유류 extends 동물 {
    @Override
    String name() {
        return "포유류";
    }
}
```
```java
public class 인간 extends 포유류 {
    @Override
    String name() {
        return "인간";
    }
}
```
```java
List<동물> animals = Arrays.asList(new 동물(), new 포유류(), new 인간());

for (동물 animal : animals) {
    System.out.println(animal.name());
}
```

* 웬만하면 매개변수가 개수가 같은 오버로딩은 헷갈릴 여지가 다분하니 오버로딩은 안하는 것이 좋다.
* 매개변수가 개수가 같아야 한다면 오버로딩 대신 메서드 이름을 다르게 지어준다면 구분하기 용이해진다.
