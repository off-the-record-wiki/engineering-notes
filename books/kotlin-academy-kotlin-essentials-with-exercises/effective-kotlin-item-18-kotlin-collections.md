# (Effective Kotlin Item 18) Kotlin - Collections

#### 인터페이스 계층 구조

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/788289e2-c93f-4b5e-b6ef-493bcc283953.png)

* 계층 구조 최상위에는 원소들을 순서대로 순회할 때 이용하는 `Iterable`이 있다.
* `iterator()` 메서드가 있기 때문에 for문을 사용해 Iterable 객체를 탐색할 수 있다.
* `get()`, `contains()`, `size()`와 같이 내부 원소를 읽기만 하고 수정하지는 못하는 메서드만 제공한다.

#### 가변 타입 대 읽기 전용 타입

* 읽기 전용 리스트와 달리 가변 리스트는 내부 원소들을 변경할 수 있다.
* `toList`와 `toMutableList`를 이용하면 읽기 전용 리스트와 가변 리스트를 손쉽게 상호 변환할 수 있다.
* `mutableListOf` 함수를 사용해 컬렉션을 생성하면 `add()`, `clear()`, `remove()`같은 함수를 제공하는 `MutableList` 객체를 반환한다.

#### 컬렉션 생성

* 컬렉션 리터럴은 원소들을 나열하는 것만으로 특정 타입 컬렉션을 손쉽게 생성해 주는 특별한 구문이다.
* 코틀린에서는 `xxxOf` 형태이며, `xxx` 자리에는 생성할 컬렉션 타입이 온다.
```kotlin
fun main() {

    // listOf 함수로 List를 만든다.
    val list: List<String> = listOf("A", "B", "C")

    // mutuableListOf 함수로 MutableList를 만든다.
    val mutableList: MutableList<Int> = mutableListOf(1, 2, 3)

    // setOf 함수로 Set을 만든다.
    val set: Set<Double> = setOf(3.14, 7.11)

    // mutableSetOf 함수로 MutableSet을 만든다.
    val mutableSet: MutableSet<Char> = mutableSetOf('A', 'B')
}
```

> **컬렉션 함수형 API 활용**
> 
> * `map()`, `filter()`, `find()`, `groupBy()`, `fold()`, `reduce()`와 같은 함수들을 체이닝(Chaining)할 때는 `asSequence()`를 고려해야한다. 중간 컬렉션 생성을 막아 대용량 데이터 처리 시 성능이 비약적으로 향상된다.

#### 리스트

* 리스트는 가장 기본적인 컬렉션 타입이다. 리스트는 순서가 있는 원소들의 목록이다.
* 리스트는 원소의 위치를 뜻하는 숫자인 인덱스로 탐색할 수 있다.
* 첫 번째 원소의 인덱스는 항상 0이며, 그 다음 원소의 인덱스는 1씩 증가한다.
* 존재하지 않는 인덱스를 건네면 `IndexOutOfBoundsException`을 반환한다.
```kotlin
fun main() {
    val list = listOf("A", "B", "C")
    println(list)
```

#### 세트

* 아래와 같은 경우에 리스트 대신 세트를 사용한다.
  * 컬렉션에서 원소가 유일해야 하는 경우(O(1)의 시간 복잡도)
  * 컬렉션의 원소를 빈번하게 찾는 경우(O(1)의 시간 복잡도)
* 세트는 리스트와 달리 순서를 중요하게 여기지 않는다.
```kotlin
fun main() {
    val set = setOf('A', 'B', 'C')
    println(set)
}
```

#### 맵

* 키와 값을 연결지어 저장할 때, 맵을 사용한다.
```kotlin
class CachedApiArticleRepository(val articleApi: ArticleApi) {
    val articleCache: MutableMap<String, String> = mutableMapOf()
    
    fun getContent(key: String) = articleCache.getOrPut(key) {
        articleApi.fetchContent(key)
    }
}
```

#### 배열

* 배열(array)은 원소들이 메모리에 저장되는 형태를 거의 그대로 표현하는 아주 기본적인 자료구조이다.
* 배열은 크기가 고정되어 있으며, 사용할 수 있는 연산의 수도 제한되어 있다.
* 배열은 크기가 고정되기 때문에 허용 크기를 넘어서면 더 이상의 원소는 추가할 수 없다.
* 반면 `ArrayList`는 내부 배열이 꽉 찬 상태에서 원소가 더 들어오면 더 큰 배열을 생성하여 이전 원소들을 옮겨 담는다.
* 이런 이유로 배열보다는 리스트를 권장하며, 배열은 성능이 매우 중요한 부분에서만 제한적으로 사용되는 것이 좋다.

#### 기본 타입 배열

* `Int`와 `Char`처럼 값을 표현하는 타입은 최적화 과정에서 일반적인 객체가 아닌 더 단순한 형태로 변환되기도 한다. 이 형태를 원시 타입이라고 한다.
* 코틀린의 최적화가 값을 사용하는 데 영향을 주지 않는다. 다만, 원시 타입으로 변환하면 메모리를 더 적게 차지하며 다양한 연산을 더 효율적으로 처리할 수 있다.
* 원시 타입은 일반적인 컬렉션에 저장할 수 없는 대신 특별한 배열에 저장할 수 있다. 즉, 원시 타입으로 변환되는 값만을 위한 특수한 배열 타입들이 존재한다.
* `xxxOf` 형태의 함수에 초기 원소들을 인수로 입력한다. 예를 들어, DoubleArray를 만들고 싶다면 Double 타입 인수들을 받는 doubleArrayOf 함수를 사용한다.
* 다른 컬렉션으로 변환하려면 이름이 `toXxxArray` 형태인 메서드를 사용한다. 예를 들어, `List<Boolean>`을 BooleanArray로 변환하려면 `List<Boolean>`의 메서드인 toBooleanArray를 이용한다.

#### 기본 타입 배열 예시

* `IntArray` : 원시 타입 Int 값의 배열
* `LongArray` : 원시 타입 Long 값의 배열
* `DoubleArray` : 원시 타입 Double 값의 배열
* `FloatArray` : 원시 타입 Float 값의 배열
* `CharArray` : 원시 타입 Char 값의 배열
* `BooleanArray` : 원시 타입 Boolean 값의 배열
* `ShortArray` : 원시 타입 Short 값의 배열
* `ByteArray` : 원시 타입 Byte 값의 배열

#### vararg 인수와 배열 함수

* 고정되지 않은 수의 인수를 받으려면 매개변수에 `vararg` 제어자를 붙인다.
* 이를 길이가 변할 수 있다는 뜻에서 '가변 길이 인수(variable length argument)'이라고 부른다.
* `vararg` 제어자는 매개변수를 배열로 바꿔준다.
