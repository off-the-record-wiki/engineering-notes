# (Effective Kotlin Item 56) Kotlin - groupBy 대신 groupingBy 사용을 고려하라

#### groupBy 대신 groupingBy 사용을 고려하라

* 컬렉션 처리에서 그룹화가 필요한 경우는 다음과 같다.
  * 사용자 목록을 기반으로 도시의 사용자 수를 계산할 때
  * 플레이어 목록을 기반으로 각 팀이 받은 점수를 찾을 때
  * 옵션 목록을 기반으로 각 카테고리에서 가장 적합한 옵션을 찾을 때
* 반복 가능한 요소를 그룹화하는 방법에는 2가지가 있다. 바로 `groupBy()`와 `groupingBy()`가 있다.

#### groupBy

* 그룹화하는 가장 쉬운 방법은 `groupBy()` 함수를 사용해 `Map<K, List<V>>`를 반환하는 것이다.
* 여기서 `V`는 원본 컬렉션의 요소 유형이고, `K`는 매핑하려는 유형이다.
* 따라서 문자열 타입의 ID로 사용자 목록을 그룹화하면 `Map<String, List<User>>`를 반환하게 된다.

```kotlin
// ⭕ Good
val byCategory: Map<String, List<Configuration>> = configurations.groupBy { it.category }
```

#### groupingBy

* 반면 코드에서 성능이 중요한 부분을 처리하는 경우에는 `groupBy()`가 최선의 선택이 아니다.
* 그룹 크기를 미리 알 수 없을 경우 시간이 더 걸리는데 이런 경우 `groupBy()` 대신 추가 작업을 수행하지 않는 `groupingBy()` 함수를 사용할 수 있다.
* 반복 가능한 객체를 리시버로 하고 키 선택기를 인수로 제공하기만 하면 된다.

```kotlin
// ⭕ Good
orders.groupingBy { it.category }.fold(0L) { acc, order -> acc + order.amount }
```
