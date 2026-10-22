# (Effective Java Item 84) Java - 프로그램의 동작을 쓰레드 스케줄러에 기대지 말라

#### 쓰레드 스케줄러

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8bbfa8aa-bf86-458c-aaf9-968ab16fb3df.png)

* 쓰레드 스케줄러란, 여러 쓰레드가 실행 중이면 운영체제의 쓰레드 스케줄러가 어떤 쓰레드를 얼마나 오래 실행할지 정하는 역할을 한다.
* 쓰레드 스케줄러는 쓰레드의 실행 시점을 관리하는 역할을 한다.
  * 언제 실행 가능한 상태로 움직여야할지
  * 언제 실행중인 상태를 종료해야할지
  * 종료했을때 쓰레드를 어디로 보내야할지
* 사용자가 제어할 수 없다.
* 제한된 자원을 여러 프로세스가 효율적으로 사용하도록 다양한 정책을 가지고 할당한다.

#### 실행 가능한 쓰레드 수를 적게 유지하기 - 당장 처리해야할 작업이 없다면 실행하지 않는다.

* 쓰레드 풀 크기를 적절히 설정하고 작업은 짧게 유지해야 한다.
* 단, 너무 짧으면 오히려 작업 분배에 부담이 가서 성능이 저하될 수 있다.
* 바쁜 대기 상태가 되면 안된다.

```java
// 바쁜 대기 버전 CountDownLatch 구현
public class SlowCountDownLatch {
    private int count;

    public SlowCountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException(count + " < 0");
        }
        this.count = count;
    }

    public void await() { // 바쁜 대기 중 (so bad ~~~)
        while (true) {
            synchronized (this) {
                if (count == 0) {
                    return;
                }
            }
        }
    }

    public synchronized void countDown() {
        if (count != 0) {
            count--;
        }
    }
}
```

#### 지양해야 하는 것 1 - `Thread.yield()`

* `yield()`란, 다른 쓰레드에게 실행을 양보하는 메서드이다.
* 이것을 지양하는 이유는 오히려 성능이 나빠질 수 있기 때문이다. 추가로 테스트할 수단도 없다.

#### 지양해야 하는 것 2 - `Thread Priority`

* 이미 잘 동작하는 프로그램의 서비스 품질을 높이기 위해서 드물게 쓰일 수는 있다.
* 그러나 거의 동작하지 않는 프로그램을 고치기 위해 사용해선 안 된다.
* 쓰레드 우선순위는 이식성이 가장 나쁜 특성에 속한다.
