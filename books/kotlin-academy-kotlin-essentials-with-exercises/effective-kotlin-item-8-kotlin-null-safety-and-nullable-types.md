# (Effective Kotlin Item 8) Kotlin - Null Safety and Nullable Types

#### 널 가능성

* 코틀린은 자바의 문제점을 해결할 목적으로 시작되었으며, 자바에서 가장 큰 문제는 널 가능성이다.
* `null`값에 대해 메서드를 호출하면 악명 높은 NPE가 발생한다.

#### 코틀린에서의 널 가능성은 어떻게 동작하는지?

**1. 모든 프로퍼티에 값을 할당해야 한다. 묵시적으로 null값이 할당되는 경우는 없다.**

```kotlin
var person: Person // 컴파일 에러(프로퍼티는 초기화가 되어야 한다.)
```

**2. 일반 타입은 null을 허용하지 않는다.**

```kotlin
var person: Person = null // 컴파일 에러(널 가능한 타입이 아니기 때문에 null이 될 수 없다)
```

**3. 널 가능한 타입으로 지정하려면 일반 타입 끝에 물음표(?)를 붙여야 한다.**

```kotlin
var person: Person? = null
```

**4. 널 가능한 타입을 직접 사용할 수 없다.**

```kotlin
person.name // 널이 될 수 있으므로 직접 사용할 수 없다.
```

#### 안전 호출

* 널 가능한 값의 메서드라 프로퍼티를 호출하는 가장 간단한 방법은 점(.) 대신에 물음표와 점(?.)을 함께 사용하는 안전 호출(safe call)이 있다.
* 안전 호출은 다음과 같이 작동한다.
  * 값이 `null`이면, 아무것도 하지 않고, `null`을 반환한다.
  * 값이 `null`이 아니면, 일반 호출처럼 동작한다.

```kotlin
class User(val name: String) {
    fun cheer() {
        println("Hello World!")
    }
}

var user: User? = null

fun main() {
    user?.cheer() // null을 할당했기 때문에 아무것도 하지 않는다.
    user = User("Cookie")
    user?.cheer() // Hello World!
}
```

* 안전 호출은 `null`에 대해 호출했을 때, `null`을 반환하므로 결괏값은 항상 널 가능한 타입이 된다.
* 따라서 널 가능성은 계속 전파될 수 있다.

#### 널 아님 어설션

* 값이 `null`이 아니어야 한다면, `null`일 때, 예외를 던지는 널 아님 어설션(not-null assertion)을 사용할 수 있다.
* 널 아님 어설션은 `!!`로 표현한다.
* **동작이 잘못되어 예기치 않게 `null`이 주어졌을 경우 NPE를 던지므로 이 방법은 안전하지 않다.**
  * `requireNotNull` : 널 가능한 값을 인수로 받고 값이 널이라면 `IllegalArgumentException`을 던진다. 널이 아니라면 널 가능하지 않은 값을 반환한다.
  * `checkNotNull` : 널 가능한 값을 인수로 받고 값이 널이라면 `IllegalStateException`을 던진다. 널이 아니라면 널 가능하지 않은 값을 반환한다.

```kotlin
// 절대 피해야 하는 패턴
// company, address, city 중 어떤 것이 null이라서 터졌는지 로그로 알 수 없음
val city = user!!.company!!.address!!.city 

// 권장 패턴: 안전 호출(?.) 사용 후 마지막에 엘비스 연산자(?:)로 처리
val city = user?.company?.address?.city ?: "Unknown"
```
* `!!`를 여러 번 연속해서 호출하면 NPE가 발생했을 때 정확히 어떤 객체가 `null`인지 Stack Trace에서 구별할 수 없다.
* 에러 로그가 해당 코드의 줄 번호만 알려주기 때문이다.

#### 스마트 캐스팅

* 널 가능성 여부로 스마트 캐스팅을 할 수도 있다.
* 널이 아님을 확신할 수 있는 스코프에서는 널 가능한 타입이 널 가능하지 않은 타입으로 캐스팅된다.

