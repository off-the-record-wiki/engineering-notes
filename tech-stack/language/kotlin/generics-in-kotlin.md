# Kotlin - Generics in Kotlin

## 📚 제네릭과 타입 파라미터

* Classes in Kotlin can have type parameters, just like in Java:
```kotlin
class Box<T>(t: T) {
    var value = t
}
```
* To create an instance of such a class, simply provide the type arguments:
```kotlin
val box: Box<Int> = Box<Int>(1)
```
* One of the trickiest aspects of Java's type system is the wildcard types (see [Java Generics FAQ](http://www.angelikalanger.com/GenericsFAQ/JavaGenericsFAQ.html)). Kotlin doesn't have these. Instead, Kotlin has declaration-site variance and type projections.

### Covariance(공변성) 사용

```kotlin
class Cage<T> {

    private val animals: MutableList<T> = mutableListOf()

    fun getFirst(): T {
        return animals.first()
    }

    fun put(animal: T) {
        animals.add(animal)
    }

    // out 키워드로 T의 하위 타입도 받을 수 있게 함
    fun moveFrom(cage: Cage<out T>) {
        this.animals.addAll(cage.animals)
    }
}

fun main() {
    val fishCage = Cage<Fish>()
    val goldFishCage = Cage<GoldFish>()
    
    goldFishCage.put(GoldFish("금붕어1"))
    goldFishCage.put(GoldFish("금붕어2"))
    
    fishCage.moveFrom(goldFishCage)  // ✅ 이제 가능!
    println(fishCage.getFirst().name)  // "금붕어1"
}
```

### Contravariance(반공변성) 사용

```kotlin
fun moveTo(cage: Cage<in T>) {
    cage.animals.addAll(this.animals)
}
```

### 공변성/반공변성 요약

|키워드|의미|사용 시기|
|:---:|:---:|:---:|
|out T|공변성(생산자, Producer)|T를 반환만 하는 경우(읽기 작업)|
|in T|반공변성(소비자, Consumer)|T를 매개변수로만 받는 경우(쓰기 작업)|

### 선언 지점 변성

* 선언 지점 변성은 제네릭 타입을 정의할 때 클래스/인터페이스 선언부에 out 또는 in을 붙여 그 타입 파라미터의 공변/반공변 성질을 고정하는 것이다.
* 이렇게 하면 그 타입을 사용하는 모든 곳에서 더 안전하고 유연하게 취급된다.

```kotlin
// Producer는 T를 반환만 하므로 선언 지점에 out을 붙일 수 있다
interface Producer<out T> {
    fun produce(): T
}

// Consumer는 T를 매개변수로만 받으므로 in을 붙일 수 있다
interface Consumer<in T> {
    fun consume(item: T)
}

// 사용 예
class GoldFishProducer : Producer<GoldFish> {
    override fun produce(): GoldFish = GoldFish("금붕어")
}

fun declarationSiteExample() {
    val goldProducer: Producer<GoldFish> = GoldFishProducer()
    // Producer<GoldFish>는 Producer<Fish>로 취급될 수 있음 (out 덕분)
    val fishProducer: Producer<Fish> = goldProducer

    // 반대로 Consumer 예
    val fishConsumer: Consumer<Fish> = object: Consumer<Fish> {
        override fun consume(item: Fish) { println(item.name) }
    }
    // Consumer<Fish>는 Consumer<GoldFish>로도 사용 가능 (in 덕분)
    val goldConsumer: Consumer<GoldFish> = fishConsumer
}
```

### 사용 지점 변성

* 사용 지점 변성(use-site variance)은 "타입을 사용하는 자리(보통 함수 시그니처)에서만 variance를 적용"하는 것이다.

