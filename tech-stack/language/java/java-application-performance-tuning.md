# Java ‐ Java Application Performance Tuning

#### JMX 설치 및 리소스 모니터링 방법

>❗Intellij - VisualVM 연동

(1). Intellij MarketPlace에서 VisualVM Install

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/70364f8a-4a52-48f0-951c-2f88b3f179e5.png)

(2). 플러그인 설치 후 JDK Path와 VisualVM을 설치한 디렉토리로 간 후 visualvm 프로그램 경로 입력

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/741d86e4-ac55-4f75-a266-b4c86bd38b58.png)

* **Indicator 1. Memory(메모리)**
  * Heap Memory Usage 
  * Non-Heap Memory Usage
* **Indicator 2. Garbage Collection(가비지 컬렉션)**
  * Collection Count   
  * Collection Time
* **Indicator 3. Threads(쓰레드)**
  * Thread Count
  * Deadlocked Threads
  * Thread States
* **Indicator 4. CPU & System(CPU 및 시스템)**
  * Process CPU Load
  * Open File Descriptor Count

#### static의 잘못된 사용법 정리

* static 영역은 필요한 경우에만 사용하고 불필요한 경우라면 클래스를 호출하지 않도록 처리해야 한다.
* static 변수는 프로그램이 종료될 때까지 메모리에 계속 남아있다. 이 변수가 더 이상 사용되지 않는 객체를 계속 참조하고 있다면 그 객체와 관련된 모든 메모리 공간이 해제되지 않아 메모리 누수가 발생하게 된다.

#### 대용량 클래스 코드를 분할하기

* 하나의 클래스 안에 편하다는 이유로 몸집을 부풀리게 된다면 불필요하게 메모리를 소모할 수 있게 되니 불필요한 로직은 제거하고 별도로 분리할 수 있는 내용이라면 분리하는 것을 추천한다.

#### 디자인 패턴으로 튜닝하기

✅ Singleton 패턴

```java
// 싱글톤 패턴 미적용
public class DateUtil {
    public static String getNow() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
	    return fmt.format(new Date()); 
    }
}
```

```java
// 싱글톤 패턴 적용(단, 싱글톤 패턴의 경우 테스트 코드를 작성하기 어렵다는 단점이 있으니 이 점 주의하자.)
public class DateUtil2 {
    private static String date;   // 싱글톤 패턴 적용
	
    public static String getNow() {
        if(date == null) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
            date = fmt.format(new Date());
	}
	return date;
    }
}
```

#### 파일을 읽을 때 주의사항 정리

* 용량이 큰 File을 자주 읽게 될 때 가급적 파일을 분할 후에 읽도록 기능을 구현하는 것이 좋다.

#### 최대 힙 메모리를 늘리면 좋은 점

* 최대 메모리가 적을수록 GC가 자주 일어나고 최대 메모리가 클수록 GC가 적게 일어난다.
* GC가 자주 일어나게 되면 CPU 성능을 더 쓰게 되는 것이고 프로그램 실행 속도가 느려지게 된다.

#### List에 대한 반복문 처리 속도 향상 방법 1 - 2중 for문 대신 Map을 사용

✅ 중복 필터링 로직 예시

```java
public class Main {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        List<Integer> list1 = ListUtil.getList(1, 500000);
        List<Integer> list2 = ListUtil.getList(5000, 7000);
        List<Integer> duplList = new ArrayList<>();
        Map<Integer, Object> map = new HashMap<>();

        for(Integer n2 : list2) {
            map.put(n2, "");
        }

        for (Integer n1 : list1) {
            if(map.get(n1) != null) {
		duplList.add(n1);
    	    }
        }
        System.out.println(duplList.size());
        long end = System.currentTimeMillis();
        long diff = end - start;
	System.out.println("성능(ms): " + diff);
    }
}
```

#### Database 쿼리 실행 속도 향상

* MyBatis에서 foreach문을 통해 대량의 Insert 시 한 번의 쿼리로 처리할 수 있게 된다.
* 그러나 트랜잭션이 처리할 수 있는 쿼리의 용량에는 제한이 있다 - 하나의 트랜잭션이 너무 많은 쿼리를 담거나 너무 오랫동안 실행하면 여러 문제가 발생할 위험이 있다.

#### Database 트랜잭션 규모와 시간에서 비롯되는 문제 1 - 락과 동시성 문제 측면

