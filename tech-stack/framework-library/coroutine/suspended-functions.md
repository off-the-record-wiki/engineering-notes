# Coroutine - Suspended Functions

#### 일시 중단 함수와 코루틴

* 일시 중단 함수란 `suspend fun` 키워드로 선언되는 함수로 함수 내에 '일시 중단 지점'을 포함할 수 있는 함수이다.
* 일시 중단 함수는 코루틴의 일시 중단 지점이 포함된 코드를 재사용할 수 있는 코드의 집합으로 만드는데 사용된다.
* 일시 중단 지점이 포함될 수 있기 때문에 코루틴에서만 호출이 가능하다.
* 일시 중단 함수 자체가 코루틴은 아니다.
* 일시 중단 함수를 별도의 코루틴에서 실행하려면 일시 중단 함수 호출부를 코루틴 빌더 함수로 감싸야 한다.

#### 일시 중단 함수의 사용

```kotlin
suspend fun searchByKeyword(keyword: String): Array<String> = coroutineScope {
    val dbResultsDeferred: Deferred<Array<String>> = async {
        searchFromDB(keyword)
    }
    val serverResultsDeferred: Deferred<Array<String>> = async {
        searchFromServer(keyword)
    }

    return@coroutineScope arrayOf(*dbResultsDeferred.await(), *serverResultsDeferred.await())
}

suspend fun searchFromDB(keyword: String): Array<String> {
    delay(1000L)
    return arrayOf("[DB]${keyword}1", "[DB]${keyword}2")
}

suspend fun searchFromServer(keyword: String): Array<String> {
    delay(1000L)
    return arrayOf("[Server]${keyword}1", "[Server]${keyword}2")
}
```

> [📚suspending functions](https://kotlinlang.org/docs/composing-suspending-functions.html)
>
> **1. suspend 함수의 정의**
> * `suspend`는 일시 중단을 의미한다. `suspend fun`은 실행을 잠시 멈췄다가 작업이 완료되면 나중에 다시 재개할 수 있는 함수를 말한다.
> * 이 함수가 일시 중단될 때, 현재 코루틴이 실행 중인 쓰레드를 블로킹하지 않는다는 것이다.
>
> **2. 주요 특징 정리**
> * 논블로킹(Non-Blocking) : 네트워크 요청이나 파일 읽기처럼 시간이 오래 걸리는 작업을 만나면 `suspend` 함수는 쓰레드를 점유한 채로 기다리지 않는다. 코루틴을 잠시 멈추고 제어권을 반환해 해당 쓰레드가 다른 코루틴을 처리할 수 있도록 양보한다.
> * 콜백(CallBack) 없는 동기적 코드 스타일 : 비동기 코드 작성 시 콜백 지옥이나 RxJava 체이닝없이 위에서 아래로 흐르는 순차적인 코드처럼 직관적으로 작성할 수 있게 해준다.
> * 호출 위치의 엄격한 제한 : `suspend` 키워드가 붙은 함수는 일반 함수에서 함부로 호출할 수 없다. 코루틴의 일시 중단과 재개를 관리할 수 있는 환경이 필요하므로, 반드시 다른 `suspend` 함수 내부이거나 `launch`, `async`와 같은 코루틴 빌더 블록 내부에서만 호출해야 한다.
>
> **3. Blocking vs Suspend 비교**
> * Blocking : 쓰레드 자체가 대기 상태에 빠져 다른 일을 하지 못한다. 제어의 주체는 운영체제가 쥐고 있으며 OS 수준의 컨텍스트 스위칭 발생이 수반된다.
> * Suspend : 코루틴만 잠들고 쓰레드는 다른 코루틴을 실행할 수 있도록 한다. 제어의 주체는 Kotlin 코루틴 라이브러리로 메모리 사에서 함수 수준의 상태 객체만 교체한다.
