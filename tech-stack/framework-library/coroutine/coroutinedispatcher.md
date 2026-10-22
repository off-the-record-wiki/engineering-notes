# Coroutine - CoroutineDispatcher

#### CoroutineDispatcher의 의미와 동작 방식

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/92fa7c15-00a7-4905-a3ea-f0f95cfbe263.png)

* 사용자의 요청 : `launch`나 `async`와 같은 빌더를 통해 코루틴이 새로 생성된다.
* 작업 대기열(Queue) 적재 : 디스패처는 넘겨받은 코루틴을 관리 하에 있는 동기화된 작업 대기열에 적재한다.
* 쓰레드 풀 상태 : Thread-1이 코루틴 1을, Thread-2가 코루틴 2를 할당받아 하부 CPU 코어 위에서 활발히 동기 연산을 수행 중인 상태이다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/86556902-f022-4eb8-aa4e-4e1766ba535d.png)

* 쓰레드 가용 상태 전환 : Thread-1이 실행 중이던 코루틴 1 연산을 완료하면 해당 쓰레드는 디스패처에게 다음 작업을 요청한다.
* Deque 및 Dispatch : 디스패처는 작업 대기열의 코루틴 3을 꺼내어 비어있는 Thread-1에 바인딩한다.
* 실행 : Thread-1은 코루틴 3 내부에 정의된 코드 블록을 실행하기 시작한다.

#### 단일 쓰레드 디스패처

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/57e8fd2d-8c3b-4468-a5bd-7320e7cea518.png)

* 단일 쓰레드 디스패처란, 생성된 코루틴들을 오직 단 하나의 쓰레드에만 할당해 순차적으로 실행하도록 강제하는 코루틴 디스패처이다.
* 다중 쓰레드 풀을 사용하는 다른 것과 달리 작업 대기열에 쌓인 코루틴들이 아무리 많아도 단 1개의 지정된 쓰레드가 컨텍스트 스위칭을 하며 작업을 처리한다.

#### 멀티 쓰레드 디스패처

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/5e3ad27d-94eb-4f50-ad31-77e2a6120c10.png)

* 멀티 쓰레드 디스패처란, 생성된 코루틴들을 여러 개의 쓰레드로 구성된 쓰레드 풀에 분배해 병렬 및 동시 실행을 제어하는 코루틴 디스패처이다.
**1. 내부 아키텍처 및 구동 메커니즘**
* 공유 작업 대기열 및 로컬 대기열 : 생성된 코루틴들은 일차적으로 디스패처가 관리하는 공유 작업 대기열에 적재되거나 각 워커 쓰레드가 소유한 독립적인 로컬 큐에 할당된다.
* 라운드 로빈 및 작업 할당 : 풀 내부의 가용 상태인 워커 쓰레드들이 큐에서 코루틴 객체를 하나씩 꺼내어 자신에게 할당된 CPU 코어 위에서 실행한다.
* 작업 훔치기 알고리즘 : 특정 쓰레드가 자신에게 할당된 로컬 큐 작업을 모두 끝내면 여전히 작업이 많이 남아있는 다른 쓰레드의 로컬 큐에서 코루틴을 훔쳐와 대신 실행함으로써 쓰레드 풀 전체의 부하 분산을 극대화한다.
* 논블로킹 인터리빙 : 실행 중인 코루틴이 `suspend()`를 만나면 해당 쓰레드는 해당 코루틴을 힙 메모리에 보존 처리한 뒤 큐로 돌아가 즉시 다음 대기 중인 코루틴을 실행한다.

> **This is a delicate API. The result of this method is a closeable resource with the associated native resources (threads or native workers).**
> * 컴퓨터 자원은 크게 두 종류로 나뉘는데 JVM이 관리하는 힙 메모리, OS가 관리하는 자원들(쓰레드, 네이티브 메모리, 파일 핸들, 소켓 등)이 있다.
> * JVM의 경우 GC에 의해 수거가 되지만 OS가 관리하는 자원들은 GC의 관리 범위 밖이다.
> * `new`를 호출하는 순간 OS 레벨에서 쓰레드가 생성되고 네이티브 메모리가 점유된다. 이 시점부터 해당 자원은 명시적으로 반환되기 전까지 계속 점유된 상태를 유지하는데 이렇게 되면 두 가지 문제가 생긴다.
> * 하나는 GC에 맡기면 힙의 객체만 제거될 뿐, OS에 할당된 쓰레드와 메모리는 반환되지 않는다. 이것이 자원 누수이다.
> * 다른 하나는 `new`를 반복 호출할 때마다 OS 자원 할당이 반복되는데, 이 작업 자체가 비용이 크다. 누적되면 메모리 고갈이나 시스템 성능 저하로 이어진다.
> * 그래서 이 객체는 한 번 생성해서 재사용하고, 더 이상 필요하지 않은 시점에 반드시 `close()`를 명시적으로 호출해서 OS 자원을 반환해야 한다.

#### Dispatchers.IO

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/05958266-ad34-48e8-bd63-2c456a43d2c6.png)

