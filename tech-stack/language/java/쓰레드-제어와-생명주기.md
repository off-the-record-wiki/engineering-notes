# Java - Thread Control & LifeCycle

#### 쓰레드 기본 정보

* Thread 클래스는 쓰레드를 생성하고 관리하는 기능을 제공한다.

```java
public class ThreadInfoMain {

	public static void main(String[] args) {

		Thread mainThread = Thread.currentThread();
		log("thread = " + mainThread);
		log("thread.threadId = " + mainThread.getId());
		log("thread.getName = " + mainThread.getName());
		log("thread.getPriority = " + mainThread.getPriority());
		log("thread.getThreadGroup = " + mainThread.getThreadGroup());
		log("thread.getState = " + mainThread.getState());

		Thread thread = new Thread(new HelloRunnable(), "thread");
		log("thread = " + thread);
		log("thread.threadId = " + thread.getId());
		log("thread.getName = " + thread.getName());
		log("thread.getPriority = " + thread.getPriority());
		log("thread.getThreadGroup = " + thread.getThreadGroup());
		log("thread.getState = " + thread.getState());
	}
}
```

* 쓰레드가 제공하는 정보
  * 쓰레드 이름
  * 쓰레드 객체 정보
  * 쓰레드 ID
  * 쓰레드 우선순위
  * 쓰레드 그룹
  * 쓰레드 상태 

#### 쓰레드 생명 주기

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/6a245ecb-bdd3-40da-99f5-e9f6cac59976.png)

* 쓰레드의 상태
  * NEW(새로운 상태) : 쓰레드가 생성은 되었으나 아직 실행되지 않은 상태
  * Runnable(실행 가능 상태) : 쓰레드가 실행 중이거나 실행될 준비가 된 상태
  * 일시 중지 상태들
    * Blocked(차단 상태) : 쓰레드가 동기화 락을 기다리는 상태
    * Waiting(대기 상태) : 쓰레드가 무기한으로 다른 쓰레드의 작업을 기다리는 상태
    * Timed Waiting(시간 제한 대기 상태) : 쓰레드가 일정 시간 동안 다른 쓰레드의 작업을 기다리는 상태
  * Terminated(종료 상태) : 쓰레드 실행이 완료된 상태

#### NEW

* 쓰레드가 생성되고 아직 시작되지 않은 사태이다.
* `start()` 메서드를 아직 호출하지 않은 상태이다.

#### Runnable

* 쓰레드가 실행될 준비가 된 상태이다.
* `start()` 메서드가 호출되면 NEW → Runnable 상태로 들어간다.
* Runnable 상태에 있는 모든 쓰레드가 동시에 실행되는 것이 아니라 운영체제 스케줄러가 각 쓰레드에 CPU 시간을 할당하여 실행하기 때문에 Runnable 상태에 있는 쓰레드는 스케줄러 실행 대기열에 포함되어 있다가 차례로 CPU에서 실행된다.
* 운영체제 스케줄러의 실행 대기열에 있든, CPU에서 실제 실행되고 있든 모두 Runnable 상태로 본다.

#### Blocked

* 쓰레드가 다른 쓰레드에 의해 동기화 락을 얻기 위해 기다리는 상태이다.(예시 : 자바의 synchronized 키워드)
* 만약 하나의 쓰레드가 synchronized 블럭에 들어갔다면 나머지 쓰레드들은 lock을 획득하지 못해 대기하는 상태가 되는데 그 상태가 바로 Blocked 이다.

#### Waiting

* 쓰레드가 다른 쓰레드의 특정 작업이 완료될 때까지 무기한 기다리는 상태이다.
* `wait()`, `join()` 메서드가 호출될 때 이 상태가 된다.

#### Timed Waiting

* 쓰레드가 특정 시간동안 다른 쓰레드의 작업이 완료되기를 기다리는 상태이다.
* `sleep(millis)`, `wait(millis)`, `join(millis)` 메서드가 호출될 때 이 상태가 된다.
* 주어진 시간이 경과하거나 다른 쓰레드가 해당 쓰레드를 깨우면 이 상태에서 벗어난다.

#### Terminated

* 쓰레드 실행이 완료된 상태이다.
* 쓰레드가 정상적으로 종료되거나 예외가 발생한 경우 이 상태로 들어간다.
* 쓰레드는 한 번 종료되면 다시 시작할 수 없다.

