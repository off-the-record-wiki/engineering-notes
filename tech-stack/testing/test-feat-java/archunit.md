# Test - ArchUnit

#### [ArchUnit](https://github.com/TNG/archUnit)

* 애플리케이션의 아키텍처를 테스트할 수 있는 오픈소스 라이브러리로 패키지, 클래스, 레이어, 슬라이스 간의 의존성을 확인할 수 있는 기능을 제공한다.
* 아키텍처 테스트 유스 케이스
  * A라는 패키지가 B(또는 C, D) 패키지에서만 사용되고 있는지 확인할 수 있다.
  * "Service"라는 이름의 클래스들이 "Controller" 또는 "Service"라는 이름의 클래스에서만 참조하고 있는지 확인할 수 있다.
  * "Service"라는 이름의 클래스들이 `..service..`라는 패키지에 들어있는지 확인할 수 있다.
  * A라는 어노테이션을 선언한 메서드만 특정 패키지 또는 특정 어노테이션을 가진 클래스를 호출하고 있는지 확인할 수 있다.
  * 특정한 스타일의 아키텍처를 준수하고 있는지 확인할 수 있다.

#### ArchUnit 아키텍처 규칙 검증

```xml
<!-- Source: https://mvnrepository.com/artifact/com.tngtech.archunit/archunit-junit5 -->
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.4.2</version>
    <scope>test</scope>
</dependency>
```

* 특정 패키지에 해당하는 클래스를 바이트 코드를 통해 읽어들여 확인할 규칙을 정의. 읽어들인 클래스들이 그 규칙을 잘 따르는지 확인한다.

1. 패키지 의존성 검증

```java
// ⭕ Good(레이어드 아키텍처 검증 - 계층 간 의존 방향 강제)
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class LayeredArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.example.myapp");

    @Test
    void 계층_간_의존_방향을_지킨다() {
        layeredArchitecture()
                .consideringAllDependencies()
                .layer("Controller").definedBy("..controller..")
                .layer("Service").definedBy("..service..")
                .layer("Repository").definedBy("..repository..")
                .layer("Entity").definedBy("..entity..")

                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                .whereLayer("Entity").mayOnlyBeAccessedByLayers("Service", "Repository")

                .check(classes);
    }
}
```
```java
// ⭕ Good(특정 패키지 의존성 감지 - domain이 infrastructure를 참조하면 안 된다.)
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Test
void domain_패키지는_infrastructure에_의존하지_않는다() {
    noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .check(classes);
}
```

```java
// ⭕ Good(순환 참조 검증)
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@Test
void 패키지_간_순환_참조가_없어야_한다() {
    slices()
            .matching("com.example.myapp.(*)..")
            .should().beFreeOfCycles()
            .check(classes);
}
```

2. 네이밍/컨벤션 검증

```java
// ⭕ Good(네이밍 컨벤션 강제)
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Test
void Repository로_끝나는_클래스는_인터페이스여야_한다() {
    classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().beInterfaces()
            .check(classes);
}

@Test
void Service_클래스는_Service_어노테이션이_붙어야_한다() {
    classes()
            .that().resideInAPackage("..service..")
            .and().areNotInterfaces()
            .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
            .check(classes);
}
```
```java
// ⭕ Good(필드 주입 금지 - 생성자 주입 강제)
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

@Test
void Autowired_필드_주입을_금지한다() {
    noFields()
            .should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
            .check(classes);
}
```

```java
// ⭕ Good(DTO는 도메인 로직을 가지면 안 된다)
@Test
void DTO는_public_메서드를_최소화한다() {
    classes()
            .that().haveSimpleNameEndingWith("Dto")
            .should().onlyHaveDependentClassesThat().resideInAnyPackage("..controller..", "..service..")
            .check(classes);
}
```

#### ArchUnit으로 클래스 의존성 확인

```java
// ⭕ Good(특정 클래스가 다른 특정 클래스에 의존하면 안 된다)
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Test
void OrderService는_PaymentGateway를_직접_의존하지_않는다() {
    noClasses()
            .that().areAssignableTo(OrderService.class)
            .should().dependOnClassesThat().areAssignableTo(PaymentGateway.class)
            .check(classes);
}
```
```java
// ⭕ Good(특정 클래스는 지정된 클래스에게만 접근을 허용한다 - 캡슐화 강제)
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Test
void InternalOrderProcessor는_OrderFacade에서만_접근한다() {
    classes()
            .that().haveSimpleName("InternalOrderProcessor")
            .should().onlyBeAccessed().byClassesThat()
            .haveSimpleName("OrderFacade")
            .check(classes);
}
```

```java
// ⭕ Good(특정 클래스는 지정한 클래스만 의존해야 한다 - 화이트리스트 방식)
@Test
void PaymentValidator는_허용된_클래스만_의존한다() {
    classes()
            .that().haveSimpleName("PaymentValidator")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("java..", "..payment.validation..", "..payment.exception..")
            .check(classes);
}
```

```java
// ⭕ Good(양방향 의존 금지 - A가 B를 의존하면 B는 A를 의존하면 안 된다)
@Test
void OrderService와_InventoryService는_서로_의존하지_않는다() {
    noClasses()
            .that().haveSimpleName("OrderService")
            .should().dependOnClassesThat().haveSimpleName("InventoryService")
            .andShould().onlyBeAccessed().byAnyPackage("..order..")
            .check(classes);

    noClasses()
            .that().haveSimpleName("InventoryService")
            .should().dependOnClassesThat().haveSimpleName("OrderService")
            .check(classes);
}
```

```java
// ⭕ Good(구체 클래스가 아닌 인터페이스에만 의존해야 한다 - DIP 강제)
@Test
void Service는_Repository_구현체가_아닌_인터페이스에_의존한다() {
    noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat()
            .haveNameMatching(".*RepositoryImpl")
            .check(classes);
}
```

```java
// ⭕ Good(특정 클래스가 필드를 통해 직접 접근되는 것을 금지 - getter 강제)
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Test
void Order_필드에_외부에서_직접_접근하지_않는다() {
    noClasses()
            .that().resideOutsideOfPackage("..order..")
            .should().accessFieldWhere(field -> field.getOwner().isEquivalentTo(Order.class))
            .check(classes);
}
```

* 클래스 단위 규칙은 특정 도메인 객체의 캡슐화(예: Order 필드 직접 접근 금지)나 핵심 클래스 간 결합도 제한(예: 서비스 간 순환 의존 금지)처럼, 패키지 단위로는 표현하기 어려운 세밀한 제약을 걸 때 유용하다.
