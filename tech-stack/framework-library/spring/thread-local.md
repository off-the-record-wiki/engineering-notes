# Spring ‐ Thread Local

#### 동시성 문제

* 여러 쓰레드가 동시에 같은 인스턴스의 필드 값을 변경하면서 발생하는 문제
* 동시성 문제는 여러 쓰레드가 같은 인스턴스 필드에 접근하기 때문에 트래픽이 적은 상황에서는 확률상 잘 나타나지 않고, 트래픽이 점점 많아질수록 자주 발생한다.
* 스프링 빈과 같이 싱글톤 컨테이너로 관리되는 경우 동시성 문제를 조심해야 한다. 

#### 쓰레드 로컬(Thread Local)

* 쓰레드 로컬은 해당 쓰레드만 접근할 수 있는 특별한 저장소를 말한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/089bf7ee-69dc-419a-aba6-28a68bc62647.png)

```java
@Slf4j
public class ThreadLocalService {

    private ThreadLocal<String> nameStore = new ThreadLocal<>();

    public String logic(String name) {
        log.info("저장 name={} -> nameStore={}", name, nameStore.get());
        
        // 해당 쓰레드 전용 보관소에 값 저장
        nameStore.set(name); 
        
        sleep(1000);
        
        log.info("조회 nameStore={}", nameStore.get());
        
        // 해당 쓰레드 전용 보관소에서 값 조회
        return nameStore.get(); 
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

* 자바는 언어 차원에서 쓰레드 로컬을 지원하기 위한 ThreadLocal 클래스를 제공한다.
* 해당 쓰레드가 쓰레드 로컬을 사용하고 나면 반드시 쓰레드 로컬에 저장된 값을 제거해주어야 한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4ea3f24d-e2e4-484a-89bb-80218b2e326d.png)

* 동시성 문제가 발생하는 이유
  * 여러 쓰레드가 힙 영역에 있는 인스턴스의 필드에 접근(싱글톤 객체)
  * 클래스 영역에 static 으로 붙은 공용 필드에 접근
* 스택 영역은 쓰레드 별로 생성이 되기 때문에 지역 변수에서는 동시성 문제가 발생하지 않는다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4ea3f24d-e2e4-484a-89bb-80218b2e326d.png)

> [📚Thread Local](https://en.wikipedia.org/wiki/Thread-local_storage)
>
> **1. Spring Framework에서의 ThreadLocal**
> * 스프링은 멀티쓰레드 환경인 WAS 위에서 동작하며, 수많은 사용자 요청을 동시에 처리한다.
> * 이 때, 스프링은 ThreadLocal을 사용하여 각 요청마다 독립적인 상태를 유지한다.
>
> **2. 동기화 없는 상태 공유**
> * 스프링의 싱글톤 빈은 상태를 가지지 않지만 ThreadLocal을 사용하면 빈의 무상태성을 유지하면서도 쓰레드 단위의 상태를 안전하게 관리할 수 있다.
>
> **3. 인프라 로직의 투명성**
> * 트랜잭션 관리나 보안 컨텍스트처럼 비즈니스 로직 외적인 '인프라성 데이터'를 비즈니스 로직에 침범시키지 않고 전파하는 역할을 한다.
> 
> **4. 생명주기 책임**
> * 쓰레드 풀을 사용하는 스프링 애플리케이션 특성상 원칙에 따라 요청 끝에서 `remove()`를 호출하는 것이 바람직하다.
> * 쓰레드 로컬의 값을 사용 후 제거하지 않으면 WAS처럼 쓰레드 풀을 사용하는 경우 심각한 문제가 발생한다.

1. 사용자가 저장 HTTP를 요청한다.
2. WAS는 쓰레드 풀에서 쓰레드를 하나 조회한다.
3. 쓰레드가 할당된다.
4. 쓰레드는 사용자A의 데이터를 쓰레드 로컬에 저장한다.
5. 쓰레드 로컬의 전용 보관소에 사용자A의 데이터를 보관한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/decfd665-018d-4235-9117-f6967e774b43.png)

1. 사용자A의 HTTP 요청이 끝난다.
2. WAS는 사용이 끝난 쓰레드를 쓰레드 풀에 반납한다. 쓰레드를 생성하는 비용은 비싸기 때문에 제거하지 않고 재사용한다.
3. 사용자A의 쓰레드는 결국 쓰레드 풀에 살아있다. 따라서 쓰레드 로컬에 있는 사용자A가 사용한 쓰레드의 데이터 역시 살아있게 된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/638d0b1c-10d7-4db2-8ae0-0047351b67f9.png)

1. 사용자B가 조회를 위한 새로운 HTTP 요청을 한다.
2. WAS는 쓰레드 풀에서 쓰레드를 하나 조회한다.
3. 사용자B에게 사용자A가 사용했던 쓰레드가 할당된다.(항상 그렇다는 것은 아니다. 물론 다른 쓰레드가 할당될 수 있다.)
4. 쓰레드는 쓰레드 로컬에서 데이터를 조회한다.
5. 쓰레드는 쓰레드 로컬 전용 보관소에 있는 사용자A 관련 값을 반환한다.
6. 결과적으로 사용자A와 관련한 값이 반환된다.
7. 사용자B는 사용자A의 정보를 조회하게 된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/6d5903ac-1e44-4fcc-9eb7-808f0f1bb171.png)

* 스프링 시큐리티의 인증 / 인가 처리는 여러 필터 체인으로 구성이 되는데 그 중 FilterChainProxy 클래스의 코드를 보면 위와 같다.
* 여기서 요청 처리 후 clearContext()를 통해 쓰레드 로컬의 값을 자동으로 클리어하기 때문에 안전하게 사용할 수 있는 것이다.
