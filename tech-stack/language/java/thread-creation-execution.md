# Java - Thread Creation & Execution

#### 쓰레드

* 자바 메모리 구조(JVM, Java Virtual Machine)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d8d5ce93-f4da-4388-bfcc-3e0f00c8df18.png)

* 메서드 영역 : 클래스 정보, static 변수, 런타임 상수 풀 정보를 저장
* 스택 영역 : 쓰레드 별로 생성되는 개별 요청 저장(지역 변수, 메서드 호출 내역 등)
* 힙 영역 : 인스턴스가 포함되는 영역

#### 쓰레드 생성 방법1 - Thread 클래스 상속

* `run()` 메서드가 아니라 반드시 `start()` 메서드를 실행시켜야 한다.
* 그래야 별도의 쓰레드가 생성되면서 동작하게 된다.

```java
public class CustomThread extends Thread{

	@Override
	public void run() {
		System.out.println(Thread.currentThread().getName());
	}
}

// ...

customThread.start();
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/6d631c7c-8abc-4608-8d10-92e013449cfb.png)

* 새로운 쓰레드가 만들어지면서 개별 스택 영역이 생성되고 메서드를 실행했더니 스택 영역에 메서드 호출 내역 등이 저장되는 것을 볼 수 있다.
* 쓰레드 간 실행 순서는 보장하지 않는다. → 쓰레드는 동시에 실행되기 때문에 쓰레드 간 실행 순서는 얼마든지 달라질 수 있다.

>❗`start()` vs `run()`
> 
> * 쓰레드의 `start()` 메서드는 쓰레드에 개별 스택 공간을 할당하면서 쓰레드를 시작하는 특별한 메서드이다.
> * 생성한 쓰레드에서 `run()` 메서드를 실행해야 한다.
> * 따라서 별도의 쓰레드에서 재정의한 `run()` 메서드를 실행하려면 반드시 `start()` 메서드를 호출하도록 해야만 한다.

#### 데몬 쓰레드

* 사용자 쓰레드
  * 프로그램의 주요 작업을 수행한다.
  * 작업이 완료될 때까지 실행한다.
  * 모든 User 쓰레드가 종료되면 JVM도 종료된다.
* 데몬 쓰레드
  * 백그라운드에서 보조적인 작업을 수행한다.
  * 모든 User 쓰레드가 종료되면 데몬 쓰레드는 자동으로 종료된다.

>❗JVM은 데몬 쓰레드 실행 완료를 기다리지 않고 종료된다.

```java
public class DaemonThreadMain {

	public static void main(String[] args) {

		System.out.println(Thread.currentThread().getName());
		DaemonThread daemonThread = new DaemonThread();
		daemonThread.setDaemon(true);	// 데몬 쓰레드로 설정
		daemonThread.start();
		System.out.println(Thread.currentThread().getName());
	}

	static class DaemonThread extends Thread {

		@Override
		public void run() {
			System.out.println(Thread.currentThread().getName());
			try {
				Thread.sleep(10000);	// 10초를 기다린다.
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			System.out.println(Thread.currentThread().getName());
		}
	}
}
````

* 데몬 쓰레드 측에서 10초를 기다리는 부분이 있으나 실행하면 main 쓰레드가 먼저 종료가 되기 때문에 데몬 쓰레드 역시 종료가 된다.

#### 쓰레드 - Runnable

* 쓰레드를 만들 때는 Thread를 상속 받는 방법과 Runnable 인터페이스를 구현하는 방법이 있다.

```java
public class HelloRunnable implements Runnable {

	@Override
	public void run() {
		System.out.println(Thread.currentThread().getName());
	}
}
````

* Thread 클래스 상속 방식
  * 간단한 구현이 가능하다.
  * 자바는 다이아몬드 상속 문제로 인해 단일 상속만이 가능하다. 만약 쓰레드 클래스가 이미 다른 클래스를 상속받고 있는 경우라면 Thread 클래스를 상속받을 수 없다.
  * 인터페이스를 사용하는 방법에 비해 유연성이 떨어진다.
* Runnable 인터페이스 구현 방식
  * Runnable 인터페이스 방식은 다른 클래스를 상속받아도 문제없이 구현할 수 있다.
  * 쓰레드와 실행할 작업을 분리하여 코드 가독성을 높일 수 있다.
  * 여러 쓰레드가 동일한 Runnable 인터페이스 객체를 공유할 수 있어 자원 관리를 효율적으로 할 수 있다.
  * 다만 코드가 복잡해질 수 있다.

#### 정적 중첩 클래스를 활용한 쓰레드 생성하기

```java
public class InnerRunnableMainV1 {
	public static void main(String[] args) {

		log("main() start");

		InnerRunnable innerRunnable = new InnerRunnable();
		Thread thread = new Thread(innerRunnable);
		thread.start();

		log("main() end");
	}

	static class InnerRunnable implements Runnable {

		@Override
		public void run() {
			System.out.println(Thread.currentThread().getName());
		}
	}
}
```

#### 익명 클래스를 활용한 쓰레드 생성하기

```java
public class InnerRunnableMainV2 {
	public static void main(String[] args) {

		log("main() start");

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				System.out.println(Thread.currentThread().getName());
			}
		};
		Thread thread = new Thread(runnable);
		thread.start();

		log("main() end");
	}

}
```
