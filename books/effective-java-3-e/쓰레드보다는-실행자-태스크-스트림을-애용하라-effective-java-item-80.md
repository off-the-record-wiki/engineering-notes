# (Effective Java Item 80) Java - 쓰레드보다는 실행자, 태스크, 스트림을 애용하라

#### Executor Framework

* 직접 사용자가 큐를 구현하기보다 다음과 같이 큐를 선언하면 만들어진다.
```java
ExecutorService exec = Executors.newSingleThreadExecutor();
```
* 큐에 태스크 실행을 명령하려면 다음과 같이 작성한다.
```java
exec.execute(runnable);
```
* 큐의 종료를 원한다면 다음과 같이 작성한다.
```java
exec.shutdown();
```

#### 작업(Task)과 수행원(Executor) 분리

* 태스크(Task) : `Runnable`나 `Callable`을 의미한다. 쓰레드 개수가 몇 개인지, CPU 코어가 몇 개인지 몰라도 되는 순수한 작업 그 자체이다.
* 실행자(Executor) : `ExecutorService`를 말한다. 태스크를 던져주면 자신이 관리하는 쓰레드 풀에서 쓰레드를 하나 꺼내엇 작업을 수행하는 주체이다.

#### 왜 `new Thread()`를 사용하지 말라는건지?

* Executor Framework를 사용하면 Low Level Thread를 직접 제어할 때 만나는 고질적인 문제들이 해결되기 때문이다.
  * 반환값의 유무 : `Runnable`은 실행하고 나면 끝이라 작업 결과를 받아오기가 극도로 까다로웠다. 반면 `Executor`와 `Callable`을 쓰면 `Future` 객체를 통해 작업이 끝날 때까지 기다렸다가 결과값을 안전하게 받아올 수 있다.
  * 쓰레드 생성 비용 방지 : 쓰레드는 생성 비용이 무척 큰 자원이다. `new Thread()`를 남발하면 메모리가 부족하거나 서버가 뻗을 수 있지만 실행자는 내부적으로 쓰레드 풀을 사용해 만들어둔 쓰레드를 안전하게 재사용한다.
  * 세련된 작업 관리 : 작업 도중에 특정 태스크를 취소하거나 여러 작업 중 가장 먼저 끝난 녀석의 결과만 취소하고 가져오거나 정해진 시간 동안만 기다리는 타임아웃 처리가 아주 쉽게 가능하다.

#### 결론

* 개발자가 직접 큐를 생성하지 말자. 또한 쓰레드를 직접 다루려고도 하지 말자.
