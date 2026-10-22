# (Effective Kotlin Item 4) Kotlin - primitive types, literals, and operations

* 코틀린에서는 기본적으로 모든 값이 객체로 간주되기 때문에(=원시 타입이 없다) 모든 값은 메서드를 제공하며, 값의 타입을 제네릭 타입 인수로 사용할 수 있다.
* 자바와 달리 코틀린은 원시 타입(primitive type)과 래퍼 타입(wrapper type)을 구분하지 않는다. 하지만 내부적으로는 성능 최적화를 위해 가능한 경우 JVM의 원시 타입으로 컴파일된다.

#### 수 (Numbers)

* 코틀린에는 수를 표현할 때 사용하는 타입이 다양하게 준비되어 있다. 
* 크게 소수점이 없는 정수 타입과 소수점이 있는 부동소수점 수 타입으로 구분할 수 있다.

#### 정수 타입

* 정수를 표현할 때는 `Int`, `Long`, `Byte`, `Short`를 사용한다.

| 타입 | 크기 (비트) | 최솟값 | 최댓값 |
|------|-------------|--------|--------|
| Byte | 8 | -128 | 127 |
| Short | 16 | -32,768 | 32,767 |
| Int | 32 | -2,147,483,648 | 2,147,483,647 |
| Long | 64 | -9,223,372,036,854,775,808 | 9,223,372,036,854,775,807 |

```kotlin
// 정수 리터럴
val decimal = 100           // Int (기본)
val long = 100L             // Long (L 접미사)
val byte: Byte = 127        // Byte (명시적 타입 지정)
val short: Short = 1000     // Short (명시적 타입 지정)

// 코드에서 소수점이 없는 수는 기본적으로 Int로 간주
val defaultInt = 42         // 타입 추론: Int
val explicitLong = 42L      // 타입 추론: Long
```

#### 부동소수점 타입

* 부동소수점 수를 표현할 때는 `Float`, `Double`을 사용한다.

| 타입 | 크기 (비트) | 유효 자릿수 |
|------|-------------|------------|
| Float | 32 | 6-7 자리 |
| Double | 64 | 15-16 자리 |

```kotlin
// 부동소수점 리터럴
val double = 3.14                    // Double (기본)
val explicitDouble = 3.14159265358979 // Double
val float = 3.14f                    // Float (f 또는 F 접미사 필요)
val floatUpper = 3.14F               // Float

// 코드에서 소수점이 있는 수는 기본적으로 Double로 간주
val defaultDouble = 2.71828          // 타입 추론: Double
val scientificNotation = 1.23e10     // 과학적 표기법: 1.23 × 10^10
val negativeExponent = 1.5e-5        // 0.000015
```

#### 수에서 밑줄 사용

* 수 리터럴에서 자릿수 사이에 밑줄(`_`)을 넣을 수 있다. 
* 밑줄은 무시되지만 큰 수의 가독성을 높이고자 할 때 사용된다.

```kotlin
// 큰 수의 가독성 향상
val oneMillion = 1_000_000
val billion = 1_000_000_000L

// 신용카드 번호처럼 그룹화
val creditCardNumber = 1234_5678_9012_3456L

// 이진수에서도 사용 가능
val bytes = 0b11010010_01101001_10010100_10010010

// 16진수에서도 사용 가능
val hexBytes = 0xFF_EC_DE_5E

// 부동소수점에서도 사용 가능
val pi = 3.141_592_653_589_793
```

#### 다양한 진법 표기

* 코틀린은 10진수 외에도 2진수, 16진수 리터럴을 지원한다.

```kotlin
// 10진수 (Decimal)
val decimal = 255

// 16진수 (Hexadecimal) - 0x 또는 0X 접두사
val hexadecimal = 0xFF
val hexUpper = 0XFF
println(hexadecimal)  // 255

// 2진수 (Binary) - 0b 또는 0B 접두사
val binary = 0b11111111
val binaryUpper = 0B11111111
println(binary)  // 255

// 8진수는 지원하지 않음
// val octal = 0377  // 컴파일 에러! 

// 진법 표기 조합 예제
val flags = 0b0000_1111  // 15
val colorRed = 0xFF_00_00  // 16711680
val permissions = 0b111_101_101  // 493
```

#### Number와 변환 함수

* 숫자를 나타내는 모든 기본 타입은 `Number` 타입의 서브 타입이다. 
* 기본적인 숫자라면 `to{NewType}` 함수를 사용해 다른 기본 타입으로 변환할 수 있다. 
* 이런 함수를 변환 함수(conversion function)라고 한다.

