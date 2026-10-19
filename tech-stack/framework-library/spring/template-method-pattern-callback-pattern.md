# Spring ‐ Template Method Pattern & Callback Pattern

#### 템플릿 메서드 패턴

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/bb342be2-1eda-4177-8310-fdeb3da303ab.png)

* 부모 클래스에 알고리즘의 골격인 템플릿을 정의하고 일부 변경되는 로직은 자식 클래스에서 정의하는 것
* 자식 클래스가 알고리즘 전체 구조를 변경하지 않고 특정 부분만 재정의 가능하다.
* 상속과 오버라이딩을 통한 다형성으로 문제를 해결한 것

```java
@Slf4j
public abstract class AbstractTemplate {

    /**
     * 변하지 않는 전체 흐름을 실행하는 템플릿 메서드
     */
    public void execute() {
        long startTime = System.currentTimeMillis();
        
        // 변하는 비즈니스 로직 실행 (자식 클래스에서 구현)
        call(); 
        
        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        
        log.info("resultTime={}", resultTime);
    }

    /**
     * 변하는 부분은 추상 메서드로 선언하여 상속을 통해 구현
     */
    protected abstract void call();
}
```

#### 전략 패턴

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/09faece8-5bb6-4b6f-ac37-4f40655ffc69.png)

* Context에서 Strategy 인터페이스를 주입받는다.
* Context에서 `execute()`를 호출할 때 Strategy에 위임한다.

```java
@Slf4j
public class ContextV1 {

	private final Strategy strategy;  // Strategy 주입

	public ContextV1(Strategy strategy) {
		this.strategy = strategy;
	}

	// Context에서 call()을 호출할 때 Strategy에 위임한다.
	public void execute() {
		long startTime = System.currentTimeMillis();
		//비즈니스 로직 실행
		strategy.call(); //위임
		//비즈니스 로직 종료
		long endTime = System.currentTimeMillis();
		long resultTime = endTime - startTime;
		log.info("resultTime={}", resultTime);
	}
}
```

#### 템플릿 콜백 패턴

* 콜백(Callback) : 다른 코드의 인수로서 넘겨주는 실행 가능한 코드
* 기존 전략 패턴에서 주입 방식 대신 파라미터 방식으로 넘겨주는 방식이 이에 해당한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4582f984-057d-4421-94a4-aa134f4fa2c1.png)

```java
@Slf4j
public class ContextV2 {

    /**
     * 전략을 필드로 가지지 않고, 실행할 때마다 파라미터로 전달받음
     * 이 방식은 Context를 싱글톤으로 사용할 때 동시성 문제로부터 안전함
     */
    public void execute(Strategy strategy) {
        long startTime = System.currentTimeMillis();
        
        // 비즈니스 로직 실행을 파라미터로 받은 strategy 객체에 위임
        strategy.call(); 
        
        long endTime = System.currentTimeMillis();
        long resultTime = endTime - startTime;
        
        log.info("resultTime={}", resultTime);
    }
}
```
