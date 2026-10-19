# (Effective Java Item 43) Java ‐ 람다보다는 메서드 참조를 사용하라

* 자바에서 함수 객체를 람다보다 더 간결하게 생성하는 방법을 제공한다. 바로 메서드 참조(Method Reference)이다.
* 그러나 메서드 참조가 무조건 좋은 것은 아니다.
* 람다가 메서드 참조보다 더 길어도, 유지보수가 가독성면에서 도움이 될 수 있다.

|메서드 레퍼런스 타입|예제|람다식|
|:---:|:---:|:---:|
|정적(static)|`Integer::parseInt`|`str -> Integer.parseInt(str)`|
|바운드(bound)|`Instant.now()::isAfter`|`Instant then = Instant.now(); t -> then.isAfter(t);`|
|언바운드(unbound)|`String::toLowerCase`|`str -> str.toLowerCase();`|
|클래스 생성자|`TreeMap<K, V>::new`|`() -> new TreeMap<K, V>();`|
|배열 생성자|`int[]::new`|`len -> new int[len];`|