```kotlin
val intValue: Int = 100

// 다양한 타입으로 명시적 변환
val byteValue: Byte = intValue.toByte()      // 100
val shortValue: Short = intValue.toShort()   // 100
val longValue: Long = intValue.toLong()      // 100
val floatValue: Float = intValue. toFloat()   // 100. 0
val doubleValue: Double = intValue.toDouble()  // 100.0

// 자동 변환은 지원하지 않음 - 명시적 변환 필요
val int: Int = 42
// val long: Long = int  // 컴파일 에러!
val long: Long = int.toLong()  // OK

// 오버플로우 예제
val largeInt: Int = 130
val smallByte: Byte = largeInt.toByte()
println(smallByte)  // -126 (오버플로우 발생)

// 부동소수점을 정수로 변환 (소수점 버림)
val pi = 3.14159
val piInt = pi.toInt()
println(piInt)  // 3
```

#### 변환 함수 목록

* 모든 수 타입은 다음 변환 함수를 제공한다.

```kotlin
val number = 100

// 정수 타입으로 변환
number.toByte()    // Byte
number.toShort()   // Short
number.toInt()     // Int
number.toLong()    // Long

// 부동소수점 타입으로 변환
number.toFloat()   // Float
number.toDouble()  // Double

// 문자로 변환 (ASCII 코드)
number.toChar()    // 'd' (ASCII 100)
```

#### 기본 산술 연산자

* 코틀린은 수 타입에 대해 표준 산술 연산자를 제공한다. 

```kotlin
val a = 10
val b = 3

// 덧셈
println(a + b)   // 13

// 뺄셈
println(a - b)   // 7

// 곱셈
println(a * b)   // 30

// 나눗셈 (정수 나눗셈 - 소수점 버림)
println(a / b)   // 3

// 나머지 (모듈로 연산)
println(a % b)   // 1

// 단항 연산자
val positive = +a  // 10
val negative = -a  // -10
```

#### 정수 나눗셈 vs 부동소수점 나눗셈

```kotlin
// 정수 나눗셈 - 결과가 정수
val intResult = 10 / 3
println(intResult)  // 3

// 부동소수점 나눗셈 - 결과가 소수
val doubleResult = 10.0 / 3.0
println(doubleResult)  // 3.3333333333333335

// 정수를 부동소수점으로 변환하여 나눗셈
val mixedResult = 10 / 3.0
println(mixedResult)  // 3.3333333333333335

val convertedResult = 10. toDouble() / 3
println(convertedResult)  // 3.3333333333333335
```

#### 복합 대입 연산자

```kotlin
var count = 10

count += 5  // count = count + 5  -> 15
count -= 3  // count = count - 3  -> 12
count *= 2  // count = count * 2  -> 24
count /= 4  // count = count / 4  -> 6
count %= 5  // count = count % 5  -> 1

println(count)  // 1
```

#### 증가 및 감소 연산자

```kotlin
var x = 10

// 후위 증가/감소
println(x++)  // 10 출력 후 증가
println(x)    // 11

println(x--)  // 11 출력 후 감소
println(x)    // 10

// 전위 증가/감소
println(++x)  // 증가 후 11 출력
println(x)    // 11

println(--x)  // 감소 후 10 출력
println(x)    // 10
```

#### 산술 연산 우선순위

```kotlin
val result1 = 2 + 3 * 4      // 14 (곱셈 먼저)
val result2 = (2 + 3) * 4    // 20 (괄호 먼저)
val result3 = 10 - 5 - 2     // 3 (왼쪽에서 오른쪽)
val result4 = 2 + 3 * 4 / 2  // 8 (*, / 먼저, 왼쪽에서 오른쪽)

// 복잡한 연산
val complex = (10 + 5) * 3 - 8 / 2 + 1
println(complex)  // 42
```

#### 비트 논리 연산

* 코틀린은 정수 타입(`Int`, `Long`)에 대해 비트 단위 연산을 지원한다.

```kotlin
val x = 0b1010  // 10 (2진수)
val y = 0b1100  // 12 (2진수)

// AND - 두 비트가 모두 1일 때 1
val and = x and y
println(and. toString(2))  // 1000 (8)

// OR - 두 비트 중 하나라도 1일 때 1
val or = x or y
println(or. toString(2))   // 1110 (14)

// XOR - 두 비트가 다를 때 1
val xor = x xor y
println(xor.toString(2))  // 0110 (6)

// NOT - 비트 반전 (inv 함수 사용)
val inv = x.inv()
println(inv)  // -11 (2의 보수)
```

