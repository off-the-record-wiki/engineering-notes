# Java - JVM & GC & SOLID

#### 자바 개요

* 객체지향 프로그래밍 언어 - 클래스와 객체를 기반으로 설계되며, 추상화, 캡슐화, 상속, 다형성을 지원
* 플랫폼 독립성 - 자바 코드는 JVM 위에서 실행되므로 OS에 관계없이 동작
* 자동 메모리 관리 - 개발자 메모리를 해제하지 않아도 JVM이 자동으로 불필요한 객체를 분리
* 멀티쓰레딩 지원 - 여러 작업을 동시에 실행할 수 있는 멀티 쓰레딩 기능을 제공

> [📚SRP](https://en.wikipedia.org/wiki/Single-responsibility_principle)
>
> * 어떤 클래스는 변경해야 하는 이유가 오직 하나뿐이어야 한다.
> * 변경되어야 하는 이유를 단 하나로 제한하는 원칙으로 여기서 '책임'이란, 특정 액터의 요구사항을 의미하며, 이를 분리함으로써 변경에 따른 부작용을 최소화하고 소프트웨어의 응집도를 높이는 것이 목적이다.

#### SRP(Single Responsibility Principle, 단일 책임 원칙)

```java
// ❌ SRP 위반 예시
public class Computer {
    public void startComputer() {
        System.out.println("부팅 중...");
        checkHardware();
        loadOS();
        connectNetwork();
        System.out.println("컴퓨터 준비 완료");
    }
  
    private void checkHardware() {
        System.out.println("하드웨어 점검");
    }
 
    private void loadOS() {
        System.out.println("운영체제 로딩");
    }
 
    private void connectNetwork() {
        System.out.println("네트워크 연결");
    }
}
```

```java
// ✅ SRP 준수한 리팩토링
class Hardware {
    void check() {
        System.out.println("하드웨어 점검");
    }
}

class OS {
    void load() {
        System.out.println("운영체제 로딩");
    }
}

class Network {
    void connect() {
        System.out.println("네트워크 연결");
    }
}

public class Computer {
    private final Hardware hardware = new Hardware();
    private final OS os = new OS();
    private final Network network = new Network();
 
    public void startComputer() {
        hardware.check();
        os.load();
        network.connect();
    }
}
```

> [📚OCP](https://en.wikipedia.org/wiki/Open%E2%80%93closed_principle)
>
> * 확장에는 열려 있고, 수정에는 닫혀 있어야 한다.
> * 단순히 코드는 고치지 말라는 뜻이 아니라, 기존의 코드를 건드리지 않고도 새로운 기능을 추가할 수 있는 구조를 만들라는 의미이다.
> * 확장에 열려 있다 - 요구사항이 변경될 때 새로운 동작을 추가하여 기능을 확장할 수 있다.
> * 수정에 닫혀 있다 - 기존 코드를 수정하지 않고도 애플리케이션의 동작을 바꿀 수 있다.
> 
> **1. 인터페이스 추상화를 통한 구현(Interface-based)**
> * '어떤 기능을 할 것인가'에 대한 규격만 정의한다.
> * 인터페이스에는 메서드의 시그니처만 선언하고, 실제 로직은 하나도 담지 않는다.
> * 구현체들끼리 코드를 공유할 필요가 전혀 없을 때 유리하다.
> * OCP 관점에서의 평가 : 새로운 기능이 필요하면 기존 코드를 수정하는 대신, 인터페이스를 구현하는 새로운 클래스를 하나 더 만들기만 하면 된다.
>
> **2. 추상 클래스를 통한 상속(Abstract Class-based)**
> * `abstract class`를 만들어 공통으로 쓰이는 메서드는 미리 구현해두고 자식마다 달라져야 하는 부분만 `abstract` 메서드로 남겨둔다.
> * 여러 구현체 사이에 중복되는 코드가 많을 때, 부모 클래스에 모아 효율적으로 관리할 수 있다.
> * OCP 관점에서의 평가 : 공통 로직(Base)은 닫아두고, 달라지는 상세 로직만 상속을 통해 확장한다.

#### OCP(Open-Closed Principle, 개방-폐쇄 원칙)

```java
// ❌ OCP 위반 예시
public class NotificationService {
    public void send(String type, String message) {
        if (type.equals("email")) {
            System.out.println("이메일 전송: " + message);
        } else if (type.equals("sms")) {
            System.out.println("문자 전송: " + message);
        }
    }
}
```

```java
// ✅ OCP 준수 리팩토링
interface Notifier {
    void send(String message);
}

class EmailNotifier implements Notifier {
    public void send(String message) {
        System.out.println("이메일 전송: " + message);
    }
}

class SmsNotifier implements Notifier {
    public void send(String message) {
        System.out.println("문자 전송: " + message);
    }
}

public class NotificationService {
    public void notify(Notifier notifier, String message) {
        notifier.send(message);
    }
} 
```

> [📚ISP](https://en.wikipedia.org/wiki/Interface_segregation_principle)
> 
> * 사용하지 않는 메서드에 의존하도록 강제해선 안 된다.
> * 하나의 거대한 인터페이스를 만드는 대신, 클라이언트가 실제로 필요로 하는 메서드들만 모아놓은 여러 개의 구체적인 인터페이스로 분리하라는 원칙이다.
> * ISP를 잘 지키는 방법은 인터페이스를 만들 때, "이 객체는 무엇인가(What it is)"가 아니라, "어떤 역할을 수행하는가"에 집중해야 한다.
> * 인터페이스가 비대해지면 발생하는 가장 큰 문제는 불필요한 연쇄 수정이다.

#### ISP(Interface Segregation Principle, 인터페이스 분리 원칙)

```java
// ❌ ISP 위반 예시
interface Machine {
    void print();
    void scan();
    void fax();
}

class BasicPrinter implements Machine {
    public void print() {
       System.out.println("문서 출력");
    }
 
    public void scan() {
        throw new UnsupportedOperationException("스캔 지원 안함");
    }
 
    public void fax() {
        throw new UnsupportedOperationException("팩스 지원 안함");
    }
}
```

```java
// ✅ ISP 준수 리팩토링
interface Printable {
    void print();
}

interface Scannable {
    void scan();
}

interface Faxable {
    void fax();
}

class BasicPrinter implements Printable {
    public void print() {
        System.out.println("문서 출력");
    }
}

class MultiFunctionPrinter implements Printable, Scannable, Faxable {
    public void print() {
        System.out.println("문서 출력");
    }
 
    public void scan() {
        System.out.println("문서 스캔");
    }

    public void fax() {
        System.out.println("문서 팩스 전송");
    }
}
```

> [📚DIP](https://en.wikipedia.org/wiki/Dependency_inversion_principle)
>
> * 상위 모듈이 하위 모듈에 의존해서는 안 되고 둘 다 추상화에 의존해야한다는 원칙이다.
> * 이는 시스템의 결합도를 낮추고 유연성을 극대화하기 위해 의존 관계의 화살표 방향을 거꾸로 뒤집는 것이 핵심이다.
> 
> **1. 전통적인 의존 관계 (DIP 위반)**
> * 상위 모듈이 구체적인 하위 모듈을 직접 참조하는 방식이다.
> * 예를 들어, 알림 방식을 SMS에서 Email로 바꾸고 싶은데 상위 모듈의 코드를 직접 수정한다면 결국 하위의 변화가 싱위로 전파되는 것이 된다.
>
> **2. 역전된 의존 관계 (DIP 준수)**
> * 중간에 인터페이스를 두어 두 모듈 인터페이스를 바라보게 한다.
> * 개발자들이 흔히 하는 실수는 저수준(도구)에 맞춰 고수준(정책)을 짜는 것이다.
> * 고수준 모듈(High-Level) : "무엇을 할 것인가"에 대한 비즈니스 로직이다.
> * 저수준 모듈(Low-Level) : "어떻게 할 것인가"에 대한 구체적인 방법이다.
> * 예를 들어, "구체적으로 누가 올진 모르겠지만, 전송할 수 있는 것과 일을 하겠다"라는 정책을 고수준 모듈에서 정의하면 "전송할 수 있는 규격에 맞춰서 만들어진 SMS 전송기"라는 도구가 이를 맞추는 대표적인 사례가 될 것이다.

#### DIP(Dependency Inversion Principle, 의존성 역전 원칙)

```java
// ❌ DIP 위반 예시
class MySQLDatabase {
    public void connect() {
        System.out.println("MySQL에 연결");
    }
}

class DataService {
    private final MySQLDatabase database;
    
    public DataService() {
        this.database = new MySQLDatabase();
    }
 
    public void use() {
        database.connect();
    }
}
```

```java
// ✅ DIP 준수 리팩토링
interface Database {
    void connect();
}

class MySQLDatabase implements Database {
    public void connect() {
        System.out.println("MySQL에 연결");
    }
}

class DataService {
    private final Database database;
    
    public DataService(Database database) {
        this.database = database;
    }

    public void use() {
        database.connect();
    }
}
```

> [📚LSP](https://en.wikipedia.org/wiki/Liskov_substitution_principle)
> 
> * 자식 클래스는 언제나 자신의 부모 클래스를 대체할 수 있어야 한다는 원칙이다.
>
> **1. instanceof를 쓰지 말라는 이유**
> * 코드 중간에 `if (obj instanceof S)`와 같은 조건문이 들어가는 순간, 그 코드는 이미 LSP를 위반했을 확률이 높다.
> * 상위 타입(T)만 보고도 코드가 완벽하게 돌아가야 하는데, 특정 하위 타입(S)인지 아닌지를 확인해야만 정상 동작한다는 뜻이기 때문이다.
> * 새로운 서브타입이 추가될 때마다 해당 조건문을 계속 수정해야 하므로 OCP까지 줄줄이 깨지게 된다.

#### LSP(Liskov Substitution Principle, 리스코프 치환 원칙)

```java
// ❌ LSP 위반 예시
class Bird {
    public void fly() {
        System.out.println("날아갑니다");
    }
}

class Ostrich extends Bird {
    @Override
    public void fly() {
        throw new UnsupportedOperationException("타조는 날 수 없습니다");
    }
}

public class Zoo {
    public void makeBirdFly(Bird bird) {
        bird.fly();
    }
}

Zoo zoo = new Zoo();
zoo.makeBirdFly(new Ostrich()); // 예외 발생 → LSP 위반
```

```java
// ✅ LSP 준수 리팩토링
interface Flyable {
    void fly();
}

class FlyingBird implements Flyable {
    public void fly() {
       System.out.println("날아갑니다");
    }
}

// 이제 날 수 있는 새만 fly()를 갖고, 타조는 분리됨
class Ostrich {
    public void run() {
        System.out.println("달립니다");
    }
}

public class Zoo {
    public void makeFly(Flyable bird) {
        bird.fly();
    }
}
```

#### JVM 자바 가상머신 구조

* JVM(Java Virtual Machine)은 자바 프로그램을 실행하기 위한 가상 머신
* 자바 코드를 바이트 코드로 변환하여 실행하는 역할을 담당
* 운영체제와 독립적으로 동작하며 어떤 OS에서도 실행 가능

#### JVM의 구성 요소

* 클래스 로더(Class Loader)
* 런타임 데이터 영역(Runtime Data Area)
* 실행 엔진(Execution Engine)
* 가비지 컬렉터(Garbage Collector)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/985c2e77-7a42-491f-b78d-bb26ba12699c.png)

#### 1. 클래스 로더(Class Loader) 

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/073b5b61-0d14-4cd2-9266-4b34cc0d0fcd.png)

**1. 클래스 로더의 3가지 원칙 (Delegation Model)**

클래스 로더는 계층 구조를 가지고 있으며, 동작할 때 반드시 지키는 3가지 원칙이 있다.
* 위임 원칙(Delegation Principle) : 클래스 로딩 요청을 받으면 먼저 상위 클래스 로더에게 확인을 부탁한다. 최상위까지 올라갔는데 없으면 그 때 직접 로딩한다.
* 가시성 원칙(Visibility Principle) : 하위 클래스 로더는 상위 클래스 로더가 로딩한 클래스를 볼 수 있지만 반대로 상위는 하위의 클래스를 볼 수 없다.
* 유일성 원칙(Uniqueness Principle) : 하위 클래스 로더가 상위 클래스 로더가 이미 로딩한 클래스를 다시 로딩하지 않도록 하여 클래스의 유일성을 보장한다.

**2. 계층 구조(Hierarchy)**

실제로 자바에는 여러 개의 기본 클래스 로더가 존재한다.
* Bootstrap Class Loader : JVM을 가동할 때 가장 먼저 생성되며, 핵심 자바 API를 로드한다.
* Platform Class Loader : 표준 확장 디렉터리에 있는 클래스들을 로드한다.
* System(Application) Class Loader : 우리가 작성한 `.class` 파일이나 외부 라이브러리 클래스들을 로드한다.

**3. 링크(Linking) 단계의 세부 과정**

* Verification(검증) : 생성된 바이트 코드가 자바 언어 명세 및 JVM 명세에 따라 잘 구성되어 있는지 확인한다. 보안을 위한 가장 중요한 단계이다.
* Preparation(준비) : 클래스가 필요로 하는 메모리를 할당한다. 여기서 `static` 키워드 변수에 대한 메모리가 기본값으로 초기화된다.
* Resolution(분석) : 심볼릭 레퍼런스를 실제 메모리 주소인 다이렉트 레퍼런스로 변경한다.

**4. 실무에서 만나는 이슈 : 동적 로딩(Dynamic Loading)**

자바는 기본적으로 클래스를 한꺼번에 로딩하지 않고, 필요할 때 로딩한다.
* 로드 타임 동적 로딩 : 클래스 A 실행 시 클래스 B가 필요하면 그 시점에 로딩한다.
* 런타임 동적 로딩 : 코드 실행 중에 `Class.forName("className")`을 호출하여 어떤 클래스가 들어올지 모르는 상태에서 로딩한다.
> **라이브러리 버전 충돌을 해결하면서 겪은 내용💡**
> 
> * 라이브러리 버전 충돌 발생 시 `NoSuchMethodError`가 발생한다면 클래스 로더의 가시성 원칙이나 위임 원칙 때문에 의도치 않은 버전의 클래스가 먼저 로드된 것은 아닌지 의심해야 한다. 실제로 회사에서 SpringBoot 버전 마이그레이션을 경험하면서 지원되지 않는 메서드로 인한 에러가 발생하면서 어려움을 겪은 적이 있었다.

**5. 초기화(Initialization)의 시점**

* `static`블록은 단순히 클래스가 로드되었다고 실행되지 않고, 클래스에 처음 접근할 때 실행된다.

#### 2. 런타임 데이터 영역(Runtime Data Area)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/443b3be0-19da-4d9b-a0a6-c26e95ed1b22.png)

**1. 데이터 영역의 분류(공유 vs 독립)**

* 모든 쓰레드가 공유(JVM 시작 시 생성)
  * Heap : 모든 객체(인스턴스)가 저장되는 곳. GC의 주요 대상이 된다.
  * Method Area : 클래스 메타 데이터, 런타임 상수 풀(Runtime Constant Pool), static 변수가 저장된다.
* 각 쓰레드마다 독립적으로 생성(쓰레드 종료 시 소멸)
  * Stack, Register, Native Method Stack : 각 쓰레드마다 할당되므로 데이터 동기화 문제가 발생하지 않는다.

**2. Heap 영역의 세분화(GC와 직결)**

* Young Generation : 새롭게 생성된 객체들이 머무는 곳. 여기서 발생하는 GC를 Minor GC라고 한다.
* Old Generation : Young 영역에서 오래 살아남은 객체들이 이동하는 곳. 여기서 발생하는 GC를 Major GC라고 한다. 발생 시 서비스 멈춤(Stop-the-World) 현상이 길어질 수 있다.

**3. Stack 영역과 Frame**

* 스택은 단순히 "지역 변수 저장소"를 넘어 스택 프레임 단위로 동작한다.
* 메서드가 호출될 때마다 하나의 프레임이 쌓이고 메서드가 종료되면 제거된다.
* 프레임 내부에는 Local Variables Array(지역 변수 배열), Operand Stack(피연산자 스택), Frame Data(상수 풀 참조) 등이 있다.
* `StackOverflowError`는 이 프레임이 설정된 스택 크기를 초과해서 쌓일 때 발생한다.

**4. 런타임 상수 풀(Runtime Constant Pool)**

* Method Area 내부에 존재하는 아주 중요한 공간이다.
* 클래스 파일 내부 상수(리터럴, 심볼릭 레퍼런스)가 로드될 때 이곳으로 들어온다.
* 문자열 풀을 사용하면 이 풀에서 생성된 참조를 재사용하여 메모리를 아낄 수 있다.

**5. PC Register(Program Counter)**

* 현재 쓰레드가 실행 중인 JVM 명령의 주소를 기록한다.
* 자바는 멀티 쓰레드 환경이므로 쓰레드가 CPU 점유를 놓쳤다가 다시 잡았을 때, 즉, 컨텍스트 스위칭(Context Switching)이 발생하면 "어디서부터 다시 실행해야하는지" 기억하는 역할을 한다.

> **Java in K8s: OOM💡**
> 
> **1. 문제 : 사라진 로그와 엉뚱한 범인**
> * 당시 입사 6개월 차에 접어들며 업무에 익숙해질 무렵, 사수님으로부터 한 통의 슬랙 메시지를 받았었다.
> * WebFlux 스택으로 운영 중인 배치 모듈에 문제가 발생한 것 같으니 원인을 파악해보라는 내용이었다. 서둘러 터미널을 열고 Kubernetes 파드의 로그를 조회하는 명령어를 통해 애플리케이션 로그를 살펴봤지만 상황은 녹록지 않았다.
> * Kubernetes의 Restart 옵션 때문이었다. 컨테이너가 이미 재시작된 탓에 장애 직전의 핵심적인 JVM 로그가 이미 휘발되어 사라진 상태였다. 남은 로그를 샅샅이 뒤진 끝에 단서 하나를 찾았는데 Redis DNS를 해석할 수 없다는 메시지였다. 당시의 나는 이 메시지를 보고 조금의 구글링을 곁들여 '간헐적인 네트워크 이슈로 인해 Redis 연결이 끊겨 발생한 문제'라고 보고했다.
>
> **2. 해결 과정: 현상 뒤에 숨겨진 진짜 원인**
> * 하지만 문제는 해결되지 않았고, 모듈은 계속해서 재시작을 거듭했다.
> * 단순히 네트워크 문제라고 하기에는 발생 주기가 예측이 불가능했다. 사수님께 원인 추적 방법에 대한 피드백을 여쭤보았고 사수님은 몇 번의 명령어를 입력하시더니 나에게 `kubectl describe` 명령어의 결과를 보여주셨고 "메모리와 관련된 문제니 해당 부분을 찾아보라"고 조언해주셨다.
> * WebFlux 모듈이기 때문에 GC가 적용되지 않는다는 백그라운드 지식을 가진 상태로 DataDog의 Non-Heap Usage를 그래프를 통해 조회한 결과 다음과 같았다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/677c3f1e-21d9-4fe8-918f-bd511e864bb5.png)

> * `jvm.non_heap_memory`가 하루 동안 163MiB → 167MiB로 완만하게 우상향하고 있었다. 이는 JVM이 Non-Heap 영역의 메모리를 계속 점유하고 있다는 의미다. Non-Heap은 GC가 관리하는 Heap 영역이 아니기 때문에 자동으로 회수되지 않고 누적되고 있었다.
> * 원인을 파악하기 위해 먼저 WebFlux의 메모리 구조를 살펴봤다. Spring MVC(Tomcat)는 요청/응답 처리 시 Heap 기반 버퍼를 사용하며, 처리 완료 후 GC에 의해 자동 회수되지만 Spring Webflux(Netty)는 요청 처리 시 Direct Buffer Pool에서 버퍼를 할당하는데, 이는 Off-Heap 메모리 영역이라 GC의 대상이 되지 않았다. 다만 처리 완료 후 Pool로 반환되어 재사용되기 때문에 직접적인 누수 원인은 아니었다.
> * Linux의 기본 메모리 할당자(glibc malloc)에서 원인을 찾았다. glibc는 메모리를 관리할 때 arena 구조를 사용한다. JVM이 Non-Heap 영역에 메모리를 요청할 때 glibc가 arena 단위로 할당하는데 사용이 끝난 메모리 chunk가 arena 최상단에 위치하지 않으면 OS로 반환되지 않고 단편화된 채로 남는다. 회사의 인프라 환경은 K8S 환경이었고 이 K8S 환경에서 glibc의 기본 arena 수는 `8 x CPU 코어 수`로 설정되는데, 여기서 코어가 많을수록 arena가 늘어나 단편화가 심해지는 구조였다. 결과적으로 JVM의 Non-Heap 사용량이 지속적으로 누적되었고, OS 입장에서 메모리가 계속 점유된 것으로 보여 K8S의 OOMKilled(Out Of Memory Killed)가 발생한 것이었다.
> 
> **3. 해결 방법**
> 1. Dockerfile의 JVM 옵션에서 Heap 크기를 고정
> * Heap을 고정하면 JVM이 메모리를 동적으로 확장하지 않아 예측 가능한 메모리 사용이 가능하고 나머지 Non-Heap 및 Native 영역에 여유 공간을 확보할 수 있다. 컨테이너 메모리 limit(1280Mi) 중 384Min를 Heap에 고정 할당하고, 나머지를 Non-Heap과 Native가 나눠 쓰는 구조가 된다.
> 
> 2. MALLOC_ARENA_MAX=2 환경변수를 추가
> * glibc의 기본 arena 수를 2로 제한해 메모리 단편화를 줄이고, Non-Heap 메모리가 OS로 정상 반환될 수 있도록 했다.
>
> 3. memory request와 limit을 동일하게 설정해 Guaranteed QoS를 적용
```yml
resources:
  requests:
    memory: 1280Mi
  limits:
    memory: 1280Mi
```

#### 3. 실행 엔진(Execution Engine)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/af88c138-8128-49fb-bba6-b1950f258ddc.png)

**1. 인터프리터와 JIT의 상호작용(Adaptive Optimization)**

* 초기 단계 : 인터프리터가 바이트코드를 한 줄씩 읽으며 빠르게 실행을 시작한다.
* 프로파일링 : 실행 중에 JVM은 어떤 메서드가 자주 호출되는지 통계를 내기 시작한다.
* JIT 전환 : 특정 임계치를 넘긴 코드를 발견하면, JIT 컴파일러가 해당 바이트코드를 통째로 기계어로 번역해 캐싱해버린다. 이후부터는 인터프리터가 아니라 CPU가 직접 기계어를 실행하므로 속도가 폭발적으로 빨라진다.

**2. JIT 컴파일러의 두 종류(C1 vs C2)**

현대의 JVM(HotSpot)은 성능 극대화를 위해 두 가지 컴파일러를 섞어서 사용한다.
* C1 컴파일러(Client Compiler) : 최적화보다는 빠른 컴파일에 집중한다.
* C2 컴파일러(Server Compiler) : 컴파일 시간은 길지만, 고도로 최적화된 기계어를 만든다.

**3. 실행 엔진의 비밀 무기 : 인라이닝(Inlining)**

* 자주 호출되는 작은 메서드의 내용을 호출부로 직접 복사해 넣는다.
* 메서드 호출에 드는 비용을 완전히 제거해 실행 속도를 획기적으로 높인다.

**4. JIT와 웜업(Warm-up)의 이해**

* 서버가 처음 배포되었을 때 첫 응답들이 느린 경우가 있는데 이는 실행 엔진이 인터프리터 모드로 동작하고 있거나 JIT 컴파일러가 바쁘게 컴파일을 하고 있기 때문이다.

**5. 가비지 컬렉터(Garbage Collector)의 위상**

* 실행 엔진이 메모리를 할당하다가 자리가 부족해지면 GC 스레드를 깨운다.
* 이 때, Stop-the-world가 발생하면 실행 엔진의 다른 작업들이 잠시 멈추게 된다.

#### 4. 가비지 컬렉터(Garbage Collector)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9f7dc658-7a36-48df-8afb-7b776c56c9cc.png)

* 가비지 컬렉터(Garbage Collector) : 사용하지 않는 객체를 자동으로 메모리에서 정리해 메모리 누수를 방지하고 효율적인 메모리 관리를 수행한다,

#### 가비지 컬렉션(Garbage Collection, GC)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/7dfe4058-f122-48ec-8dd2-198ca6f09c2b.png)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/919bc7f1-130a-4dbd-b878-e5003f2a93de.png)

**1. GC의 대전제 : Weak Generational Hypothesis**

* 대부분의 객체는 금방 접근 불가능한 상태가 된다.
* 오래된 객체에서 젋은 객체로의 참조는 아주 적게 존재한다.
* 이러한 가설 덕분에 JVM은 전체 Heap을 다 뒤지지 않고, Young Generation 영역만 자주 청소하며 효율을 높인다.

**2. Mark and Sweep and Compact**

* Mark : 사용 중인 객체를 식별한다.
* Sweep : Mark 되지 않은 객체들을 메모리에서 제거한다.
* Compact : 살아남은 객체들을 메모리 한쪽으로 차곡차곡 쌓아 빈 공간(단편화, Fragmentation)을 없앤다. 이 과정이 있어야 나중에 큰 객체가 들어올 자리를 확보할 수 있다.

**3. Stop-the-world(STW)**

* GC를 실행하기 위해 JVM이 애플리케이션 실행을 멈추는 현상이다.

#### 가비지 컬렉터(Garbage Collector)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/301da895-a927-4eb7-95f3-6f56ceda692f.png)

#### 힙 메모리 내 객체 생존 흐름
* Eden 영역에서 객체를 생성
  * 자바 애플리케이션에서 new 키워드로 객체를 생성하면 대부분 Eden 영역에 생성된다.
  * Eden은 Young Generation에 속한다.
* Minor GC 발생
  * Eden 영역이 꽉 차면 Minor GC가 발생
  * GC는 살아있는 객체만을 Survivor 영역으로 복사
  * 살아남은 객체들은 Survivor 영역으로 이동하고 죽은 객체는 제거됨
* Survivor ↔ Survivor
  * 이후 다시 Eden 영역에 객체가 쌓이고 또 Minor GC가 발생하면 Survivor 영역에 있던 객체들은 다른 Survivor 영역으로 복사됨
  * 복사될 때마다 age값(생존 횟수)이 증가한다.
* Old Generation으로 Promotion
  * age값이 특정 임계값 이상이 되면 더 이상 Young Generation에 두지 않고 Old Generation으로 승격된다.
  * Old 영역은 Major GC의 대상이 된다.
>❗Minor GC란?
> * Young Generation에서 발생하는 GC
> * 빠르게 실행되며 Stop-the-World 시간이 짧다.

>❗Major GC란?
> * Old Generation에서 발생하는 GC
> * 힙 영역 전체를 검사하기 때문에 실행 시간이 길고 Stop-the-World 시간이 길어 성능에 영향을 준다.
> * GC 튜닝을 통해 Major GC 발생을 최소화하는 것이 중요하다.

#### GC 알고리즘

* G1 GC(Java 9+ 기본) : Heap을 바둑판 모양의 Region으로 잘게 나누어 관리한다. 쓰레기가 가장 많은 영역을 우선으로 치워 효율성이 좋다.
* ZGC (Java 15+) : 8MB부터 16TB까지의 거대한 메모리도 STW를 10ms 이하로 억제한다. 대규모 트래픽을 다루는 백엔드에서 선호한다.