```kotlin
// 간단한 use-site variance 예제 (Kotlin)
// - moveAll: use-site out (읽기 전용 소스)
// - copyAll: use-site out/in (from 읽기, to 쓰기)
// - printUnknown: star projection (List<*>)
// 컴파일 및 실행 가능 (Kotlin 1.3+)

abstract class Animal(val name: String) {
    override fun toString() = "${this::class.simpleName}($name)"
}
open class Fish(name: String) : Animal(name)
class GoldFish(name: String) : Fish(name)
class Carp(name: String) : Fish(name)

class Cage<T> {
    private val animals: MutableList<T> = mutableListOf()

    fun put(animal: T) { animals.add(animal) }
    fun getAll(): List<T> = animals.toList()
    override fun toString(): String = animals.joinToString(", ")
}

// use-site out: from은 읽기 전용으로 취급 -> 하위 타입도 전달 가능
fun <T> moveAll(to: Cage<T>, from: Cage<out T>) {
    // from은 use-site projection으로 인해 읽기만 안전하므로 getAll()로 읽어서 to에 넣을 수 있다.
    for (item in from.getAll()) {
        to.put(item)
    }

    // 주의: 아래처럼 from.put(...) 시도하면 컴파일 에러 발생
    // from.put(...) // Error: out-projected type 'Cage<out T>' prohibits the use of 'fun put(T): Unit'
}

// MutableList에서 자주 쓰이는 형태: "producer"는 out, "consumer"는 in
fun <T> copyAll(from: MutableList<out T>, to: MutableList<in T>) {
    for (item in from) {
        to.add(item) // 안전: from의 요소는 T 또는 T의 하위타입이므로 to에 넣어도 안전
    }
    // from.add(...) 는 불가 (out-projected)
    // to.get(...) 는 불가 (in-projected)은 반환 타입으로 사용 불가
}

// star projection: 타입을 모를 때 읽기는 가능, 쓰기는 금지
fun printUnknown(list: List<*>) {
    for (item in list) {
        println(item) // Any? 타입으로 읽음
    }
    // list.add(...) // Error: List<*>는 읽기 전용으로만 안전
}

fun main() {
    val fishCage = Cage<Fish>()
    val goldCage = Cage<GoldFish>()

    goldCage.put(GoldFish("금붕어1"))
    goldCage.put(GoldFish("금붕어2"))

    // use-site out 덕분에 Cage<GoldFish>를 Cage<out Fish>로 취급하여 이동 가능
    moveAll(fishCage, goldCage)
    println("fishCage after moveAll: ${fishCage.getAll()}") // 금붕어들이 Fish 케이지에 들어감

    // MutableList 예제
    val animals: MutableList<Animal> = mutableListOf()
    val golds: MutableList<GoldFish> = mutableListOf(GoldFish("금붕어3"))
    copyAll(golds, animals) // from: MutableList<out T> (golds), to: MutableList<in T> (animals)
    println("animals after copyAll: $animals")

    // 반대 순서는 타입 유추/안전성에 맞지 않아 컴파일 에러가 된다(주석 처리)
    // copyAll(animals, golds) // Error: 타입 불일치

    // star projection 예제
    val mixed: List<Any> = listOf("str", 123, GoldFish("금붕어4"))
    printUnknown(mixed) // List<*>로 받아 읽기만 안전하게 출력
}
```

### 타입 소거와 Star Projection

* 타입 소거는 JDK 호환성을 위해 런타임 때 제네릭 클래스의 타입 파라미터 정보가 지워지는 것을 말한다.
* Kotlin에서는 inline 함수와 reified 키워드를 이용해 타입 소거를 일부 막을 수 있다.
* Star Projection이란, 어떤 타입이건 들어갈 수 있다는 의미이다.
> Kotlin Documentation
> * Star projection, denoted by the * character. For example, in the type KClass<*>, * is the star projection. See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/generics.html#star-projections) for more information.
```kotlin
// 스타 프로젝션을 사용하면 어떤 타입의 리스트라도 인자로 받을 수 있습니다.
fun printAnyList(items: List<*>) {
    println("리스트의 첫 번째 요소: ${items.firstOrNull()}")
    println("리스트의 모든 요소: $items")
    
    // items.add("New Element") // 컴파일 에러: 요소를 추가할 수 없습니다.
}

fun main() {
    val stringList: List<String> = listOf("Kotlin", "is", "awesome")
    val intList: List<Int> = listOf(1, 2, 3)
    
    printAnyList(stringList) // 어떤 타입이든 허용
    printAnyList(intList)    // 어떤 타입이든 허용
}
```
```kotlin
// inline과 reified를 사용하면 런타임에 타입 정보를 알 수 있습니다.
inline fun <reified T> checkTypeWithReified(items: List<Any>) {
    // 이제 is T가 정상적으로 작동합니다.
    val filteredList = items.filter { it is T } 
    println("Filtered List: $filteredList")
}

fun main() {
    val items = listOf("Apple", 1, "Banana", 2.0, "Cherry")
    
    println("Checking for String type:")
    checkTypeWithReified<String>(items) // 출력: Filtered List: [Apple, Banana, Cherry]
    
    println("\nChecking for Int type:")
    checkTypeWithReified<Int>(items)     // 출력: Filtered List: [1]
}
```
