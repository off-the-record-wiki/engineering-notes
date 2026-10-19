# Kotlin - Miscellaneous Topics in Kotlin

## 📚 코틀린 표준 라이브러리 함수들

* `repeat` : 반복 횟수 지정
* `measureTimeMillis` : 코드 소요 시간 측정
* `require` : 파라미터 검증 → `IllegalArgumentException`을 던진다.
* `check` : 상태 혹은 멤버 변수 검증 → `IllegalStateException`을 던진다.
* `runCatching` : 요청을 보내면 Result 객체를 받아 Caller가 원하는대로 처리할 수 있다.

## 📚 꼬리 재귀함수와 인라인 클래스, multiple catch

```kotlin
tailrec fun factorialV2(n: Int, curr: Int = 1): Int {
    return if (n <= 1) {
        curr
    } else {
        factorialV2(n-1, n * curr)
    }
}
```

* 일반적으로 재귀 함수를 사용하면 스택이 쌓여 메모리에 부담이 될 수 있다.
* `tailrec` 키워드를 사용하면 스택이 쌓이는 재귀 함수 호출을 루프로 변경해 컴파일 시킨다.

```kotlin
class ResultWrapper<T>(
    private val result: Result<T>,
    private val knownExceptions: MutableList<KClass<out Throwable>>,
) {
    fun onError(vararg exceptions: KClass<out Throwable>, action: (Throwable) -> Unit): ResultWrapper<T> {
    this.result.exceptionOrNull()?.let {
        if (it::class in exceptions && it::class !in this.knownExceptions) {
            action(it)
        }
    }
    return this
}

// ...

runCatching { logic(10) }
    .onError(AException::class, BException::class) {
        println("A 또는 B 예외가 발생했습니다.")
    }
    .onError(CException::class) {
        println("C 예외가 발생했습니다.")
    }
}
```

* `try ~ catch`문에서 여러 예외를 받고 싶다면 Result\<T> 객체를 활용해서 만들 수도 있다.
* 다만 이 방식은 보일러 플레이트 코드 증가를 유발하니 고민해서 사용해야한다.