```kotlin
class DataManager {
    var data: String? = null

    fun process() {
        if (data != null) {
            // 컴파일 에러! (다른 스레드에서 data를 null로 바꿨을 수도 있기 때문)
            // println(data.length) 
            
            // 해결법: 지역 변수(val)로 캡처하여 복사한 뒤 스마트 캐스팅 활용
            val capturedData = data
            if (capturedData != null) {
                println(capturedData.length) // 정상 동작
            }
        }
    }
}
```
* 널 여부를 체크하면 컴파일러가 자동으로 널 불가능 타입으로 바꿔주는 스마트 캐스팅은 매우 편리하지만 항상 동작하는 것은 아니다.
* 스마트 캐스팅은 '체크 시점'과 '사용 시점' 사이에 값이 절대 바뀌지 않는다고 확신할 때만 동작한다. 클래스의 변경 가능한 프로퍼티(var)나 커스텀 게터(get)가 있는 val은 다른 쓰레드에서 값을 변경할 위험이 있으므로 스마트 캐스팅이 거부된다.

#### 엘비스 연산자

* 널 가능성 처리에 사용되는 마지막 기능은 바로 엘비스 연산자이다.
* 엘비스 연산자는 두 값 사이에 위치하는데, 왼쪽 값이 `null`이 아니라면 연산 결과는 왼쪽 널 가능한 값 그대로이다. 왼쪽 값이 `null`이라면 오른쪽 값이 반환된다.

```kotlin
fun main() {
    println("A" ?: "B")   // A
    println(null ?: "B")  // B
    println("A" ?: null)  // A
    println(null ?: null) // null
}
```

* 코틀린에서는 `return`과 `throw`도 하나의 식으로 취급되므로 엘비스 연산자 우측에 배치해 `null`인 경우 로직을 즉시 종료시킬 수 있다.
* 중첩된 if문을 획기적으로 줄여줄 수 있다.

```kotlin
fun processUser(user: User?) {
    // user가 널이면 함수 즉시 종료
    val validUser = user ?: return 
    
    // name이 널이면 예외 발생 (스프링 백엔드 로직에서 예외 던질 때 매우 유용)
    val userName = validUser.name ?: throw IllegalArgumentException("이름이 없습니다.")
    
    // 이 시점부터 validUser와 userName은 절대 Null이 아님이 보장됨
    println(userName)
}
```

#### 널 가능한 타입의 확장 함수

* 널 가능한 변수에서 일반 함수를 호출할 수는 없다.
* 하지만 널 가능한 변수에서 호출할 수 있는 특별한 함수를 정의할 수 있다.
* 대표적인 예시로 코틀린 표준 라이브러리에는 `String?`에서 호출할 수 있는 다음 함수들이 마련되어 있다.
  * `orEmpty` : 값이 `null`이 아니면 값을 그대로 반환한다. 그렇지 않다면 빈 문자열을 반환한다.
  * `isNullOrEmpty` : 값이 `null`이거나 비어있는 문자열이면 true를 반환한다. 그렇지 않다면 false를 반환한다.
  * `isNullOrBlack` : 값이 `null`이거나 공백이라면 true를 반환한다. 그렇지 않다면 false를 반환한다.
* 위와 같이 String?에서 호출할 수 있는 함수들도 있고 널 가능한 리스트용으로도 비슷한 함수들을 제공한다.
  * `orEmpty` : 값이 `null`이 아니면 값을 그대로 반환한다. 그렇지 않다면 빈 리스트를 반환한다.
  * `isNullOrEmpty` : 값이 `null`이거나 비어 있는 리스트라면 true를 반환한다.

#### 지연 초기화(lateinit)

* 프로퍼티를 널이 불가능한 타입으로 정의하고 싶지만 객체를 생성할 시점에는 값을 명시하지 못하는 상황이 있다.
* 지연초기화 프로퍼티는 널이 가능하지 않은 타입이지만 인스턴스 생성 과정에서는 초기화할 수 없다.
* 지연 초기화 프로퍼티를 사용할 때는 처음 사용하기 전에 반드시 값을 할당해야 한다.
* 할당하지 않으면 런타임에 `UninitializedPropertyAccessException`을 던진다.
* 코틀린은 지연 초기화로 선언한 프로퍼티가 초기화되었는지 확인하는 기능도 제공한다. 바로 `isInitialized` 프로퍼티이다.
* 2개의 콜론(`::`)으로 시작한 다음, 초기화 여부를 확인하려는 프로퍼티 이름을 적어주면 된다.

```kotlin
lateinit var text: String

private fun printIfInitialized() {
    if (::text.isInitialized) {
        println(text)
    } else {
        println("Not initialized")
    }
}
```

* `lateinit`은 값이 나중에 주입될 `var(가변 변수)`에 사용한다. 반면 `lazy`는 처음 접근할 때 초기화 블록이 실행되는 `val(불변 변수)`에 사용한다.
