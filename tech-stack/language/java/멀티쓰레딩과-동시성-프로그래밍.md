# Java ‐ Multi Threading & Concurrent Programming

#### 왜 멀티쓰레딩이 필요한지?

* 작업을 순서대로 처리하게 되면 하나가 끝날 때까지 다음 작업을 하지 못하게 된다.
* 비효율적인 자원 사용 방식 : `sleep()` 동안 CPU는 작업을 중단하고 대기한다.
* 이와 같이 비효율적인 자원 사용 방식을 타개하기 위해 동시에 여러 작업을 처리하고 싶을 때 멀티쓰레딩이 필요하다.

#### 멀티쓰레딩

* 하나의 프로그램에서 여러 작업(쓰레드)을 동시에 실행하는 것
* 대기 시간이 많은 작업을 병렬로 처리할 수 있어 성능 향상 가능 - CPU가 놀지 않고 다른 작업을 동시에 처리할 수 있다.
* 특히 네트워크, 파일 I/O, UI 등에서 주로 사용된다.

#### 멀티쓰레딩을 구현하는 방법 1 - Thread 클래스 상속

* `start()` 메서드를 호출하면 새로운 스레드가 실행된다.
* `run()`을 직접 호출하면 멀티 스레딩이 되지 않으므로 반드시 `start()`를 호출해야한다.

```java
class MyThread extends Thread {
    private String name;
    
    public MyThread(String name) {
        this.name = name;
    }
 
    @Override
    public void run() {
        try {
            Thread.sleep(1000); // 1초 대기
            System.out.println(name + " 완료");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class Main {
    public static void main(String[] args) {
    Thread t1 = new MyThread("작업 1");
    Thread t2 = new MyThread("작업 2");
    Thread t3 = new MyThread("작업 3");
 
    t1.start();
    t2.start();
    t3.start();
    }
}
```

#### 멀티쓰레딩을 구현하는 방법 2 - Runnable 인터페이스를 구현하는 방법

* Thread 상속 방식보다 유연하며, 다른 클래스도 동시에 상속 가능
* Thread를 상속받지 않고, Runnable 인터페이스를 구현
* Thread 객체를 생성할 때 Runnable 객체를 전달하여 실행

```java
class MyRunnable implements Runnable {
    private String name;
    
    public MyRunnable(String name) {
        this.name = name;
    }
 
    @Override
    public void run() {
        try {
            Thread.sleep(1000); // 1초 대기
            System.out.println(name + "완료");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Thread t1 = new Thread(new MyRunnable("작업 1"));
        Thread t2 = new Thread(new MyRunnable("작업 2"));
        Thread t3 = new Thread(new MyRunnable("작업 3"));
 
        t1.start();
        t2.start();
        t3.start();
    }
}
```

#### Runnable 인터페이스를 구현하는 방법 3 - Executors 프레임워크 사용

* ExecutorService를 사용하여 스레드를 효율적으로 관리할 수 있다.
* 스레드 개수를 자동으로 조절하며, 스레드 풀(Thread Pool) 기능을 제공한다.
* 직접 Thread를 생성하는 것보다 성능과 안정성이 뛰어나다.

```java
public class Main {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(3); // 3개 스레드 사용
        Runnable task = () -> {
            try {
                Thread.sleep(1000); // 1초 대기
                System.out.println("스레드 실행: " + Thread.currentThread().getName());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
 
        for (int i = 0; i < 5; i++) {
            executor.execute(task); // 스레드 풀에서 스레드 실행
        }
        executor.shutdown(); // 스레드 풀 종료
    }
} 
```

> [📚Thread Pool](https://en.wikipedia.org/wiki/Thread_pool)
>
> * 쓰레드 풀이란 병행 실행을 달성하기 위한 소프트웨어 디자인 패턴이다. 병행 실행을 위해 할당될 테스크를 기다리는 여러 쓰레드를 유지한다.
> * 쓰레드 풀을 유지함으로써 성능을 향상시키고 짧은 수명 테스크를 위해 쓰레드를 빈번하게 생성하고 소멸하는 데 따른 실행 지연을 방지한다.
> * 쓰레드 풀의 크기는 테스크 실행을 위해 예비로 유지되는 쓰레드의 수이다. 일반적으로 애플리케이션의 튜닝 가능한 매개변수이며, 프로그램 성능을 최적화하기 위해 조정된다. 
> * 각 테스트에 대해 새로운 쓰레드를 생성하는 것보다 쓰레드 풀의 한 가지 이점은 쓰레드 생성 및 파괴 오버헤드가 풀의 초기 생성으로 제한되어 더 나은 성능과 더 나은 시스템 안정성을 가져올 수 있다는 것이다. 그러나 과유불급이란 말이 있듯이, 예비 쓰레드 수가 과도하면 메모리가 낭비되고 실행 가능한 쓰레드 간의 컨텍스트 스위칭이 오히려 빈번해져 성능 저하를 초래한다.
> * 너무 많은 쓰레드를 생성하면 리소스가 낭비되고 사용되지 않는 쓰레드를 생성하는 데 시간이 소요되고 반대로 너무 많은 쓰레드를 파괴하면 나중에 다시 생성할 때 많은 시간이 필요하다.
> * 너무 느리게 쓰레드를 생성하면 긴 대기 시간으로 인해 클라이언트 성능이 저하될 수 있고 너무 느리게 쓰레드를 파괴하면 다른 프로세스의 리소스가 부족해질 수 있다.
