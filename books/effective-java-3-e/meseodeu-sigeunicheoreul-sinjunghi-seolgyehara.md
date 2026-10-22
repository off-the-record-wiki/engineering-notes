# (Effective Java Item 51) Java ‐ 메서드 시그니처를 신중히 설계하라

#### 메서드 이름을 신중히 지어야 한다.
* 표준 명명 규칙을 따라야 한다.
* 이해할 수 있고, 같은 패키지에 속한 다른 이름들과 일관되게 짓는 게 최우선 목표다.
* 그 다음 목표는 개발자 커뮤니티에서 널리 받아들여지는 이름을 사용하는 것이다.
* 긴 이름은 피해야 한다.
* 애매하다면 [자바 라이브러리의 API 가이드](https://docs.oracle.com/en/java/javase/21/docs/api/index.html)를 참조하자.

#### 편의 메서드를 너무 많이 만들진 말아야 한다.
* 모든 메서드는 각각 자신의 소임을 다해야 한다.
* 메서드가 너무 많은 클래스는 익히고, 사용하고, 문서화하고, 테스트하고, 유지보수하기가 어렵다.
* 클래스나 인터페이스는 자신의 각 기능을 완벽히 수행하는 메서드로 제공해야 한다.

#### 매개변수 목록은 짧게 유지해야 한다.
* 같은 타입의 매개변수가 여러 개 연달아 나오게 되면 상당히 좋지 않다.
* 사용자가 매개변수 순서를 기억하기 어려울뿐더러, 실수로 순서를 바꿔 입력해도 그대로 컴파일되고 실행된다.
* 단지 의도와 다르게 동작할 뿐이다.

1. 여러 메서드로 쪼갠다.
* 잘못하면 메서드가 너무 많아질 수 있지만, 직교성을 높여 오히려 메서드 수를 줄여주는 효과를 볼 수 있다.
```java
// Bad
public void processOrder(Order order) {
    // 1. 주문 유효성 검사
    if (order == null || order.getItems().isEmpty()) {
        System.out.println("Error: Invalid order.");
        return;
    }
    System.out.println("Order validated.");

    // 2. 결제 처리
    if (!processPayment(order.getAmount(), order.getPaymentInfo())) {
        System.out.println("Error: Payment failed.");
        return;
    }
    System.out.println("Payment successful.");

    // 3. 재고 업데이트
    for (Item item : order.getItems()) {
        inventorySystem.decreaseStock(item.getId(), item.getQuantity());
    }
    System.out.println("Inventory updated.");

    // 4. 주문 상태 변경
    order.setStatus("PROCESSED");
    orderRepository.save(order);
    System.out.println("Order status updated.");

    // 5. 고객에게 확인 이메일 발송
    emailService.sendConfirmationEmail(order.getCustomerEmail(), order.getId());
    System.out.println("Confirmation email sent.");
}
```
```java
// Good
public void processOrder(Order order) {
    validateOrder(order);
    processPayment(order);
    updateInventory(order);
    saveOrderStatus(order);
    sendConfirmationEmail(order);
    System.out.println("Order processing complete.");
}

private void validateOrder(Order order) {
    if (order == null || order.getItems().isEmpty()) {
        throw new IllegalArgumentException("Invalid order.");
    }
    System.out.println("Order validated.");
}

private void processPayment(Order order) {
    // 실제 결제 로직 호출
    if (!paymentGateway.charge(order.getAmount(), order.getPaymentInfo())) {
        throw new RuntimeException("Payment failed.");
    }
    System.out.println("Payment successful.");
}

private void updateInventory(Order order) {
    for (Item item : order.getItems()) {
        inventorySystem.decreaseStock(item.getId(), item.getQuantity());
    }
    System.out.println("Inventory updated.");
}

private void saveOrderStatus(Order order) {
    order.setStatus("PROCESSED");
    orderRepository.save(order);
    System.out.println("Order status updated.");
}

private void sendConfirmationEmail(Order order) {
    emailService.sendConfirmationEmail(order.getCustomerEmail(), order.getId());
    System.out.println("Confirmation email sent.");
}
```

2. 매개변수 여러 개를 묶어주는 도우미 클래스를 만든다.
```java
// Bad
public void generateReport(String reportTitle, Date startDate, Date endDate, boolean includeDetails, String formatType, String outputPath) {
    // 보고서 생성 로직
    System.out.println("Generating report: " + reportTitle);
}

reportService.generateReport("Sales_2024", someStartDate, someEndDate, true, "PDF", "/tmp/reports/");
```

```java
// Good
public class ReportConfig {
    private String reportTitle;
    private Date startDate;
    private Date endDate;
    private boolean includeDetails;
    private String formatType;
    private String outputPath;
}

public void generateReport(ReportConfig config) {
    // 보고서 생성 로직...
    System.out.println("Generating report: " + config.getReportTitle());
}

ReportConfig config = new ReportConfig.Builder("Sales_2024")
    .startDate(someStartDate)
    .endDate(someEndDate)
    .includeDetails(true)
    .formatType("PDF")
    .outputPath("/tmp/reports/")
    .build();

reportService.generateReport(config);
```

3. 객체 생성에 사용한 빌더 패턴을 메서드 호출에 응용한다.
```java
 Good
public class User {
    private final String userId;
    private final String userName;
    private final String email;
    private final String phone;
    private final String address;

    private User(Builder builder) {
        this.userId = builder.userId;
        this.userName = builder.userName;
        this.email = builder.email;
        this.phone = builder.phone;
        this.address = builder.address;
    }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }

    public static class Builder {
        private final String userId;
        private final String userName;

        private String email = null;
        private String phone = null;
        private String address = null;

        public Builder(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
```
* Lombok에서 제공하는 `@Builder`, `@Getter`, `@Setter` 등 어노테이션이 제공되니 보일러 플레이트 코드는 알아서 제거하도록 한다.

#### 매개변수 타입으로는 클래스보다 인터페이스가 낫다.
* 예를 들어, 메서드에 HashMap보다는 Map을 사용한다.
* 또한 boolean보다는 원소 2개짜리 열거 타입이 낫다.(단, 메서드 이름상 boolean을 받아야 의미가 더 명확하다면 그건 예외다)
```java
// Bad
// isDeliverable이 true이면 배송 가능한지 아니면 배송 불가능한지?
// 변수명으로는 명확한 의미 파악이 어렵다.
public class Order {
    private boolean isDeliverable;

    public void setDeliverable(boolean deliverable) {
        this.isDeliverable = deliverable;
    }

    public boolean isDeliverable() {
        return this.isDeliverable;
    }
}
```

```java
// Good
public enum DeliveryStatus {
    DELIVERABLE,    // 배송 가능
    UNDELIVERABLE   // 배송 불가능
}

public class Order {
    private DeliveryStatus status;

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public DeliveryStatus getStatus() {
        return this.status;
    }
}
```
