# Coroutine - Limitations of Thread-Based Work & the Emergence of Coroutines

#### 단일 쓰레드 애플리케이션 한계와 멀티 프로그래밍

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ea7a522a-1228-4780-b835-70c2158d1212.png)

* 쓰레드는 한 번에 하나의 작업밖에 수행하지 못한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b3754188-c1c9-4393-a1db-89ad7f840877.png)

* 만약 다음과 같이 메인 쓰레드가 오래 걸리는 작업에 의해 점유된다면 앱이 버벅이고 ANR이 발생하게 된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/1ce3ff97-0890-4d4a-91ce-fa55507e89f3.png)

* 서버에서 사용자의 요청을 단일 쓰레드만을 사용해 처리하면 요청 처리에 오랜 시간이 걸린다.
* 그래서 등장하게 된 개념이 바로 멀티 쓰레드 프로그래밍이다. 멀티 쓰레드 프로그래밍이란, 여러 개의 쓰레드를 사용해 작업을 처리하는 프로그래밍 기법을 말한다.
* 다른 쓰레드를 추가로 사용해 오래 걸리는 작업을 처리하면 문제를 해결할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/06002138-376b-4086-a73d-3cee835583fc.png)

* 다음과 깉이 여러 개의 쓰레드를 사용해 병렬 처리하면 해결이 가능하다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ea659042-e60a-4ccd-8a4d-5caac5413653.png)

#### 쓰레드를 사용한 멀티 쓰레드 프로그래밍

```kotlin
import java.lang.Thread

class ExampleThread : Thread() {
    override fun run() {
        println("[${Thread.currentThread().name}] 시작")
        Thread.sleep(2000L)
        println("[${Thread.currentThread().name}] 종료")
    }
}

fun main() {
    println("[${Thread.currentThread().name}] 시작")
    
    ExampleThread().start()
    
    Thread.sleep(1000L)
    println("[${Thread.currentThread().name}] 종료")
}
```

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/bc7a96d9-ac5f-4dc7-a4e8-6b95acf51d2f.png)

* 위와 같이 `Thread`를 사용하면 간단하게 병렬 처리가 가능하다.
* 그러나 `Thread`를 사용한 멀티 쓰레드 프로그래밍은 다음과 같은 한계를 지니고 있다.

**1. `Thread`의 `start()` 함수를 호출할 때마다 새로운 쓰레드가 생성되고 재사용이 어렵다.**
* 쓰레드는 비싼 자원이다.
* 그렇기 때문에 재사용이 어렵다.

**2. 개발자가 쓰레드 생성과 관리에 대한 책임을 가진다.**
* 개발자의 실수나 오류로 인해서 메모리 누수가 일어날 수 있다.
* 프로그램이 복잡해질수록 쓰레드의 생성과 관리를 직접 하는 것은 불가능에 가까워진다.

#### 쓰레드 풀을 사용한 멀티 쓰레드 프로그래밍

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/78eb12c6-6bfb-4e37-bfc7-38ee63a694a4.png)

* 쓰레드는 비싼 자원이고 재사용도 어렵다.
* 위와 같은 단점들을 해결하기 위해 `Executor` 프레임워크가 등장했다.
* `Executor` 프레임워크란 쓰레드의 집합인 쓰레드 풀을 미리 생성해놓고 작업을 요청 받으면 쉬고 있는 쓰레드에 작업을 분배할 수 있는 시스템을 말한다.
  * 개발자가 더 이상 쓰레드를 직접 관리하지 않도록 했다. 개발자가 할 일은 쓰레드 개수 지정과 작업 제출 뿐이다.
  * 쓰레드의 재사용을 손쉽게 가능하게 만들었다.
* 그러나 모든 기술엔 장점만 있는 것이 아니다. `Executor` 프레임워크는 쓰레드 블로킹 문제가 발생할 수 있다.
* 쓰레드 블로킹 문제란, 쓰레드가 사용될 수 없는 상태에 있는 것을 말한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/5afaf2da-7bc8-4420-9eb4-1c79b1a51372.png)

#### 쓰레드 기반 작업을 사용하는 방식의 한계와 코루틴의 등장

