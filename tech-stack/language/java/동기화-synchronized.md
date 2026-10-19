# Java - synchronized

#### 출금 예제 코드를 보고 비롯되는 문제점 지적하기

```java
public interface BankAccount {

	boolean withdraw(int amount);  // 출금 메서드
	int getBalance();              // 잔고 조회 메서드
}
``` 

```java
// BankAccount 인터페이스를 구현한 구현체 클래스
public class BankAccountImplV1 implements BankAccount {

	private int balance;	// 잔고(자원)

	public BankAccountImplV1(int balance) {
		this.balance = balance;
	}

	@Override
	public boolean withdraw(int amount) {
		log("거래 시작: " + getClass().getSimpleName());
		log("[검증 시작] 출금액: " + amount + ", 잔액: " + balance);
		if (balance < amount) {

			log("[검증 실패] 출금액: " + amount + ", 잔액: " + balance);
			return false;
        }

		log("[검증 완료] 출금액: " + amount + ", 잔액: " + balance);
		sleep(1000);
		balance = balance - amount;
		log("[출금 완료] 출금액: " + amount + ", 변경 잔액: " + balance);
		log("거래 종료");
		return true;
	}

	@Override
	public int getBalance() {
		return balance;
	}
}
```

```java
// 출금 요청 쓰레드 생성
public class WithdrawTask implements Runnable {

	private final BankAccount bankAccount;
	private final int amount;

	public WithdrawTask(BankAccount bankAccount, int amount) {
		this.bankAccount = bankAccount;
		this.amount = amount;
	}

	@Override
	public void run() {
		bankAccount.withdraw(amount);
	}
}
```

```java
public class BankAccountMain {

	public static void main(String[] args) throws InterruptedException {

		BankAccount bankAccount = new BankAccountImplV1(1000);

		Thread thread1 = new Thread(new WithdrawTask(bankAccount, 800), "t1");
		Thread thread2 = new Thread(new WithdrawTask(bankAccount, 800), "t2");

		thread1.start();
		thread2.start();

		thread1.join();
		thread2.join();

		log("최종 잔액: " + bankAccount.getBalance());
	}
}
```

> 실행 결과

```
01:25:56.196 [       t2] 거래 시작: BankAccountImplV1
01:25:56.196 [       t1] 거래 시작: BankAccountImplV1
01:25:56.202 [       t1] [검증 시작] 출금액: 800, 잔액: 1000
01:25:56.202 [       t2] [검증 시작] 출금액: 800, 잔액: 1000
01:25:56.203 [       t1] [검증 완료] 출금액: 800, 잔액: 1000
01:25:56.206 [       t2] [검증 완료] 출금액: 800, 잔액: 1000
01:25:57.208 [       t2] [출금 완료] 출금액: 800, 변경 잔액: 200
01:25:57.209 [       t2] 거래 종료
01:25:57.212 [       t1] [출금 완료] 출금액: 800, 변경 잔액: -600
01:25:57.212 [       t1] 거래 종료
01:25:57.217 [     main] 최종 잔액: -600
```

#### 그림 설명

![스크린샷 2025-01-09 오전 1 29 16](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/661e8b61-fce4-41fd-9154-5c0cc217115b.png)

![스크린샷 2025-01-09 오전 1 29 31](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a97fb55c-9294-4d4a-b6a8-a5c368eaac8d.png)

```
- 2개의 요청 쓰레드가 생성되고 출금 요청을 처리하게 된다. 이 때, `BankAccount` 타입의 인스턴스가 공유가 된다.
- 동일한 `BankAccount` 인스턴스를 공유하기 때문에 멤버 변수인 잔고 역시 공유가 된다.
- 따라서 2개의 요청 쓰레드가 요청을 보내게 되면 각각 하나의 인스턴스만을 놓고 보았을 때, 출금하는데 제약이 발생하지 않아(즉, 잔고보다 많은 출금 금액이 아니라는 것) 정상적으로 출금이 된다.
- 그 결과 2개의 요청 쓰레드가 동시에 출금에 성공하면서 현재 잔고 1000원보다 많은 1600원이 출금되어 -600원이라는 음수값이 도출되는 것이다.
```