```java
public class ThreadStateMain {
	public static void main(String[] args) throws InterruptedException {
		Thread thread = new Thread(new MyRunnable(), "myThread");
		log("thread.state1 = " + thread.getState());  // 1. NEW
		log("thread.start");
		thread.start();            
		Thread.sleep(1000);
		log("thread.state3 = " + thread.getState());  // 3. TIMED_WAITING
		log("sleep end");
		Thread.sleep(4000);
		log("thread.state5 = " + thread.getState());  // 5. TERMINATED
	}

	static class MyRunnable implements Runnable {

		@Override
		public void run() {
			try {
				log("start");
				log("thread.state2 = " + Thread.currentThread().getState()); // 2. RUNNABLE
				log("thread.sleep");
				Thread.sleep(3000);
				log("sleep end");
				log("thread.state4 = " + Thread.currentThread().getState()); // 4. RUNNABLE
				log("end");
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
````

> 실행 결과 분석

```
16:46:53.888 [     main] thread.state1 = NEW
16:46:53.889 [     main] thread.start
16:46:53.890 [ myThread] start
16:46:53.890 [ myThread] thread.state2 = RUNNABLE
16:46:53.890 [ myThread] thread.sleep
16:46:54.895 [     main] thread.state3 = TIMED_WAITING
16:46:54.896 [     main] sleep end
16:46:56.895 [ myThread] sleep end
16:46:56.896 [ myThread] thread.state4 = RUNNABLE
16:46:56.896 [ myThread] end
16:46:58.901 [     main] thread.state5 = TERMINATED
```

#### join

```java
public class ThreadSumMain {

	public static void main(String[] args) throws InterruptedException {
		HelloThread task1 = new HelloThread(1, 50);
		HelloThread task2 = new HelloThread(51, 100);

		Thread thread1 = new Thread(task1, "thread1");
		Thread thread2 = new Thread(task2, "thread2");

		thread1.start();
		thread2.start();

		//thread1.join();
		//thread2.join();

                thread1.join(1000);
                thread2.join(1000);

		log("task1.result = " + task1.result);
		log("task2.result = " + task2.result);

		int sum = task1.result + task2.result;
		log("sum = " + sum);
	}

	static class HelloThread implements Runnable {

		int start;
		int end;
		int result = 0;

		public HelloThread(int start, int end) {
			this.start = start;
			this.end = end;
		}

		@Override
		public void run() {
			sleep(2000);
			int sum = 0;
			for (int i = start; i <= end; i++) {
				sum += i;
			}
			result = sum;
		}
	}
}
```

* `join()` : 다른 쓰레드가 완료될 때까지 무기한 기다린다.
* `join(millis)` : 다른 쓰레드의 작업을 일정 시간 동안만 기다린다.

#### 인터럽트

* 인터럽트를 사용하면 WAITING, TIMED_WAITING 같은 대기 상태 쓰레드를 직접 깨워서 작동하는 Runnable 상태로 만들 수 있다.

```java
public class ThreadStopMain {

	public static void main(String[] args) {
		MyTask myTask = new MyTask();
		Thread thread = new Thread(myTask, "task");
		thread.start();

		sleep(4000);
		thread.interrupt();
		log("인터럽트 상태 1 = " + thread.isInterrupted());
	}

	static class MyTask implements Runnable {

		@Override
		public void run() {
			try {
				while (true) {
					log("작업 중");
					Thread.sleep(3000);
				}
			} catch (InterruptedException e) {
				log("인터럽트 상태 2 = " + Thread.currentThread().isInterrupted());
				log("인터럽트 메시지 = " + e.getMessage());
				log("상태 = " + Thread.currentThread().getState());
				log("자원 정리");
				log("작업 종료");
			}
		}
	}
}
```

* 여기서 MyTask 쓰레드는 작업 후 3초동안 대기 상태로 들어가게 된다.
* main 쓰레드에서 MyTask 쓰레드를 실행한 후 4초 뒤에 인터럽트를 걸게 된다.
* MyTask 쓰레드의 경우 4초 시점에는 대기 상태에 진입한 상태로 main 쓰레드에서 인터럽트를 걸면 대기 상태에서 깨어나 실행 상태로 전환이 된다.
* 쓰레드의 인터럽트 상태를 확인하려면 `isInterrupted()`메서드를 사용하면 된다.
* 하지만 직접 체크해서 사용할 때는 `Thread.interrupted()`메서드를 사용하면 된다.
  * 쓰레드가 인터럽트 상태라면 true를 반환하고 해당 쓰레드 인터럽트 상태를 false로 변경한다.
  * 쓰레드가 인터럽트 상태가 아니라면 false를 반환하고 해당 쓰레드 인터럽트 상태를 변경하지 않는다.

#### yield

* 다른 쓰레드에게 실행 기회를 양보하고 싶다면 `yield()`메서드를 사용한다.
