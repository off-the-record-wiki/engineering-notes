# 개발자가 반드시 정복해야 할 객체 지향과 디자인 패턴 - 설계 원칙: SOLID

#### 단일 책임 원칙(SRP, Single Responsibility Principle)

* 클래스가 여러 책임을 갖게 되면 그 클래스는 각 책임마다 변경 이유가 발생하기 때문에, 클래스가 한 개의 이유로만 변경되려면 클래스는 한 개의 책임만을 가져야 한다.
* 객체에게 책임을 할당하는 것이 객체 설계의 기본인 만큼, 단일 책임 원칙은 가장 중요한 원칙 중의 하나이다.
* 단일 책임 원칙이 잘 지켜지지 않으면 다른 원칙들도 그 효과가 반감되기 때문에 최대한 지켜야 하는 원칙이다.

```java
// ❌ Bad(클래스가 바뀌는 이유는 4가지)
class User {
    private String name;
    private String email;

    // 책임 1: 사용자 데이터 (도메인)
    public String getName() { return name; }

    // 책임 2: 데이터베이스 저장 (영속성)
    public void save() {
        Connection conn = DriverManager.getConnection("jdbc:...");
        // INSERT INTO users ... 직접 SQL 실행
    }

    // 책임 3: 이메일 발송 (인프라)
    public void sendWelcomeEmail() {
        SmtpClient smtp = new SmtpClient("smtp.gmail.com");
        smtp.send(email, "환영합니다", "가입을 축하합니다");
    }

    // 책임 4: 리포트 형식 (표현)
    public String toHtmlReport() {
        return "<h1>" + name + "</h1><p>" + email + "</p>";
    }
}
```
```java
// ⭕ Good
// 책임 1: 사용자 데이터만 (도메인)
class User {
    private final String name;
    private final String email;

    User(String name, String email) {
        this.name = name;
        this.email = email;
    }
    public String getName() { return name; }
    public String getEmail() { return email; }
}

// 책임 2: 저장만 (영속성)
class UserRepository {
    public void save(User user) {
        // DB 저장 로직 — DB 기술이 바뀌면 여기만 수정
    }
}

// 책임 3: 이메일 발송만 (인프라)
class EmailService {
    public void sendWelcome(User user) {
        // 메일 발송 로직 — 메일 방식이 바뀌면 여기만 수정
    }
}

// 책임 4: 리포트 형식만 (표현)
class UserReportFormatter {
    public String toHtml(User user) {
        return "<h1>" + user.getName() + "</h1><p>" + user.getEmail() + "</p>";
    }
}

// 조립해서 사용 — 각자 자기 일만 함
class UserSignupService {
    private final UserRepository repository;
    private final EmailService emailService;

    UserSignupService(UserRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    public void signup(User user) {
        repository.save(user);
        emailService.sendWelcome(user);
    }
}
```

#### 개방 폐쇄 원칙(OCP, Open Closed Principle)

* 확장에는 열려 있어야 하고, 변경에는 닫혀 있어야 한다.
* 즉, 기능을 변경하거나 확장할 수 있으면서 그 기능을 사용하는 코드는 수정하지 않는다.

```java
// ❌ Bad(새 종류가 생길 때마다 기존 코드를 수정)
class DiscountCalculator {
    public int calculate(String memberType, int price) {
        if (memberType.equals("BASIC")) {
            return price;                         // 할인 없음
        } else if (memberType.equals("SILVER")) {
            return (int)(price * 0.95);           // 5%
        } else if (memberType.equals("GOLD")) {
            return (int)(price * 0.90);           // 10%
        }
        // VIP 추가? → 여기 또 수정
        // PLATINUM 추가? → 또 수정...
        return price;
    }
}
```
```java
// ⭕ Good(확장 지점을 인터페이스로 고정)
interface DiscountPolicy {
    int calculate(int price);
}

// 1. 각 정책은 독립된 클래스 — 서로 모름
class BasicDiscount implements DiscountPolicy {
    public int calculate(int price) { return price; }
}
class SilverDiscount implements DiscountPolicy {
    public int calculate(int price) { return (int)(price * 0.95); }
}
class GoldDiscount implements DiscountPolicy {
    public int calculate(int price) { return (int)(price * 0.90); }
}

// 2. 계산기는 구체 정책을 모르고, 인터페이스에만 의존
class DiscountCalculator {
    public int calculate(DiscountPolicy policy, int price) {
        return policy.calculate(price);   // 절대 수정될 일 없음
    }
}
```

#### 개방 폐쇄 원칙이 깨질 때의 주요 증상

**1. 다운 캐스팅을 한다.**

