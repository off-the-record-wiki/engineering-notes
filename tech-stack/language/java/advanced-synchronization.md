# Java - Advanced Synchronization

#### LockSupport(저수준)

* **synchronized** 단점 정리
  * 무한 대기 : BLOCKED 상태 쓰레드는 락을 획득한 쓰레드가 락을 해제할 때까지 무한 대기한다.
    * 특정 시간까지만 대기하는 타임아웃X
    * 중간에 인터럽트 발생 X
  * 공정성 : 락이 해제된 후 BLOCKED 상태 쓰레드 중에서 어떤 쓰레드가 락을 획득했는지 알 수 없다. 최악의 경우 특정 쓰레드가 너무 오랜기간 동안 락을 획득하지 못할 수 있다.
>❗이런 문제를 해결하기 위해 동시성 문제 해결을 위한 라이브러리 패키지가 추가되었다.<br>
>❗LockSupport를 사용하면 synchronized의 가장 큰 단점인 무한 대기 문제를 해결할 수 있다.

#### ReentrantLock

```java
/**
 * Creates an instance of {@code ReentrantLock}.    
 * This is equivalent to using {@code ReentrantLock(false)}.
 */
public ReentrantLock() {
    sync = new NonfairSync();
}

/**
 * Creates an instance of {@code ReentrantLock} with the
 * given fairness policy.
 *
 * @param fair {@code true} if this lock should use a fair ordering policy
 */
public ReentrantLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
}
```

* 비공정모드(Non-fair mode)
  * 비공정모드는 `ReentrantLock`의 기본 모드이다. 이 모드에서는 락을 먼저 요청한 쓰레드가 먼저 획득한다는 보장이 없다.
  * 락을 해제할 때 대기 중인 쓰레드 중 아무나 락을 획득할 수 있다.
  * 이는 락을 빨리 획득할 수 있지만, 특정 쓰레드가 장기간 락을 획득하지 못할 수도 있다.
  * 성능 특징 : 락을 획득하는 속도가 빠르다, 새로운 쓰레드가 기존 쓰레드보다 먼저 락을 선점할 수 있다, 특정 쓰레드가 계속해서 락을 획득하지 못할 수 있다.
* 공정모드(Fair mode)
  * 생성자에서 true를 전달하면 된다.
  * 공정 모드는 락을 요청한 순서대로 쓰레드가 락을 획득할 수 있다.
  * 이는 먼저 대기한 쓰레드가 먼저 락을 획득하게 되면서 쓰레드 간의 공정성을 보장한다. 그러나 이로 인해 성능이 저하될 수 있다.
  * 성능 특징 : 대기 큐에서 먼저 대기한 쓰레드가 락을 먼저 획득한다, 모든 쓰레드가 언젠가는 락을 획득할 수 있도록 보장한다, 락을 획득하는 속도가 느려질 수 있다.

>❗비공정 모드는 성능을 중시하고 쓰레드가 락을 빨리 획득할 수 있지만 특정 쓰레드가 계속해서 락을 획득하지 못할 수 있다.<br>
>❗공정 모드는 쓰레드가 락을 획득하는 순서를 보장하여 공정성을 중시하지만 성능이 저하될 수 있다.

#### ReentrantLock 예제

```java
public class BankAccount {

	private int balance;
	private final Lock lock = new ReentrantLock();

	public BankAccount(int balance) {
		this.balance = balance;
	}

	public boolean withdraw(int amount) throws InterruptedException {
		lock.lock();	// Reentrant Lock으로 락을 걸기

		try {
			if (balance < amount) {
				return false;
			}

			Thread.sleep(1000);
			balance -= amount;
		} finally {
			lock.unlock();	// 락 해제
		}
		return true;
	}

	public int getBalance() {
		return balance;
	}
}
```

* ReentrantLock이 제공하는 락은 객체 내부의 모니터 락이 아니다.
* ReentrantLock 내부에는 락과 락을 얻지 못해 대기하는 쓰레드를 관리하는 대기 큐가 존재한다.

#### ReentrantLock 대기 중단

```java
/**     
 * @param timeout the time to wait for the lock
 * @param unit the time unit of the timeout argument
 * @return {@code true} if the lock was free and was acquired by the
 *         current thread, or the lock was already held by the current
 *         thread; and {@code false} if the waiting time elapsed before
 *         the lock could be acquired
 * @throws InterruptedException if the current thread is interrupted
 * @throws NullPointerException if the time unit is null
 */
public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
    return sync.tryLockNanos(unit.toNanos(timeout));
}

/**
 * Performs non-fair tryLock.
 */
@ReservedStackAccess
final boolean tryLock() {
    Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (compareAndSetState(0, 1)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    } else if (getExclusiveOwnerThread() == current) {
        if (++c < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        setState(c);
        return true;
    }
    return false;
}
```

* ReentrantLock을 사용하면 락을 무한 대기하지 않고 중간에 빠져나오는 것이 가능하다.
* 심지어 락을 얻을 수 없다면 기다리지 않고 즉시 빠져나오는 것도 가능하다.
* `boolean tryLock()` : 락 획득을 시도하고 즉시 성공 여부를 반환한다. 만약 다른 쓰레드가 이미 락을 획득했다면 false를 반환하고 그렇지 않으면 true를 반환한다.
* `boolean tryLock(long time, TimeUnit unit)` : 주어진 시간 동안 락 획득을 시도한다. 주어진 시간 안에 락을 획득했다면 true를 반환하고 주어진 시간이 지나도 락을 획득하지 못한 경우 false를 반환한다. 이 메서드는 대기 중 인터럽트가 발생하면 `InterruptedException`이 발생하고 락 획득을 포기한다.