* 쓰레드 기반 작업들은 작업 전환이 어렵고 전환 간에 드는 비용이 비싸다.
* 간단한 작업이라면 앞서 본 콜백 방식을 사용하거나 체이닝 함수를 사용하는 방식으로 쓰레드 블로킹 문제를 해결할 수 있지만, 실제 애플리케이션은 작업 간 종속성이 복잡해 쓰레드 블로킹이 발생하는 것은 필연적이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/093d11b1-f7b3-4f28-a136-05e820eac303.png)

* 코루틴에서는 '코루틴'이라고 불리는 작업 단위를 사용한다.
* 코루틴은 쓰레드의 사용 권한을 양보할 수 있다. 즉, 쓰레드에 붙었다 떼었다 할 수 있는 작업 단위가 된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/5fda113e-ecb2-4123-be95-9ca426f2f9e0.png)

#### 코루틴을 언제 사용하면 좋은지?

* 코루틴은 I/O 작업(네트워크, DB 입출력)과 같은 곳에서 사용하면 좋은 성능을 기대할 수 있지만 CPU Bound 작업(이미지/동영상의 인코딩 혹은 디코딩, 대용량 데이터 변환)에서는 코루틴을 사용해도 성능이 비슷하다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/83333b56-dc05-4ccd-84d9-3455da901127.png)

<br>

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/8385fe92-6b38-4d55-8a8c-be3d88d2c94d.png)

<br>

> [📚Coroutine](https://en.wikipedia.org/wiki/Coroutine)
>
> **1. 코루틴(Coroutine)의 정의 및 명세**
> * 코루틴은 실행 도중 임의의 지점에서 `suspend`되고 이후 해당 지점부터 다시 재개할 수 있는 비선점형 서브루틴을 말한다.
> * 일반적인 서브루틴 : 진입점은 항상 함수의 시작 부분이며, `return` 시그널을 만나면 활성화 레코드가 완전히 파괴되고 제어권이 호출자에게 영구적으로 반환된다.
> * 코루틴 : 실행 도중 제어권을 호출자나 다른 코루틴에게 양보할 수 있다. 이 때, 힙 메모리에 코루틴의 상태가 보존되므로 재개 시 이전 상태를 그대로 복원하여 연산을 이어갈 수 있다.
>
> **2. OS 관점에서의 스레드 vs 코루틴 메커니즘 비교**
> * 쓰레드(Thread)
>   * 관리 주체 : 운영체제(OS) 커널 모드에서 실행된다.
>   * 동작 : 선점형 스케줄링을 따른다. 타임 슬라이스가 만료되면 OS 하드웨어 인터럽트에 의해 쓰레드의 제어권이 강제로 찬탈당한다.
>   * 컨텍스트 스위칭 비용 : 쓰레드 간 전환 시 커널 공간과 사용자 공간 간의 모드 전환이 발생한다. CPU 레지스터의 상태를 TCB에 저장하고 복원하는 과정에서 캐시 미스 및 커널 오버헤드가 크게 발생한다. 
>   * 메모리 자원 : 각 쓰레드는 고정된 크기의 독립적인 스택 영역을 할당받으므로, 수천 개의 쓰레드를 생성하면 메모리 고갈이 발생한다.
>
> * 코루틴(Coroutine)
>   * 관리 주체 : OS 커널이 아닌 사용자 공간의 런타임 라이브러리나 가상 머신(JVM) 레벨에서 관리한다.
>   * 동작 : 비선점형 스케줄링을 따른다. 코루틴 스스로가 제어권을 양보(suspend)하기 전까지는 다른 코루틴이 끼어들 수 없다. 즉, 코드 레벨에서 명시적으로 제어권 권한 시점을 통제한다.
>   * 컨텍스트 스위칭 비용 : 커널 모드 전환이 전혀 발생하지 않는 전적인 사용자 공간 내부에서의 객체 스위칭이다. 단순 함수 호출 수준의 CPU 레지스터 포인터 변경만 발생하므로 컨텍스트 스위칭 오버헤드가 쓰레드 대비 수백 배 이상 미미하다.
>   * 메모리 자원 : 수바이트 ~ 수KB 수준의 매우 가벼운 객체 형태로 힙 메모리에 할당되므로, 단일 쓰레드 내에서 수십만 개의 코루틴을 오버헤드없이 동시 구동할 수 있다.  
