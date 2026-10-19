# (Effective Kotlin Item 1) Kotlin - 가변성을 제한하라

#### 가변성을 제한하라

```kotlin
// ❌ Bad
class BankAccount {
    var balance = 0.0
        private set   // 외부에서는 읽기만, 변경은 클래스 내부에서만

    fun deposit(depositAmount: Double) {
        balance += depositAmount
    }

    @Throws(InsufficientFunds::class)
    fun withdraw(withdrawAmount: Double) {
        if (balance < withdrawAmount) {
            throw InsufficientFunds()
        }
        balance -= withdrawAmount
    }
}

class InsufficientFunds : Exception()
```

* 상태를 가지는 것은 양날의 검이다.
* 시간에 따라 변화하는 요소를 표현할 수 있는 것은 장점이나 반면 다음과 같은 이유 때문에 관리하기가 어렵다는 단점이 뒤따른다.

#### 1. 변경 지점(mutating point)이 많은 프로그램은 이해하고 디버깅하기 어렵다.

* 가변 대상들 사이의 관계를 이해해야 하고, 변경이 많이 일어날수록 그 변화를 추적하기가 어려워진다.
* 특히 서로 의존하는 변경 지점이 많은 클래스는 이해하고 수정하기가 어려워진다.

#### 2. 가변성은 코드를 추론하기 어렵게 만든다.

* 불변 요소의 상태는 명확하지만 가변 상태는 훨씬 이해하기 어렵다.
* 언제든지 변경될 수 있으므로 값이 무엇인지 추론하기가 더 어렵다.

#### 3. 가변 상태는 멀티쓰레드 프로그램에서 적절히 동기화가 되어야 한다.

* 모든 변화는 잠재적으로 충돌을 일으킬 수 있다.

#### 4. 가변 요소는 테스트를 하기가 어렵다.

* 가능한 한 모든 상태를 테스트해야 하는데, 가변성이 높을수록 확인해야 할 상태가 많아진다.
* 한 객체나 한 파일 안에서 가능한 상태들의 모든 조합을 고려해야 하므로 변경 지점들의 수에 따라 테스트해야 할 상태의 수는 기하급수적으로 증가한다.

#### 5. 상태가 변경되면 다른 클래스에 이 변경 사항을 알려야 하는 경우가 많다.

* 예를 들어, 정렬된 리스트에 가변 요소를 추가하면 이 요소가 변경될 때마다 리스트를 다시 정렬해야 한다.

> 가변 상태는 실제 시스템의 상태를 나타내기에 매우 유용한 방법이지만 가능하면, 가변성을 제한하여 정말 합당한 이유가 있을 때만 사용하길 권한다.

#### 코틀린에서 가변성 제한하기

* 코틀린은 가변성을 제한할 수 있도록 설계되어 있다.
* 불변 객체를 만들거나 프로퍼티를 변경할 수 없게 만드는 것은 쉽다.
* 코틀린의 많은 기능과 특징 덕분인데, 이 중에서 가장 중요한 것들은 다음과 같다.
  * **읽기 전용 프로퍼티 val**
  * **가변 컬렉션과 읽기 전용 컬렉션의 구분**
  * **데이터 클래스의 copy**

#### 읽기 전용 프로퍼티 val

* 코틀린에서는 프로퍼티를 읽기 전용 `val`이나 읽기/쓰기가 가능한 `var`로 만들 수 있다.
* 읽기 전용 `val` 프로퍼티의 경우에는 값을 새로 설정할 수가 없다.

#### 가변 컬렉션과 읽기 전용 컬렉션의 구분

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/86ca70f2-61d9-4d12-a4d1-977e0e13222f.png)

* `Iterable`, `Collection`, `Set`, `List` 인터페이스들은 모두 읽기 전용으로 변경을 허용하는 메서드가 아예 없다.
* `MutableIterable`, `MutableCollection`, `MutableSet`, `MutableList` 인터페이스들은 모두 가변 컬렉션으로 변경을 허용하는 메서드를 추가한다.
* 코틀린에서 읽기 전용 컬렉션을 가변 컬렉션으로 다운캐스팅하면 절대로 안 된다.
* 만약 읽기 전용 컬렉션을 가변 컬렉션으로 바꿔야 한다면 수정할 수 있는 복사본을 생성하는 `List.toMutableList` 함수를 사용해야 한다.
* 컬렉션 다운캐스팅은 규약 위반일 뿐만 아니라 추상화가 아닌 구현에 의존하게 된다. 이는 안전하지 않을 뿐만 아니라 예상치 못한 결과로 이어질 수 있다.