* `Dispatchers.IO`는 파일 입출력, 네트워크 요청, 데이터베이스 쿼리 실행 등과 같은 물리적 대기 시간을 수반하는 블로킹 I/O 작업을 처리하도록 설계된 멀티 쓰레드 기반의 코루틴 디스패처이다.
* 초기상태(idle) : 애플리케이션 구동 초기에는 불필요하게 쓰레드를 많이 생성하지 않고 최소한의 워커 쓰레드만 유지한다.
* 수요 기반 생성 : 대량의 블로킹 I/O 작업을 가지는 코루틴들이 `Dispatchers.IO`로 들어오면 기존 쓰레드들이 모두 블로킹 상태에 빠져 큐의 작업을 처리할 수 없게 된다. 이 때, 디스패처는 최대 임계치에 도달하기 전까지 실시간으로 새로운 쓰레드를 풀에 추가 생성해 적재된 코루틴들을 할당한다.
* 자원 회수 및 셧다운 : I/O 트래픽 스파이크가 지나가고 작업 대기열이 비워지면, 새로 생성되었던 쓰레드들이 가용(idle) 상태로 전환된다. 지정된 타임아웃(Keep-Alive Time)동안 추가 작업이 들어오지 않는 쓰레드들은 JVM 자원 반환을 위해 런타임에 의해 자동으로 파괴되어 메모리 점유율을 스스로 낮춘다.
* 기본적으로 사용할 수 있는 쓰레드의 수는 64와 JVM에서 사용할 수 있는 프로세서의 수 중 큰 값이다.

#### Dispatchers.Default

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/dc3b3a7c-a591-4d55-aa77-5346a2e26beb.png)

* `Dispatchers.Default`는 CPU 바운드 작업(대규모 데이터 정렬, 복잡한 알고리즘 연산, JSON/XML 파싱, 이미지 및 동영상 인코딩, 암호화 가동)을 위한 디스패처이다.
* 코루틴 빌더를 사용할 때 별도의 디스패처를 명시하지 않으면 이 디스패처가 기본값으로 자동 지정된다.
* 논리적 태그 제어 : 동일한 워커 쓰레드 풀 내부에서 현재 실행되는 코루틴이 `Default` 규칙으로 들어왔는지, `IO` 규칙으로 들어왔는지 논리적으로 카운팅하며 쓰레드 제한 수만 동적으로 변환한다.
* 스위칭 최적화 : 비즈니스 로직 도중 대용량 가공 연산을 하다가 파일 저장으로 넘어가기 위해 `withContext(Dispatchers.IO)`를 호출하더라도 쓰레드가 바뀌는 하드웨어 스위칭 없이 동일한 쓰레드 내에서 객체의 컨텍스트만 바뀌어 실행되므로 내부 연산 효율이 극대화된다.

#### 공유 쓰레드 풀과 limitedParallelism

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4985f7fb-808a-46a4-b463-6f123107a16b.png)

**1. CoroutineScheduler**
* Kotlin Coroutine 라이브러리는 JVM 환경에서 구동될 때, 내장 디스패처들을 위해 자체 설계한 고성능 쓰레드 풀인 `CoroutineScheduler`를 단 하나만 개설해 관리한다.
* 공유 쓰레드 풀이 바로 `CoroutineScheduler` 인스턴스를 말한다. `Dispatchers.Default`와 `Dispatchres.IO`는 별개의 쓰레드 풀을 각각 생성하는 것이 아니라 단 하나의 스케줄러에서 쓰레드를 나누어 쓰는 구조인 것이다.

**2. limitedParallelism**
* `limitedParallelism`은 기존에 존재하는 디스패처를 기반으로 동시 실행 가능한 코루틴의 최대 개수를 지정된 상한선으로 제한하는 역할을 수행한다.
* 새로운 쓰레드 풀을 개설하는 비용을 지불하지 않는다. 원래 디스패처의 쓰레드 자원을 그대로 재사용하되, 제출된 코루틴들에 대해서만 논리적인 동시성 상한선을 적용한다.

#### Dispatchers.Main

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4e7b5e62-2d27-4027-b9ec-985af5a5f0a6.png)

* 메인 쓰레드에서의 작업을 위한 디스패처로 기본 코루틴 라이브러리에는 구현체가 없다.
* 해당 디스패처를 사용하려면 안드로이드 코루틴 라이브러리 추가가 필요하다.
* `Dispatchers.Main`은 여러 쓰레드에서 코루틴을 실행하는 요청하든 코루틴을 작업 대기열에 먼저 적재한 후 Main Thread가 비었을 때, 코루틴을 보낸다.

#### Dispatchers.Main.immediate

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c4e66aa6-4511-404e-89a8-e158c1aedab9.png)

* `Dispatchers.Main.immediate`는 코루틴을 실행하는 코드가 메인 쓰레드에서 실행되고 있다면 작업 대기열에 적재될 필요없이 메인 쓰레드에서 그대로 실행될 수 있게 한다.
* 만약 백그라운드 쓰레드에서 코루틴을 실행 요청하면 작업 대기열에 적재 후 메인 쓰레드로 보낸다.(=Dispatchers.Main과 같은 메커니즘)
