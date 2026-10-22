# Java - Thread Pool & Executor Framework

#### 쓰레드 사용시 주의할 점

1. 쓰레드 생성 비용으로 인한 성능 문제 유발

* **메모리 할당** : 각 쓰레드는 자신만의 호출 스택을 가지고 있어야 한다. 이 호출 스택은 쓰레드가 실행되는 동안 사용하는 메모리 공간이다. 따라서 쓰레드를 생성할 때는 이 호출 스택을 위한 메모리를 할당해야 한다.
* **운영체제 자원 사용** : 쓰레드를 생성하는 작업은 운영체제 커널 수준에서 이루어지며, 시스템 콜을 통해 처리된다. 이는 CPU와 메모리 리소스를 소모한다.
* **운영체제 스케줄러 설정** : 새로운 쓰레드가 생성되면 운영체제의 스케줄러는 이 쓰레드를 관리하고 실행 순서를 조정해야 한다. 이는 운영체제 스케줄러 알고리즘에 따라 추가적인 오버헤드가 발생한다.
* 쓰레드를 생성하는 작업은 상대적으로 무겁다. 단순히 자바 객체를 하나 생성하는 것과는 비교할 수 없을 정도로 큰 작업이다.
* 이런 문제를 해결하려면 쓰레드 재사용을 고려할 수 있다. 쓰레드를 재사용하려면 처음 생성할 때를 제외하고 생성을 위한 시간이 들지 않는다.

2. 쓰레드 관리 문제

* 서버의 CPU, 메모리 자원은 한정적이기 때문에 쓰레드를 무한히 만들 수 없다.
* 문제를 해결하려면 시스템이 버틸 수 있는 최대 쓰레드의 수까지만 쓰레드를 생성할 수 있게 관리해야 한다.

3. `Runnable` 인터페이스의 번거로움

* **반환 값이 없다** : `run()` 메서드는 반환 값이 없기에 실행 결과를 얻기 위해서는 별도의 메커니즘을 사용해야 한다. 쉽게 이야기해서 쓰레드 실행 결과를 직접 받을 수 없다.
* **예외 처리** : `run()` 메서드는 체크 예외를 던질 수 없다. 체크 예외 처리는 메서드 내부에서 처리해야 한다.

#### Executor 프레임워크

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/5f471f20-86e1-4ec3-8c55-03e5d3f24bf0.png)

* ThreadPoolExecutor(ExecutorService)는 크게 2가지 구성요소로 이루어져 있다.
  * 쓰레드 풀 : 쓰레드를 관리한다.
  * BlockingQueue : 작업을 보관한다. 생산자 소비자 문제를 해결하기 위해 단순한 큐가 아니라 BlockingQueue를 활용한다.
* ThreadPoolExecutor 생성자
  * `corePoolSize` : 쓰레드 풀에서 관리되는 기본 쓰레드 수
  * `maximumPoolSize` : 쓰레드 풀에서 관리되는 최대 쓰레드 수
  * `keepAliveTime`, `TimeUmit unit` : 기본 쓰레드 수를 초과해서 만들어진 쓰레드가 생존할 수 있는 대기 시간
  * `BlockingQueue workQueue` : 작업을 보관할 블로킹 큐

#### Future

* Future는 작업의 미래 결과를 받을 수 있는 객체이다.
* `submit()`을 호출하면 `future`는 즉시 반환된다. 덕분에 요청 쓰레드는 블로킹 되지 않고 필요한 작업을 할 수 있다.
* Future가 완료 상태면 요청 쓰레드는 대기하지 않고 값을 즉시 반환받을 수 있다.
* Future가 완료 상태가 아니라면 어쩔 수 없이 요청 쓰레드가 결과를 받기 위해 블로킹 상태로 대기해야 한다.

#### Future 필요성

* Future라는 개념 덕분에 요청 쓰레드가 끝날 때까지 대기하지 않고 다른 작업을 수행할 수 있게 된 것이다.
* Future는 요청 쓰레드를 블로킹(대기) 상태로 만들지 않고 필요한 요청을 모두 수행할 수 있게 해준다.
* 필요한 모든 요청을 처리한 다음에 `Future.get()`을 통해 블로킹 상태로 대기하며 결과를 받으면 된다.

### Future 주요 메서드