#### 동시성 문제

* 동시성 문제와 관련된 두 가지 시나리오는 아래와 같다.

```
①. t1 쓰레드가 t2 쓰레드보다 아주 약간 빠르게 실행된 경우
②. t1 쓰레드와 t2 쓰레드가 동시에 실행된 경우
```

#### 1. t1 쓰레드가 t2 쓰레드보다 아주 약간 빠르게 실행된 경우

![스크린샷 2025-01-09 오전 1 45 21](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/aae81c2e-6bff-45b1-b16a-b4c6dd9c9172.png)

```
- t1 쓰레드가 아주 근소한 차이로 먼저 코드 블럭에 진입해 분기문 측에서 검증 로직을 체크하고 있고 이후 t2 쓰레드가 코드 블럭에 진입한 상태이다.
- 쓰레드 t1이 아주 근소한 차이로 먼저 실행되면서 출금을 시도한다.
- 쓰레드 t1이 출금 검증 로직을 실행한다. 이 때, 조건을 만족하므로 검증 로직 통과 후 `sleep(1000)`으로 가게 된다.
- t2 쓰레드가 이후 들어오면서 출금 검증 로직을 실행한다. 이 시점에 t1 쓰레드는 출금 로직 실행 대기를 위해 `sleep(1000)` 상태에 있기 때문에 아직 완벽히 출금이 되지 않은 상태이므로 t2 쓰레드가 출금 검증 로직을 통과하고 나오게 된다.
- 결과적으로 t1, t2 쓰레드 둘 다 출금 검증 로직을 통과하면서 1600원이란 금액이 출금되면서 음수값이 나오게 되는 것이다.
```

#### 2. t1 쓰레드와 t2 쓰레드가 동시에 실행된 경우

![스크린샷 2025-01-09 오전 1 51 52](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e24ac51f-2b55-4b09-bf65-f01acf60a293.png)

```
- t1, t2 쓰레드가 출금 검증 로직을 거치게 된다. 이 때, `BankAccount` 인스턴스가 공유가 된다.
- t1, t2 쓰레드 모두 출금 금액이 잔액보다 적기에 검증 로직을 그대로 통과하게 되면서 둘 다 잔고는 200원이 된다.
- 여기서 한 가지 문제가 된다. 분명 2개의 쓰레드 요청의 출금 금액을 합하면 1600원이 출금이 되는데 실제론 한 번만 출금이 된 것으로 계산이 된 200원이 나오게 되는 것이다.
- 출금 요청 800원은 어디론가 증발이 된 것이다.
```

#### 임계 영역

* 위와 같은 동시성 문제가 발생한 근본 원인은 바로 **여러 쓰레드가 함께 사용하는 공유 자원을 여러 단계로 나누어 사용하기 때문이다.**
* 여기서 `BankAccount` 인터페이스 구현체가 가지는 멤버 변수인 `balance(잔액)`은 여러 쓰레드가 함께 사용하는 **공유 자원**이다.
* 따라서 출금 로직을 수행하는 중간에 다른 쓰레드에서 이 값을 얼마든지 바꿀 수 있다.
* 출금 메서드를 한 번에 하나의 쓰레드만 실행할 수 있게 제한한다면 동시성 문제는 발생하지 않게 된다.

