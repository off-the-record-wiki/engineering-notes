# (Effective Java Item 81) Java - wait와 notify는 동시성 유틸리티를 애용하라

#### `wait()`와 `notify()`

* `wait()`와 `notify()`는 쓰레드의 상태 제어를 위한 메서드이다.
  * `wait()` : 가지고 있던 고유 락을 해제하고 쓰레드를 잠들게 하는 역할을 하는 메서드
  * `notify()` : 잠들어 있던 쓰레드 중 임의로 하나를 골라 꺠우는 역할을 하는 메서드
```java
public class ThreadB extends Thread {
    int total = 0; // 누적 값을 저장할 변수 (기존 코드 보완)

    // 해당 쓰레드가 실행되면 자기 자신의 모니터링 락을 획득
    // 5번 반복하면서 0.5초씩 쉬면서 total에 값을 누적
    // 그후에 notify()메소드를 호출하여 wait하고 있는 쓰레드를 깨움
    @Override
    public void run() {
        synchronized (this) {
            for (int i = 0; i < 5; i++) {
                System.out.println(i + "를 더합니다.");
                total += i;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            notify(); // wait하고 있는 쓰레드를 깨움
        }
    }
}
```
```java
public class ThreadA {
    public static void main(String[] args) {
        // 앞에서 만든 쓰레드 B를 만든 후 start 
        // 해당 쓰레드가 실행되면, 해당 쓰레드는 run메소드 안에서 자신의 모니터링 락을 획득
        ThreadB b = new ThreadB();
        b.start();

        // b에 대하여 동기화 블럭을 설정
        // 만약 main쓰레드가 아래의 블록을 위의 Thread보다 먼저 실행되었다면 wait를 하게 되면서 모니터링 락을 놓고 대기       
        synchronized (b) {
            try {
                // b.wait()메소드를 호출.
                // 메인쓰레드는 정지
                // ThreadB가 5번 값을 더한 후 notify를 호출하게 되면 wait에서 깨어남
                System.out.println("b가 완료될때까지 기다립니다.");
                b.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 깨어난 후 결과를 출력
            System.out.println("Total is: " + b.total);
        }
    }
}
```
* `wait()`과 `notify()`는 올바르게 사용하기가 위와 같이 아주 까다롭기 때문에 고수준 동시성 유틸리티를 사용하는 것이 좋다.

#### 고수준 동시성 유틸리티 1 - 동시성 컬렉션

* 표준 컬렉션 + 동시성 = 동시성 컬렉션
* 동시성 컬렉션의 동시성을 무력화하는 것은 불가능하며, 외부에서 락을 걸면 오히려 속도가 느려진다. 동시성을 무력화하지 못하므로 여러 메서드를 원자적으로 묶어 호출할 수도 없다.
* 상태 의존적 메서드 - 여러 기본 동작들을 하나로 묶는 상태 의존적 메서드가 추가(ex. `putIfAbsent()`)
* `Collections.synchronized~` - `Collections`에서 제공해주는 `synchronized~()`를 사용하여 동기화한 컬렉션을 만드는 것보다 동시성 컬렉션을 사용하는 것이 성능적으로 좋다.

#### 고수준 동시성 유틸리티 2 - 동기화 장치

* 컬렉션 인터페이스 중 일부는 작업이 성공적으로 완료될 때까지 기다리도록 확장되었다.
```java
public interface BlockingQueue<E> extends Queue<E> {
		/**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @return the head of this queue
     * @throws InterruptedException if interrupted while waiting
     */
    E take() throws InterruptedException;
}
```
* `BlockingQueue`의 `take()` 메서드는 큐의 원소를 꺼내는 역할을 하는데, 만약 큐가 비어있다면 새로운 원소가 추가될 때까지 기다린다.

#### 동기화 장치의 종류 1 - `CountDownLatch`, `Semaphore`

* 하나 이상의 쓰레드가 또 다른 하나 이상의 쓰레드 작업이 끝날 때까지 기다리게 한다.
* 생성할 때 `int`값을 받는데, 이 값이 `countDown()`을 몇 번 호출해야 대기 중인 쓰레드를 꺠우는지 결정한다.

```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CountDownLatchTest {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        try {
            long result = time(executorService, 3, () -> System.out.println("hello"));
            System.out.println("총 걸린 시간 : " + result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    public static long time(Executor executor, int concurrency, Runnable action) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(concurrency);

        for (int i = 0; i < concurrency; i++) {
            executor.execute(() -> {
                ready.countDown(); // 타이머에게 준비가 됐음을 알린다.
                try {
                    // 모든 작업자 스레드가 준비될 때까지 기다린다.
                    start.await();
                    action.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // 타이머에게 작업을 마쳤음을 알린다.
                    done.countDown();
                }
            });
        }

        ready.await(); // 모든 작업자가 준비될 때까지 기다린다.
        long startNanos = System.nanoTime();
        start.countDown(); // 작업자들을 깨운다.
        done.await(); // 모든 작업자가 일을 끝마치기를 기다린다.
        return System.nanoTime() - startNanos;
    }
}
```

#### 그럼에도 불구하고 `wait()`와 `notify()` 메서드를 사용해야하는 상황이라면?

* 반드시 동기화 영역 안에서 사용해야 하며, 항상 반복문 안에서 사용해야 한다.

```java
synchronized (obj) {
    while (조건이 충족되지 않았다) {
        obj.wait(); // 락을 놓고, 깨어나면 다시 잡는다.
    }

    ... // 조건이 충족됐을 때의 동작을 수행한다.
}
```
* 반복문 밖에서 절대 호출하면 안 된다. 또한 대기 전에 조건을 검사해 조건을 이미 충족했다면 `wait()`를 호출하지 않게 해야 하는데, 이는 응답 불가 상태를 예방하기 위해서이다.
* 한편 대기한 이후 조건을 검사해 조건을 충족하지 않았을 때, 다시 대기하게 하는 경우도 있는데 이는 조건이 만족되지 않아도 쓰레드가 깨어날 수 있는 상황이 몇 가지 있기 때문이다.
* 일반적으로 `notify()`보다 `notifyAll()`이 안전하다.