* 만약 하나의 트랜잭션이 너무 많은 데이터를 수정하거나 오랜 시간 동안 실행되면 락이 걸린 데이터가 많아지고 락이 유지되는 시간도 길어지게 된다.
* 이로 인해 다른 작업들이 해당 데이터를 사용하지 못하고 기다리는 병목 현상이 발생해 전체 시스템 성능이 크게 저하될 수 있다. 최악의 경우 여러 트랜잭션이 서로의 락이 해제되기를 기다리는 교착 상태에 빠질 수 있다.

#### Database 트랜잭션 규모와 시간에서 비롯되는 문제 2 - 로그 파일 용량 측면

* 데이터베이스는 트랜잭션의 변경 사항을 로그 파일로 기록한다. 이는 문제가 발생했을 때 데이터를 복구하기 위함이다.
* 트랜잭션의 규모가 크면 클수록 기록해야 할 로그의 양도 많아지게 된다. 이로 인해 로그 파일이 저장되는 디스크 공간이 가득 찰 수 있으며, 이는 데이터베이스 시스템 전체의 장애로 이어지게 된다.

#### Database 트랜잭션 규모와 시간에서 비롯되는 문제 3 - 메모리 사용량 측면

* 트랜잭션이 처리하는 데이터는 데이터베이스 서버의 메모리에 임시로 저장된다.
* 거대한 트랜잭션은 많은 메모리를 사용하게 되어 서버 메모리 부족을 유발할 수 있다. 이는 다른 프로세스에 영향을 주어 서버 전체 성능을 떨어뜨리는 원인인 된다.

#### API 성능 테스트를 위한 JMeter

* [Apache Jmeter 다운로드](https://jmeter.apache.org/download_jmeter.cgi)
* [JMeter](../../testing/test-feat-load-testing/jmeter.md)

#### Tomcat(WAS) 성능 튜닝

* [Optimising and Tuning Apache Tomcat](https://spring.io/blog/2008/10/14/optimising-and-tuning-apache-tomcat-part-2)

#### Log는 적재적소에

**1. 디스크 I/O는 느린 작업 중 하나라는 사실을 인지**

* Blocking 발생 : 로그를 한 줄 남길 때마다 쓰레드가 디스크에 데이터가 물리적으로 쓰여지기를 기다려야 한다면 그동안 애플리케이션의 비즈니스 로직은 멈추게 된다.
* Context Switching 유발 : 유저 모드에서 실행되던 JVM 애플리케이션이 로그를 쓰기 위해 커널 모드로 시스템 콜을 호출하는 과정에서 빈번한 컨텍스트 스위칭이 발생한다. 이는 CPU 자원을 갉아먹는 주범이다.

**2. 문자열 연산 자체의 비용도 무시못해**

* DEBUG 로그에는 보통 현재 객체의 상태나 파라미터 값들이 상세하게 포함된다.
* 로그 레벨이 INFO여서 실제로 출력이 되지 않더라도, Java는 실행하기 위해 `user.toString()`을 호출하고 문자열을 합치는 연산을 수행한다.
* 매 요청마다 수십 개의 DEBUG 레벨의 로그가 있다면, 출력되지도 않을 문자열을 만드느라 Heap 메모리가 낭비되고 이는 곧 GC 부하로 이어진다.

**3. 동기화와 락(Lock) 경쟁**

* 많은 로깅 라이브러리(Logback, Log4j2 등)는 여러 쓰레드가 동시에 하나의 로그 파일에 쓸 때, 내용이 섞이지 않도록 동기화 처리를 한다.
* Lock Contention : DEBUG 로그가 너무 많으면, 수많은 워커 쓰레드들이 로그를 남기기 위해 로그 쓰기 권한을 얻으려고 경쟁을 하게 된다.
* 결과적으로 비즈니스 로직을 처리해야 할 쓰레드들이 로깅 쓰레드 뒤에서 대기하면서 응답 속도가 기하급수적으로 증가한다. 

#### Thread Dump 분석 방법

* 프로세스에 속한 모든 쓰레드들의 상태를 기록한 것으로 발생된 문제들을 진단, 분석하고 JVM 성능을 최적화하는데 필요한 정보들을 보여준다.

#### Heap Dump 분석 방법

* Heap 영역은 객체 인스턴스들이 위치하는 영역이다. 덤프 파일은 운영중인 애플리케이션의 힙 메모리 영역을 스냅샷으로 기록한 내역을 저장한 파일을 말한다.

> [참고 - Thread Dump와 Heap Dump](https://sup2is.github.io/2020/10/29/thread-dump-and-heap-dump.html)