#### 비트 시프트 연산

```kotlin
val num = 0b00001010  // 10

// 왼쪽 시프트 (<<) - 2를 곱하는 효과
val leftShift1 = num shl 1
println(leftShift1. toString(2))  // 10100 (20)

val leftShift2 = num shl 2
println(leftShift2.toString(2))  // 101000 (40)

// 오른쪽 시프트 (>>) - 2로 나누는 효과 (부호 유지)
val rightShift1 = num shr 1
println(rightShift1.toString(2))  // 101 (5)

val rightShift2 = num shr 2
println(rightShift2.toString(2))  // 10 (2)

// 부호 없는 오른쪽 시프트 (>>>)
val negative = -16
val signedShift = negative shr 2
val unsignedShift = negative ushr 2
println(signedShift)    // -4 (부호 유지)
println(unsignedShift)  // 1073741820 (부호 무시)
```

#### BigDecimal, BigInteger

* 코틀린의 모든 기본 타입은 표현할 수 있는 크기와 정확도 측면에서 한계가 있기 때문에 특정 상황에서 정확하지 않거나 잘못된 결과를 만들 수 있다. 

JVM에서는:
- **크기에 제한이 없는 정수**가 필요할 때는 `BigInteger`
- **크기에 제한이 없고 정확해야 하는 부동소수점 수**가 필요하면 `BigDecimal`을 사용해야 한다.

#### BigInteger 사용

```kotlin
import java.math.BigInteger

// 기본 타입의 한계
val maxLong = Long.MAX_VALUE
println("Long 최댓값: $maxLong")  // 9223372036854775807
// val overflow = maxLong + 1  // 오버플로우 발생! 

// BigInteger로 해결
val bigInt1 = BigInteger("123456789012345678901234567890")
val bigInt2 = BigInteger("987654321098765432109876543210")

// 산술 연산
val sum = bigInt1 + bigInt2
val difference = bigInt2 - bigInt1
val product = bigInt1 * bigInt2
val quotient = bigInt2 / bigInt1

println("합: $sum")
println("차: $difference")
println("곱: $product")
println("몫: $quotient")

// 기본 타입에서 BigInteger로 변환
val fromInt = 1000. toBigInteger()
val fromLong = 1000000L.toBigInteger()
val fromString = "999999999999999999". toBigInteger()

// BigInteger에서 기본 타입으로 변환
val toInt = fromInt.toInt()
val toLong = fromInt.toLong()
```

#### BigDecimal 사용

```kotlin
import java.math.BigDecimal
import java.math.RoundingMode

// Double의 정확도 문제
val d1 = 0.1
val d2 = 0. 2
println(d1 + d2)  // 0.30000000000000004 (부정확!)

val d3 = 1.0
val d4 = 3.0
println(d3 / d4)  // 0. 3333333333333333 (반올림됨)

// BigDecimal로 정확한 계산
val bd1 = BigDecimal("0.1")
val bd2 = BigDecimal("0.2")
println(bd1 + bd2)  // 0.3 (정확!)

// 금융 계산 예제
val price = BigDecimal("19.99")
val quantity = BigDecimal("3")
val taxRate = BigDecimal("0. 1")  // 10%

val subtotal = price * quantity  // 59.97
val tax = subtotal * taxRate     // 5.997
val total = subtotal + tax       // 65.967

// 반올림 설정
val roundedTax = tax.setScale(2, RoundingMode.HALF_UP)      // 6.00
val roundedTotal = total. setScale(2, RoundingMode.HALF_UP)  // 65.97

println("소계: $$subtotal")
println("세금: $$roundedTax")
println("합계: $$roundedTotal")

// 비교 연산
val value1 = BigDecimal("10.00")
val value2 = BigDecimal("10.0")
println(value1 == value2)                    // false (스케일이 다름)
println(value1.compareTo(value2) == 0)       // true (값이 같음)
```

#### 불 리터럴

* `true`, `false` 두 가지 값만 가능한 `Boolean`도 기본 타입으로 제공한다.

```kotlin
val isTrue: Boolean = true
val isFalse: Boolean = false

// 타입 추론
val autoTrue = true    // Boolean
val autoFalse = false  // Boolean
```

#### 논리 연산자

