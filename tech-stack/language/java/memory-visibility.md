# Java - Memory Visibility

#### volatile(메모리 가시성)

* CPU는 처리 성능을 개선하기 위해 중간에 캐시 메모리라는 것을 사용한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/1b090c9c-4673-4199-adfc-b4bbc31c2ac7.png)

* CPU 연산은 매우 빠르기 때문에 CPU 연산을 따라가려면 CPU 가까이에 매우 빠른 메모리가 필요한데 이것을 캐시 메모리라고 한다.
* 캐시 메모리는 CPU와 가까이 붙어 있고, 속도도 매우 빠른 메모리이다. 하지만 상대적으로 비싸기 때문에 큰 용량을 구성하기 어렵다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/32da758f-bd53-4a8f-89e0-cea9dea664fd.png)

* 각 쓰레드가 보유한 캐시 메모리의 값이 변화하는 것이지 메인 메모리에 바로 반영되는 것이 아니다.
* 캐시 메모리에 있는 값이 메인 메모리에 반영되는 시점은 알 수 없다.
* 메인 메모리에 반영된 상태를 쓰레드가 사용하는 캐시 메모리로 다시 불러와야 한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d4767f37-7b73-4471-ae69-1f23f7de2d12.png)

* 캐시 메모리를 메인 메모리에 반영하거나 메인 메모리 변경 내역을 캐시 메모리에 다시 불러오는 것 역시 언제 발생할지 알 수 없다.
* 주로 컨텍스트 스위칭 시 캐시 메모리도 함께 갱신이 된다.
* 이처럼 멀티 쓰레드 환경에서 한 쓰레드가 변경한 값이 다른 쓰레드에서 언제 보이는가에 대한 문제를 메모리 가시성 문제라고 한다.

#### volatile

* 캐시 메모리를 사용하면 CPU 처리 성능을 개선할 수 있으나 상대적으로 비용이 비싼 부분이 단점으로 꼽힌다.
* 자바에서는 메인 메모리에 직접 접근할 수 있도록 해주는 `volatile` 키워드가 있다.

```java
public class VolatileFlagMain {
	public static void main(String[] args) {

		MyTask myTask = new MyTask();
		Thread thread = new Thread(myTask, "task");
		thread.start();
		sleep(1000);

		myTask.flag = false;
		log("flag = " + myTask.flag + ", count = " + myTask.count + " in main()");
	}

	static class MyTask implements Runnable {

		volatile boolean flag = true;  // 쓰레드가 메인 메모리에 직접 접근
		volatile long count;           // 쓰레드가 메인 메모리에 직접 접근

		@Override
		public void run() {
			while (flag) {
				count++;
				if (count % 100_000_000 == 0) {
					log("flag = " + flag + ", count = " + count + ", in while()");
				}
			}
			log("flag = " + flag + ", count = " + count + " 종료");
		}
	}
}
```

> 실행 결과

```
21:18:17.473 [     task] flag = true, count = 100000000, in while()
21:18:17.666 [     task] flag = true, count = 200000000, in while()
21:18:17.848 [     task] flag = true, count = 300000000, in while()
21:18:18.034 [     task] flag = true, count = 400000000, in while()
21:18:18.225 [     task] flag = true, count = 500000000, in while()
21:18:18.259 [     main] flag = false, count = 517652502 in main()
21:18:18.259 [     task] flag = false, count = 517652502 종료
````

* 실행 결과를 조회해보면 main 쓰레드와 task 쓰레드가 종료된 시점에 count의 값이 같다는 것을 알 수 있다.
* 하지만 volatile 키워드는 성능 저하를 유발하니 정말 필요한 곳에만 사용을 하도록 해야 한다.

#### 자바 메모리 모델(Java Memory Model)

* JMM은 자바 프로그램이 어떻게 메모리에 접근하고 수정할 수 있는지를 규정하며, 특히 멀티 쓰레드 프로그래밍에서 쓰레드 간 상호 작용을 정의한다.

#### happens-before

* happens-before 관계는 자바 메모리 모델에서 쓰레드 간 작업 순서를 정의하는 개념이다.
