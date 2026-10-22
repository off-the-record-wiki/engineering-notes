# (Effective Kotlin Item 10) Kotlin - Extend

#### 구성 요소 오버라이딩

* 코틀린에서 기본적으로 서브 클래스는 슈퍼 클래스에 정의된 구성 요소를 오버라이딩 할 수 없다.
* 코틀린의 모든 요소는 기본적으로 닫혀 있기 때문에 오버라이딩하려면 `open` 제어자를 사용해 명시적으로 허용해야 한다.
* 그리고 서브 클래스에서 오버라이딩하는 요소에 대해 `override` 제어자를 추가해야 한다.

```kotlin
open class Mammal {           // open으로 명시
    val canFeed = false

    open fun feedYoung() {
        if (canFeed) {
            println("Feeding young with mulk")
        }
    }
}

class Cat : Mammal() {
    
    override fun feedYoung() {  // override 제어자 사용
        if (canFeed) {
            // ...
        } else {
            // ...
        }
    }
}
```

#### 비어 있지 않은 생성자가 있는 부모

* 슈퍼 클래스 생성자가 매개변수를 받으면 괄호 안에 적절한 인수를 추가하여 호출해야 한다.

```kotlin
open class Animal(val name: String)
class Dodo : Animal("Dodo")
```

#### super 호출

* 클래스를 상속할 때 슈퍼클래스의 동작을 계승하면서 서브클래스에 특화된 동작을 추가할 수 있다.

```kotlin
open class Dog {
    open fun seeFriend() {
        println("Wave its tall")
    }
}

class BorderCollie : Dog() {
    override fun seeFriend() {
        println("Lie down")
        super.seeFriend()
    }
}

fun main() {
    val dog = Dog()
    dog.seeFriend()
    var borderCollie = BorderCollie()
    borderCollie.seeFriend()
}
```

#### 추상 클래스
 
* 다른 클래스들의 슈퍼 클래스로만 이용하고 객체를 생성할 수 없는 클래스를 정의할 때는 클래스 정의 앞에 `abstract` 키워드를 붙인다.
* `open` 제어자는 이 클래스로부터 상속이 가능하다는 의미지만, `abstract`는 이 클래스를 사용하려면 반드시 상속해야 한다는 의미를 가진다.

```kotlin
abstract class Mammal {
    val haveHairOrFur = true
    val warmBlooded = true
    val canFeed = false

    fun feedYoung() {
        if (canFeed) {
            println("Feeding young with milk")
        }
    }
}
```

#### 인터페이스

* 인터페이스는 클래스가 제공해야 할 프로퍼티와 메서드를 정의한다.
* 인터페이스는 `interface` 키워드와 이름, 그리고 구현해야 할 프로퍼티와 메서드로 이루어진 본문으로 정의한다.
* 인터페이스에는 생성자가 없으므로 생성자를 호출하진 않는다.
* 읽기만 가능한 `val` 프로퍼티는 쓰기도 가능한 `var` 프로퍼티로 오버라이딩할 수 있다.
* `val` 프로퍼티에는 `Getter`만 필요하지만 `var` 프로퍼티는 `Getter/Setter` 모두 있어서 포괄적이기 때문에 가능한 것이다.
* 추가로 인터페이스는 메서드를 디폴트 메서드(default method)로 지정할 수 있다.
* 디폴트 메서드는 본문을 가지고 있어서 서브 클래스가 구현하지 않아도 된다. 만약 기본 로직을 변경해야 한다면 오버라이딩을 하면 된다.

```kotlin
interface CoffeeMaker {
    val type: String
    fun makeCoffee(size: Size): Coffee
}
```

> **인터페이스(interface)와 추상 클래스(abstract class)의 결정적 차이는 상태에 있다.**
>
> * 인터페이스에 디폴트 메서드가 추가되면서 추상 클래스와의 차이가 모호해 보일 수 있으나 코틀린에서 2가지를 구분하는 중요한 기준은 바로 상태의 저장 여부에 있다.
> * 추상 클래스는 내부에 데이터를 저장할 수 있는 상태를 가질 수 있지만, 인터페이스는 상태를 저장할 수 없다.

#### 가시성

* 클래스는 최소한의 정보만 노출하도록 설계되어야 한다.
* 공개할 이유가 없다면 숨기는 것이 맞다.
* 제한이 덜한 가시성 타입을 사용할 합당한 이유가 없다면 클래스와 요소의 가시성을 최대한 제한해야 한다. 코틀린에서는 가시성 제어자를 사용하여 이 원칙을 설계에 투영한다.

|제어자|특징|
|:----:|:----:|
|public(기본)|선언된 클래스를 볼 수 있는 클라이언트 전부가 볼 수 있다.|
|private|클래스 내부에서만 볼 수 있다.|
|protected|클래스와 서브 클래스에서 볼 수 있다.|
|internal|선언된 클래스를 볼 수 있는 클라이언트를 포함하는 모듈 내부에서 볼 수 있다.|

#### Any

* 코틀린의 클래스 계층구조 최상위에는 `Any` 클래스가 있다.
* 슈퍼클래스를 명시하지 않은 모든 클래스의 슈퍼 클래스는 암묵적으로 `Any`가 되며, 매개변수를 `Any?`으로 선언하면 어떠한 객체라도 인수로 받겠다는 뜻이다.
* `Any`에 정의된 메서드들은 오픈되어 있는 디폴트 메서드이므로 필요하다면 오버라이딩할 수 있다.

```kotlin
fun consumeAnything(a: Any?) {
    println("Om nom $a")
}
```

> **Any 클래스 3대 규약**
>
> * `Any` 클래스에는 `equals()`, `hashCode()`, `toString()` 3가지 메서드가 정의되어 있다. 이 메서드들을 오버라이딩할 때는 반드시 지켜야 하는 논리적 규약이 있다.
> * 이를 어기면 `Set`이나 `Map`같은 컬렉션에서 객체를 잃어버리는 치명적인 버그가 발생한다.