```kotlin
val a = true
val b = false

// AND (&&) - 둘 다 true일 때만 true
println(a && b)   // false
println(true && true)   // true

// OR (||) - 하나라도 true면 true
println(a || b)   // true
println(false || false)  // false

// NOT (!) - 반전
println(!a)       // false
println(! b)       // true

// 복합 논리 연산
val result = (a && b) || (! a && !b)
println(result)   // false
```

#### 비교 연산자

* 비교 연산자는 `Boolean` 결과를 반환한다. 

```kotlin
val x = 10
val y = 20

// 동등 비교
println(x == y)   // false (같음)
println(x != y)   // true (다름)

// 크기 비교
println(x < y)    // true (작음)
println(x > y)    // false (큼)
println(x <= y)   // true (작거나 같음)
println(x >= y)   // false (크거나 같음)

// 문자열 비교
val str1 = "abc"
val str2 = "abc"
val str3 = "def"

println(str1 == str2)  // true
println(str1 == str3)  // false
println(str1 < str3)   // true (사전순)
```

#### 단락 평가 (Short-circuit Evaluation)

```kotlin
fun expensive(): Boolean {
    println("expensive() 호출됨")
    return true
}

// AND 연산 - 첫 번째가 false면 두 번째 평가 안 함
val and = false && expensive()
// "expensive() 호출됨" 출력 안 됨
println(and)  // false

// OR 연산 - 첫 번째가 true면 두 번째 평가 안 함
val or = true || expensive()
// "expensive() 호출됨" 출력 안 됨
println(or)  // true

// 두 번째도 평가하는 경우
val and2 = true && expensive()
// "expensive() 호출됨" 출력됨
println(and2)  // true
```

#### 문자열

* 삼중 따옴표 문자열에 `trimIndent` 함수를 사용하면 문자열을 더 깔끔하게 표현할 수 있다.
* 해당 함수는 들여쓰기용으로 쓰인 공백들을 모든 줄에서 일관되게 잘라준다.

### val vs const val

```kotlin
// val - 런타임 상수 (실행 시 값 결정)
val runtimeConstant = System.currentTimeMillis()
val calculated = 10 * 20

// const val - 컴파일 타임 상수 (컴파일 시 값 결정)
// 최상위 레벨 또는 object 내에서만 선언 가능
// 기본 타입과 String만 가능
const val COMPILE_TIME_CONSTANT = 100
const val PI = 3.141592653589793
const val APP_NAME = "My Application"

// const val은 표현식 사용 불가
// const val INVALID = 10 * 20  // 컴파일 에러! 

// 사용 예제
const val MAX_RETRY_COUNT = 3
const val DEFAULT_TIMEOUT = 5000L
const val API_VERSION = "v1"

fun connectWithRetry() {
    for (i in 1..MAX_RETRY_COUNT) {
        println("연결 시도 $i/$MAX_RETRY_COUNT")
    }
}
```

#### 정수 오버플로우

```kotlin
// 오버플로우 예제
val maxInt = Int.MAX_VALUE
println("Max Int: $maxInt")  // 2147483647

val overflow = maxInt + 1
println("Overflow: $overflow")  // -2147483648 (오버플로우!)

// 안전한 처리
val safeLong = maxInt.toLong() + 1
println("Safe: $safeLong")  // 2147483648
```

#### 부동소수점 정확도 문제

```kotlin
// 부동소수점 계산의 부정확성
println(0.1 + 0.2)  // 0. 30000000000000004
println(1.0 / 3.0)  // 0.3333333333333333

// 비교 시 주의
val a = 0.1 + 0.2
val b = 0.3
println(a == b)  // false! 

// 해결 방법 1: 임계값 사용
val epsilon = 0.0000001
println(kotlin.math.abs(a - b) < epsilon)  // true

// 해결 방법 2: BigDecimal 사용
val bd1 = BigDecimal("0.1")
val bd2 = BigDecimal("0.2")
val bd3 = BigDecimal("0.3")
println(bd1 + bd2 == bd3)  // false (스케일 다름)
println((bd1 + bd2).compareTo(bd3) == 0)  // true
```

#### NaN과 무한대

```kotlin
// 0으로 나누기
val divByZero = 1. 0 / 0.0
println(divByZero)  // Infinity

val negDivByZero = -1. 0 / 0.0
println(negDivByZero)  // -Infinity

// NaN (Not a Number)
val nan = 0.0 / 0. 0
println(nan)  // NaN

// NaN 확인
println(nan. isNaN())  // true
println(nan == nan)   // false (NaN은 자기 자신과도 같지 않음!)

// 무한대 확인
println(divByZero.isInfinite())  // true
println(divByZero.isFinite())    // false
```