```java
// ❌ Bad(이 객체가 사실 어떤 자식인지 내가 알아야겠다고 선언하는 것이라, 다형성을 스스로 무너뜨린다)
interface Animal {
    void move();
}

class Dog implements Animal {
    public void move() { System.out.println("달린다"); }
    public void bark() { System.out.println("멍멍"); }   // Dog만의 기능
}

class Bird implements Animal {
    public void move() { System.out.println("난다"); }
    public void fly()  { System.out.println("훨훨"); }    // Bird만의 기능
}

class AnimalService {
    public void handle(Animal animal) {
        animal.move();

        // 타입을 캐내서 다운캐스팅 → OCP 붕괴
        if (animal instanceof Dog) {
            ((Dog) animal).bark();
        } else if (animal instanceof Bird) {
            ((Bird) animal).fly();
        }
        // Cat 추가? → 여기 또 instanceof + 캐스팅 추가...
    }
}
```

**2. 비슷한 if-else 블록이 존재한다.**

```java
// ❌ Bad(결제 수단(type)으로 분기하는 switch가 수수료 계산)
class PaymentService {

    int calculateFee(String type, int amount) {
        switch (type) {
            case "CARD":   return (int)(amount * 0.03);
            case "KAKAO":  return (int)(amount * 0.02);
            case "BANK":   return 0;
            default: throw new IllegalArgumentException();
        }
    }

    String getLabel(String type) {          // 또 같은 분기
        switch (type) {
            case "CARD":   return "신용카드";
            case "KAKAO":  return "카카오페이";
            case "BANK":   return "계좌이체";
            default: throw new IllegalArgumentException();
        }
    }

    boolean isValid(String type, int amount) {  // 또 같은 분기
        switch (type) {
            case "CARD":   return amount <= 1_000_000;
            case "KAKAO":  return amount <= 500_000;
            case "BANK":   return true;
            default: throw new IllegalArgumentException();
        }
    }
}
```
* 개방 페쇄 원칙은 변경의 유연함과 관련된 원칙이다. 만약 기존 기능을 확장하기 위해 기존 코드를 수정해 주어야 한다면 새로운 기능을 추가하는 것이 점점 힘들어진다.
* 즉, 확장에는 닫히고 변경에는 열리는 반대 상황이 발생한다.
* 개방 폐쇄 원칙은 변화가 예상되는 곳을 추상화해서 변경의 유연함을 얻도록 해준다. 변화되는 부분을 추상화하지 못하면 개방 폐쇄 원칙을 지킬 수 없게 되어 시간이 흐를수록 기능 변경이나 확장을 어렵게 만든다는 것을 뜻한다. 따라서 코드에 대한 변화 요구가 발생하면 변화와 관련된 구현을 추상화해서 개방 폐쇄 원칙에 맞게 수정할 수 있는지 확인해야 한다.

#### 리스코프 치환 원칙(LSP, Liskov Substitution Principle)

* 상위 타입 객체를 하위 타입 객체로 치환해도 상위 타입을 사용하는 프로그램은 정상적으로 동작해야 한다.

```java
// ⭕ Good(대체 가능한 "약속"만 공유 — 넓이를 구할 수 있다는 것뿐)
interface Shape {
    int area();
}

class Rectangle implements Shape {
    private final int width;
    private final int height;

    Rectangle(int width, int height) {
        this.width = width;
        this.height = height;
    }
    public int area() { return width * height; }
}

class Square implements Shape {
    private final int side;

    Square(int side) { this.side = side; }
    public int area() { return side * side; }
}
```

#### 인터페이스 분리 원칙(ISP, Interface Segregation Principle)

* 인터페이스는 그 인터페이스를 사용하는 클라이언트를 기준으로 분리해야 한다.
* 인터페이스 분리 원칙은 자신이 사용하는 메서드에만 의존해야 한다는 원칙인데, 만약 하나의 클래스 멤버 함수에 대한 시그니처 변경이 발생할 경우 모든 코드를 재컴파일해야 하는 상황이 발생하게 된다.

```java
// ⭕ Good(역할별로 분리)
interface Printer {
    void print(Doc d);
}
interface Scanner {
    void scan(Doc d);
}
interface Fax {
    void fax(Doc d);
}

// 필요한 능력만 골라 구현
class OldPrinter implements Printer {
    public void print(Doc d) { /* ... */ }   // 딱 이것만. 억지 구현 없음
}

// 여러 능력이 필요하면 조합해서 구현
class AllInOne implements Printer, Scanner, Fax {
    public void print(Doc d) { /* ... */ }
    public void scan(Doc d)  { /* ... */ }
    public void fax(Doc d)   { /* ... */ }
}
```

#### 의존 역전 원칙(DIP, Dependency Inversion Principle)

* 고수준 모듈은 저수준 모듈의 구현에 의존해선 안 된다. 저수준 모듈이 고수준 모듈에서 정의한 추상 타입에 의존해야 한다.
  * 예를 들어, 고수준 모듈이라 함은 "바이트 데이터를 읽어와 암호화하고 결과 바이트 데이터를 쓴다." 이렇게 "무엇"을 정의한다.
  * 예를 들어, 저수준 모듈이라 함은 "파일에서 바이트 데이터를 읽어온다", "AES 알고리즘으로 암호화한다", "파일에 바이트 데이터를 쓴다"와 같이 "어떻게"를 정의한다.
* 소스 코드 의존과 런타임 의존