```java
public interface Future<V> {

    /**
     * Attempts to cancel execution of this task.  This method has no
     * effect if the task is already completed or cancelled, or could
     * not be cancelled for some other reason.  Otherwise, if this
     * task has not started when {@code cancel} is called, this task
     * should never run.  If the task has already started, then the
     * {@code mayInterruptIfRunning} parameter determines whether the
     * thread executing this task (when known by the implementation)
     * is interrupted in an attempt to stop the task.
     *
     * <p>The return value from this method does not necessarily
     * indicate whether the task is now cancelled; use {@link
     * #isCancelled}.
     *
     * @param mayInterruptIfRunning {@code true} if the thread
     * executing this task should be interrupted (if the thread is
     * known to the implementation); otherwise, in-progress tasks are
     * allowed to complete
     * @return {@code false} if the task could not be cancelled,
     * typically because it has already completed; {@code true}
     * otherwise. If two or more threads cause a task to be cancelled,
     * then at least one of them returns {@code true}. Implementations
     * may provide stronger guarantees.
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    boolean isCancelled();

    /**
     * Returns {@code true} if this task completed.
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * {@code true}.
     *
     * @return {@code true} if this task completed
     */
    boolean isDone();

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     */
    V get() throws InterruptedException, ExecutionException;

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     * @throws TimeoutException if the wait timed out
     */
    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
```

#### Executor 쓰레드 풀 관리

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/33ac8dfe-05c3-42a4-a3da-fe5ce4e86d93.png)

* `task1` 작업을 요청한다.
* `Executor`는 쓰레드 풀에 쓰레드가 core 사이즈만큼 있는지 확인한다.
  * 만약 core 사이즈만큼 없으면 쓰레드를 하나 생성한다.
  * 작업을 처리하기 위해 쓰레드를 하나 생성했기 때문에 작업을 큐에 넣을 필요없이 해당 쓰레드가 바로 작업을 처리할 수 있게 된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a539ceba-3456-4c1f-90c9-4f94262b52bf.png)

* 새로 만들어진 쓰레드가 `task1`을 수행한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d3edf390-123e-4c72-bd9e-e6e74f60fa93.png)

* `task2`를 요청한다.
* `Executor`는 쓰레드 풀에 쓰레드가 core 사이즈만큼 있는지 확인한다.
* 아직 core 사이즈만큼 쓰레드가 안 찼기에 쓰레드를 하나 생성한다.
* 새로 만들어진 쓰레드가 `task2`를 수행한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/6cf3873e-aa51-4da1-ac2b-a058a04304ed.png)

* `task3`를 요청한다.
* `Executor`는 쓰레드 풀에 core 사이즈만큼 있는지 확인한다.
* 이미 core 사이즈(=2)만큼 쓰레드가 만들어져 있기 때문에 쓰레드 풀에 사용할 수 있는 쓰레드가 없으므로 이 경우에는 큐에 작업을 보관한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7252817e-df21-45c2-8228-c9e279f4c757.png)

* `task4`를 요청한다.
* `Executor`는 쓰레드 풀에 core 사이즈만큼 있는지 확인한다.
* 이미 core 사이즈(=2)만큼 쓰레드가 만들어져 있기 때문에 쓰레드 풀에 사용할 수 있는 쓰레드가 없으므로 이 경우에는 큐에 작업을 보관한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/bf9504c5-40e4-49c3-86b8-4316288d2425.png)

* `task5`를 요청한다.
* `Executor`는 쓰레드 풀에 core 사이즈만큼 있는지 확인한다.
* 이미 core 사이즈(=2)만큼 쓰레드가 만들어져 있기 때문에 큐에 보관을 하려고 시도하나 큐 사이즈(=2) 역시 가득 찼다.
* 큐가 가득차면 긴급 상황이 된다. 대기하는 작업이 꽉 찰 정도로 요청이 많다는 의미이기 때문이다.
* 이 경우에 해당되면 `Executor`는 쓰레드 풀의 max 사이즈까지 초과 쓰레드를 만들어서 작업을 수행한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/bc835891-c128-4453-9df1-f4d45f6ef4ee.png)

* `task6`를 요청한다.
* 큐도 가득 찼다.
* 초과 쓰레드를 만들어서 작업을 수행한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/66939007-81f1-4e2e-858b-2b3395e41d14.png)

* `task7`를 요청한다.
* 큐도 가득 찼다.
* 쓰레드 풀 max 사이즈도 다 찼다.
* 이 경우 큐에 넣을 수도 없고 작업을 수행할 쓰레드도 만들 수 없다. 따라서 작업을 거절하게 되면서 `RejectedExecutionException`이 발생한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/857e83b1-2eba-4c82-a3b1-69a26dba1a2a.png)

* 모든 작업이 완료된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/eed40b5d-b47b-4579-a3c8-e880437aeba7.png)

* 초과 쓰레드들은 지정된 시간까지 작업을 하지 않고 대기하면 제거된다. 긴급한 경우에만 투입되는 것으로 이해할 수 있다.
* 참고로 초과 스레드가 작업을 처리할 때 마다 시간은 계속 초기화 된다. 작업 요청이 계속 들어온다면 긴급한 상황이 끝난 것이 아니다. 따라서 긴급한 상황이 끝날 때 까지는 초과 스레드를 살려두는 것이 많은 스레드를 사용해서 작업을 더 빨리 처리할 수 있다. 

