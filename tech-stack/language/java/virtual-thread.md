# Java - Virtual Thread

#### Virtual Thread 개념

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3c078786-38b2-479c-a0f6-837825fcd6ec.png)

* Heap 영역에 수많은 Virtual Thread를 할당해놓고, 플랫폼 쓰레드에 대상 Virtual Thread를 마운트/언마운트하여 컨텍스트 스위칭을 수행한다. 그렇기 때문에 컨텍스트 스위칭 비용이 작아질 수 밖에 없다.
* 쓰레드 크기와 컨텍스트 스위칭 비용이 많이 감소한 모델이기 때문에 Spring MVC/Tomcat 등의 모델이 Netty/WebFlux 모델에 비해 가진 단점이 많이 희석되었다.

#### Virtual Thread States

```java
    /*
     * Virtual thread state and transitions:
     *
     *      NEW -> STARTED         // Thread.start
     *  STARTED -> TERMINATED      // failed to start
     *  STARTED -> RUNNING         // first run
     *
     *  RUNNING -> PARKING         // Thread attempts to park
     *  PARKING -> PARKED          // cont.yield successful, thread is parked
     *  PARKING -> PINNED          // cont.yield failed, thread is pinned
     *
     *   PARKED -> RUNNABLE        // unpark or interrupted
     *   PINNED -> RUNNABLE        // unpark or interrupted
     *
     * RUNNABLE -> RUNNING         // continue execution
     *
     *  RUNNING -> YIELDING        // Thread.yield
     * YIELDING -> RUNNABLE        // yield successful
     * YIELDING -> RUNNING         // yield failed
     *
     *  RUNNING -> TERMINATED      // done
     */
    private static final int NEW      = 0;
    private static final int STARTED  = 1;
    private static final int RUNNABLE = 2;     // runnable-unmounted
    private static final int RUNNING  = 3;     // runnable-mounted
    private static final int PARKING  = 4;
    private static final int PARKED   = 5;     // unmounted
    private static final int PINNED   = 6;     // mounted
    private static final int YIELDING = 7;     // Thread.yield
    private static final int TERMINATED = 99;  // final state
```

* 다음과 같이 Virtual Thread의 상태에 따라서 플랫폼 쓰레드에 마운트/언마운트해 실행을 관리한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c9e9a429-3d2e-4706-a406-e6c216c3cd24.png)

* 플랫폼 쓰레드에 마운트/언마운트할 때에는 `park` 혹은 `unpark` 메서드를 사용한다.

```java
/**
 * Base class for virtual thread implementations.
 */
sealed abstract class BaseVirtualThread extends Thread
        permits VirtualThread, ThreadBuilders.BoundVirtualThread {

    /**
     * Initializes a virtual Thread.
     *
     * @param name thread name, can be null
     * @param characteristics thread characteristics
     * @param bound true when bound to an OS thread
     */
    BaseVirtualThread(String name, int characteristics, boolean bound) {
        super(name, characteristics, bound);
    }

    /**
     * Parks the current virtual thread until the parking permit is available or
     * the thread is interrupted.
     *
     * The behavior of this method when the current thread is not this thread
     * is not defined.
     */
    abstract void park();

    /**
     * Parks current virtual thread up to the given waiting time until the parking
     * permit is available or the thread is interrupted.
     *
     * The behavior of this method when the current thread is not this thread
     * is not defined.
     */
    abstract void parkNanos(long nanos);

    /**
     * Makes available the parking permit to the given this virtual thread.
     */
    abstract void unpark();
}
```

> [📚Virtual Thread](https://en.wikipedia.org/wiki/Virtual_thread)
>
> * **플랫폼 쓰레드** : OS 커널 쓰레드와 1:1로 대응한다. 즉, 자바에서 쓰레드를 하나 생성하면 실제 운영체제(OS)의 리소스도 하나 점유하게 된다. 이 OS 쓰레드는 생성 비용이 비싸고 메모리를 많이 사용한다. 수천 개만 생성해도 메모리 부족이나 컨텍스트 스위칭 오버헤드로 인해 서버 성능이 급격히 떨어진다.
> * **가상 쓰레드** : OS 커널 쓰레드와 직접 대응되지 않고, 자바 런타임이 관리하는 논리적 단위이다. 메모리 점유가 매우 적고 수십만 개에서 수백만 개를 동시에 생성해도 시스템에 큰 무리가 없다. 가상 쓰레드가 실제로 코드를 실행하려면 반드시 플랫폼 쓰레드에 마운트되어야만 한다.
> * **캐리어 쓰레드** : 가상 쓰레드는 스스로 CPU를 사용할 수 없다. 가상 쓰레드는 플랫폼 쓰레드에 마운트되어야만 실제로 사용이 가능한데 이 때, 가상 쓰레드가 할당되어 실행되는 플랫폼 쓰레드를 캐리어 쓰레드라고 부른다.
>
> **1. Work-Stealing Scheduler**
> * 가상 쓰레드를 관리하는 것은 JVM 내부의 ForkJoinPool이다. 이 스케줄러가 여러 개의 캐리어 쓰레드를 미리 준비해두고 실행 대기 중인 가상 쓰레드들을 적절히 배분해 처리한다.
>
> **2. Blocking**
> * 기존 플랫폼 쓰레드는 I/O 작업이 발생하면 OS 쓰레드 자체가 멈춘다.
> * 그동안 캐리어 쓰레드는 다른 가상 쓰레드의 작업을 처리하러 떠난다. 덕분에 쓰레드 낭비가 거의 없다.

#### Virtual Thread Pinning

* Virtual Thread는 JVM이 자체적으로 Virtual Thread를 스케줄링하고 컨텍스트 스위칭 비용을 줄여 효율적으로 운영할 수 있다는 장점이 있다.
* 하지만 Virtual Thread가 플랫폼 쓰레드에 고정되어 장점을 활용할 수 없는 경우가 있다.
* Virtual Thread 내에서 `synchronized` 동기화를 사용하거나 JNI를 통해 네이티브 메서드를 사용하는 경우이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/79300ca2-ef92-457c-8e9d-895e857696bd.png)

#### SpringBoot에서의 Virtual Thread

```yml
spring:
  threads:
    virtual:
      enabled: true
```

> 참고한 문서 정리
> 
> * **[Java의 미래, Virtual Thread](https://techblog.woowahan.com/15398/#toc-7)**
> * **[Virtual Thread 기본 개념](https://d2.naver.com/helloworld/1203723)**
> * **[Java 가상 스레드, 깊이 있는 소스 코드 분석과 작동 원리 1편 - 생성과 시작](https://techblog.lycorp.co.jp/ko/about-java-virtual-thread-1)**
> * **[Java 가상 스레드, 깊이 있는 소스 코드 분석과 작동 원리 2편 - 컨텍스트 스위칭](https://techblog.lycorp.co.jp/ko/about-java-virtual-thread-2)**
> * **[Java 가상 스레드, 깊이 있는 소스 코드 분석과 작동 원리 3편 - 고정 이슈와 한계](https://techblog.lycorp.co.jp/ko/about-java-virtual-thread-3)**
> * **[JDK 21](https://github.com/openjdk/jdk21/)**
> * **[Java Virtual Threads Tech & Media Labs](https://jenkov.com/tutorials/java-concurrency/java-virtual-threads.html)**
> * **[What is JVM?](https://usemynotes.com/what-is-jvm-jit/)**