> [❗임계 영역(Critical Section)](https://en.wikipedia.org/wiki/Critical_section)
> 
> * 여러 쓰레드가 동시에 접근하면 데이터 불일치나 예상치 못한 동작이 발생할 수 있는 위험하고 중요한 코드 부분을 뜻한다.<br>
> * 여러 쓰레드가 동시에 접근해서는 안 되는 공유 자원에 접근하거나 이를 수정하는 부분을 의미한다.<br>

* 한 번에 하나의 쓰레드만 접근할 수 있도록 임계 영역을 안전하게 보호하는 방법을 Java에서 제공하는데 바로 `synchronized` 키워드가 그 역할을 수행한다.

#### synchronized 메서드

```java
@Override
public synchronized boolean withdraw(int amount) {
	log("거래 시작: " + getClass().getSimpleName());
	log("[검증 시작] 출금액: " + amount + ", 잔액: " + balance);
	if (balance < amount) {

		log("[검증 실패] 출금액: " + amount + ", 잔액: " + balance);
		return false;
        }

	log("[검증 완료] 출금액: " + amount + ", 잔액: " + balance);
	sleep(1000);
	balance = balance - amount;
	log("[출금 완료] 출금액: " + amount + ", 변경 잔액: " + balance);
	log("거래 종료");
	return true;
}
```

* 위와 같이 `synchronized` 키워드를 붙이면 하나의 쓰레드만 작업을 할 수 있게 된다.

> 실행 결과

```
14:07:55.104 [       t1] 거래 시작: BankAccountImplV1
14:07:55.110 [       t1] [검증 시작] 출금액: 800, 잔액: 1000
14:07:55.110 [       t1] [검증 완료] 출금액: 800, 잔액: 1000
14:07:56.116 [       t1] [출금 완료] 출금액: 800, 변경 잔액: 200
14:07:56.117 [       t1] 거래 종료
14:07:56.117 [       t2] 거래 시작: BankAccountImplV1
14:07:56.118 [       t2] [검증 시작] 출금액: 800, 잔액: 200
14:07:56.118 [       t2] [검증 실패] 출금액: 800, 잔액: 200
14:07:56.123 [     main] 최종 잔액: 200
```

* 자바의 synchronized 키워드가 어떻게 작동하는지는 아래와 같다.

![스크린샷 2025-01-09 오후 2 15 10](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/91f4e27b-f51d-4ef7-b28c-6be62a48a4e0.png)

```
- 모든 객체(인스턴스)는 자신만의 락(Lock)을 가지고 있다.
- 쓰레드가 synchronized 키워드가 있는 메서드에 진입하려면 반드시 해당 인스턴스의 락이 있어야 한다.
```

![스크린샷 2025-01-09 오후 2 18 20](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/fc20d54e-0089-4c87-b7ec-e7207224ffeb.png)

```
- 쓰레드 t1이 먼저 실행되고 BankAccount 인스턴스의 락을 확보하면서 메서드가 실행되고 검증 로직을 수행하게 된다.
- 이 때, 쓰레드 t2는 락을 획득하지 못했기 때문에 대기 상태에 진입하게 된다.
```

![스크린샷 2025-01-09 오후 2 21 02](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/66811a20-478b-4fef-b100-3c37b79f48f1.png)

```
- 쓰레드 t1이 메서드 수행을 끝나고 BankAccount 인스턴스에 락을 반납한다.
```

![스크린샷 2025-01-09 오후 2 23 37](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4f80af94-930a-466d-89a3-a72a4ee3783a.png)

```
- 쓰레드 t2가 BankAccount 인스턴스의 락을 획득하면서 synchronized 키워드가 붙은 메서드에 진입한다.
- 하지만 잔액보다 많은 금액의 출금을 시도하므로 실패하게 된다.
```

> ❗락을 획득하는 순서는 보장되지 않는다.
> 
> * BankAccount 인스턴스의 `withdraw()` 메서드를 수많은 쓰레드가 동시에 호출한다면 1개의 쓰레드만 Lock을 획득하고 나머지는 쓰레드는 Blocked 상태가 된다.<br>
> * 그리고 락을 획득한 쓰레드가 요청을 수행한 BankAccount 인스턴스에 락을 반납하면 해당 인스턴스의 락을 기다리는 수많은 쓰레드 중 하나가 락을 획득하고 작업을 하게 된다.<br>
> * 어떤 순서로 락을 획득하는지는 자바 표준에 정의되어 있지 않다. 따라서 순서를 보장하지 않고, 환경에 따라서 순서가 달라질 수 있다.<br>

#### synchronized 코드 블럭

* `synchronized` 키워드의 가장 큰 장점이자 단점은 한 번에 하나의 쓰레드만 실행할 수 있다는 점이다.
* 동시성 문제를 해결해주지만 여러 쓰레드가 동시에 실행하지 못하기 때문에 전체적으로 보면 성능이 저하된다.
* 따라서 `synchronized`를 통해 동시에 실행할 수 없는 코드 구간은 꼭 필요한 곳에만 한정해서 사용을 해야 한다.

```java
@Override
public boolean withdraw(int amount) {
	log("거래 시작: " + getClass().getSimpleName());

	// synchronized 코드 블럭 단위로..
	synchronized (this) {
		log("[검증 시작] 출금액: " + amount + ", 잔액: " + balance);
		if (balance < amount) {

			log("[검증 실패] 출금액: " + amount + ", 잔액: " + balance);
			return false;
		}

		log("[검증 완료] 출금액: " + amount + ", 잔액: " + balance);
		sleep(1000);
		balance = balance - amount;
		log("[출금 완료] 출금액: " + amount + ", 변경 잔액: " + balance);
	}

	log("거래 종료");
	return true;
}
```

> 실행 결과

```
14:44:57.276 [       t1] 거래 시작: BankAccountImplV3
14:44:57.276 [       t2] 거래 시작: BankAccountImplV3
14:44:57.281 [       t1] [검증 시작] 출금액: 800, 잔액: 1000
14:44:57.281 [       t1] [검증 완료] 출금액: 800, 잔액: 1000
14:44:58.287 [       t1] [출금 완료] 출금액: 800, 변경 잔액: 200
14:44:58.287 [       t1] 거래 종료
14:44:58.287 [       t2] [검증 시작] 출금액: 800, 잔액: 200
14:44:58.289 [       t2] [검증 실패] 출금액: 800, 잔액: 200
14:44:58.294 [     main] 최종 잔액: 200
```

* 자바에서 동기화(synchronized) 키워드는 여러 쓰레드가 동시에 접근할 수 있는 자원에 대해 일관성있고 안전한 접근을 보장하기 위한 메커니즘이다.
* 동기화는 주로 멀티쓰레드 환경에서 발생할 수 있는 문제, 예를 들어 데이터 손상이나 예기치 않은 결과를 방지하기 위해 사용된다.

```java
// 메서드 동기화(넓은 범위)
public synchronized void synchronizedMethod() {

}
```

```java
// 코드 블록 동기화(좁은 범위)
public void method() {
    synchronized (this) {
    
    }
}
```

> ❗락을 오래 잡을수록 성능과 확장성이 나빠진다.
>
> * 코드 블록 동기화(좁은 범위)를 대체적으로 선호하는 이유는 락 점유 시간을 최소화해 불필요한 경쟁을 감소시키기 때문이다.
> * 메서드 동기화(넓은 범위)의 경우 필요 없는 코드까지 전부 락을 걸기 때문에 I/O나 계산이 포함되면 병목이 심해져 유지보수 시 어디가 임계 영역인지가 명확히 보이지 않는 문제가 발생한다.<br>
> * 너무 잘게 쪼개라는 것이 아니다. 가능하면 좁게 잡되 안전성을 깨지 않는 선에서 해야한다.<br>

* 이런 동기화를 사용하면 다음 문제들을 해결할 수 있다. 다만 트레이드오프(성능 저하)는 인지를 하고 써야한다.
  * 경합 조건(Race Condition) : 두 개 이상의 쓰레드가 경쟁적으로 동일한 자원을 수정할 때 발생하는 문제
  * 데이터 일관성 : 여러 쓰레드가 동시에 읽고 쓰는 데이터의 일관성을 유지