```kotlin
// ❌ Bad
val list = listOf(1, 2, 3)

if (list is MutableList) {
    list.add(4)
}
```

* 위와 같이 코틀린에서는 읽기 전용 컬렉션을 가변 컬렉션으로 다운캐스팅하면 절대로 안 된다.
* 읽기 전용 컬렉션을 가변 컬렉션으로 바꿔야 한다면 수정할 수 있는 복사본을 생성하는 `List.toMutableList` 함수를 사용해야한다.

```kotlin
// ⭕ Good
val list = listOf(1, 2, 3)

val mutableList = list.toMutableList()
mutableList.add(4)
```

#### 데이터 클래스의 copy

#### 1. 한 번 생성되면 상태가 동일하게 유지되므로 상태를 추론하기가 더 쉽다.

#### 2. 공유되는 객체들 간의 충돌이 발생하지 않으므로 프로그램을 병렬화하기 더 쉽다.

#### 3. 불변 객체에 대한 참조는 변경되지 않을 것이므로 캐싱될 수 있다.

#### 4. 불변 객체에 대해 방어적 복사본을 만들 필요가 없다. 불변 객체를 복사할 때는 굳이 깊은 복사를 할 필요가 없다.

#### 5. 변경이 허용되는 위치를 결정할 수 있으며, 불변 객체에서 작업하는 것이 더 쉽다.

#### 6. 불변 객체를 Set에 추가하거나 Map의 키로 사용할 수 있다.

* 프로퍼티마다 복사본을 생성하는 함수를 만들 수는 있지만, 모든 프로퍼티에 적용해야 한다면 귀찮은 작업이 될 수 있다.
* 그럴 때는 `data` 한정자를 사용할 수 있다.
* `data` 한정자는 `copy` 메서드를 사용할 수 있다.
* `copy` 메서드는 모든 기본 생성자의 프로퍼티가 이전 인스턴스와 동일한 새로운 인스턴스를 생성한다.

```kotlin
// ⭕ Good
data class User(
    val name: String,
    val surname: String
)

var user = User("Maja", "Markiewicz")
user = user.copy(surname = "Moskala")
print(user)
```

#### 다른 종류의 변경 지점

* 변경할 수 있는 리스트를 나타내야 한다고 가정할 때, 가변 컬렉션을 사용하거나 읽기-쓰기가 가능한 var 프로퍼티를 사용하는 방법이 있다.
```kotlin
val list1 : MutableList<Int> = mutableListOf()
var list2 : List<Int> = listOf()
```
```kotlin
list1.add(1)
list2 = list2 + 1
```
* 두 방식 모두 `+=` 연산자로 대체해서 사용할 수 있지만 각각에 대해 다르게 처리된다.

```kotlin
// ❌ Bad
val list1 : MutableList<Int> = mutableListOf()
list1.add(1)
```
* 이 방식은 구체적인 리스트 구현체 내에서 변경이 발생한다.
* 멀티쓰레드를 사용한다면 컬렉션이 적절한 동기화 기법을 가지고 있어야 정상적으로 동작한다.

```kotlin
// ⭕ Good
var list2 : List<Int> = listOf()
list2 = list2 + 1
```
* 이 방식은 동기화를 직접 구현해야 한다.
* 하지만 변경 지점이 프로퍼티 하나이므로 안정성 측면에서 조금 더 낫다고 볼 수 있다.
* 그러나 적절한 동기화 처리가 누락되면 여전히 일부 요소가 손실될 수 있다.

```kotlin
// ❌ Bad
var list3 = mutableListOf<Int>()
```

* `var` + `MutableList`라 변경 지점이 두 곳이 된다. 둘 다 동기화를 신경써야하고 어디서 바뀌는지 추적도 어려워 가장 나쁜 조합으로 불린다.