#### Executor 전략 - 고정 풀 전략

* `ThreadPoolExecutor`를 사용하면 쓰레드 풀에 사용되는 숫자와 블로킹 큐 등 다양한 속성을 조절할 수 있다.
  * `corePoolSize` : 스레드 풀에서 관리되는 기본 스레드의 수
  * `maximumPoolSize` : 스레드 풀에서 관리되는 최대 스레드 수
  * `keepAliveTime` , `TimeUnit unit` : 기본 스레드 수를 초과해서 만들어진 스레드가 생존할 수 있는 대기 시간, 이 시간 동안 처리할 작업이 없다면 초과 스레드는 제거된다.
  * `BlockingQueue workQueue` : 작업을 보관할 블로킹 큐

#### `newSingleThreadPool()` 전략 - 단일 쓰레드 풀 전략

* 단일 쓰레드 풀 전략
* 쓰레드 풀에 기본 1개의 쓰레드만 사용한다.
* 큐 사이즈에 제한이 없다.
* 주로 간단히 사용하거나 테스트 용도로 사용한다.

#### `newFixedThreadPool(nThreads)` 전략 - 고정 쓰레드 풀 전략

* 쓰레드 풀에 `nThreads`만큼의 기본 쓰레드를 생성한다. 초과 쓰레드는 생성하지 않는다.
* 큐 사이즈에 제한이 없다.
* 쓰레드 수가 고정되어 있기 때문에 CPU, 메모리 리소스가 어느 정도 예측이 가능한 안정적인 방식이다.
* 하지만, 이 방식은 점진적인 사용자 증가 혹은 갑작스런 요청 증가가 발생하면 응답이 느려지거나 응답을 받지 못할 수 있다.

#### `newCachedThreadPool()` 전략 - 캐시 풀 전략

* 기본 쓰레드를 사용하지 않고, 60초 생존 주기를 가진 초과 쓰레드만 사용한다.
* 초과 쓰레드 수는 제한이 없다.
* 큐에 작업을 저장하지 않는다. 대신, 생산자 요청을 쓰레드 풀의 소비자 쓰레드가 직접 받아서 바로 처리한다.
* 모든 요청이 대기하지 않고 쓰레드가 바로바로 처리를 한다. 따라서 빠른 처리가 가능하다.
* 하지만 모든 기술에는 트레이드 오프가 있듯이 이 방식 역시 단점이 있다.
* CPU, 메모리를 유연하게 사용할 수 있다는 것이 장점이나 점진적인 사용자 증가로 인해 CPU 메모리 사용량 역시 자연스럽게 증가한다면 시스템 자원을 너무 많이 사용하게 되면서 시스템이 다운될 수 있고 갑작스런 요청의 증가로 인해 응답을 받지 못할 수 있다.

>❗고정 쓰레드 풀 전략은 서버 자원은 여유가 있는데, 사용자만 점점 느려지는 문제가 발생할 수 있다. 반면에 캐시 쓰레드 풀 전략은 서버 자원을 최대한 사용하려고 하지만, 서버가 감당할 수 있는 임계점을 넘는 순간 시스템이 다운될 수 있다.

#### Executor 전략 - 사용자 정의 풀 전략

* 상황1 : 점진적인 사용자 증가
* 상황2 : 갑작스런 요청 증가
```
일반: 일반적인 상황에는 CPU, 메모리 자원을 예측할 수 있도록 고정 크기의 스레드로 서비스를 안정적으로 운영한다.
긴급: 사용자의 요청이 갑자기 증가하면 긴급하게 스레드를 추가로 투입해서 작업을 빠르게 처리한다.
거절: 사용자의 요청이 폭증해서 긴급 대응도 어렵다면 사용자의 요청을 거절한다.
```
```java
// 100개의 기본 쓰레드
// 60초 생존 주기
// 추가로 긴급 대응 가능한 쓰레드 100개(200 - 100)
// 큐 사이즈 1000
ExecutorService es = new ThreadPoolExecutor(100, 200, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
```

#### Executor 예외 정책

* 생산자 소비자 문제를 실무에서 적용할 때 결국 소비자가 처리할 수 없을 정도로 생산 요청이 가득 차면 어떻게 할지를 정해야 한다.
* 이런 것을 위해 예외 정책이 필요하다.
* `ThreadPoolExecutor`는 작업을 거절하는 다양한 정책을 제공한다.
  * `AbortPolicy` : 새로운 작업을 제출할 때 `RejectedExecutionException`을 발생
  * `DiscardPolicy` : 새로운 작업을 조용히 버린다.
  * `CallerRunsPolicy` : 새로운 작업을 제출한 스레드가 대신해서 직접 작업을 실행
  * `사용자 정의` : 개발자가 직접 정의한 거절 정책을 사용
