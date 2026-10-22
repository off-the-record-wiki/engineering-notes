# (Effective Kotlin Item 35) Kotlin - 의존성 주입을 고려하라

#### 의존성 주입을 고려하라

* 의존성 주입은 다른 객체에 의존하는 객체를 생성할 때, 의존성을 직접 생성하지 않고록 해주는 기법이다.
* 이 기법은 코드를 유연하고 재사용할 수 있게 만들어주므로 중요하다.
* 코틀린에서 널리 사용되는 의존성 주입 기법은 생성자 기반 의존성 주입(constructor-based dependency injection)이다.
* 클래스가 구성해야 하는 다른 클래스의 인스턴스를 직접 생성하는 대신, 클래스의 생성자에서 의존성 타입을 지정한다. 
* 이렇게 하면 클래스가 자체적으로 의존성을 생성하는 대신 외부에서 의존성을 주입받게 된다. 이것을 제어의 역전(inversion of control)이라고 한다.

```kotlin
// ❌ Bad - UserService가 의존성을 직접 생성 → 강하게 결합됨
class UserService {
    private val repository = MySqlUserRepository()   // 구체 구현을 직접 만듦
    private val emailSender = SmtpEmailSender()       // 여기도

    fun register(name: String) {
        val user = repository.save(name)
        emailSender.send(user.email, "환영합니다")
    }
}
```
```kotlin
// ⭕ Good - 생성자로 의존성을 받음(무엇을 쓸지는 바깥이 결정)
class UserService(
    private val repository: UserRepository,   // 인터페이스에 의존
    private val emailSender: EmailSender,
) {
    fun register(name: String) {
        val user = repository.save(name)
        emailSender.send(user.email, "환영합니다")
    }
}
```

* 의존성 주입의 장점은 다음과 같다.
  * **의존성을 명시적으로 보여준다. 클래스의 생성자를 보면 해당 클래스가 어떤 의존성을 가지는지 알 수 있다.**
  * **테스트가 더 쉬워진다. 의존성을 쉽게 모킹하고 클래스를 격리하여 테스트를 실행할 수 있다.**
  * **코드가 더 유연해진다. 의존성을 다른 구현체로 쉽게 교체할 수 있다.**
  * **코드의 재사용성이 높아진다. 클래스에서 의존성을 인터페이스로 정의하면 다른 컨텍스트에서 사용해야 할 때 인터페이스의 구현체만 변경해 해당 클래스를 재사용할 수 있다.**
* 의존성 주입을 사용하면 실제 객체의 생성은 활성 객체의 외부에 위임된다.
* 의존성 주입을 하더라도 의존성을 어떻게 생성할지는 정의해야 한다.

```kotlin
// Koin 코틀린 의존성 주입 프레임워크
val appModule = module {
    single<OrderRepository> { MySqlOrderRepository() }
    single<PaymentGateway> { StripeGateway() }
    single { OrderService(get(), get()) }  // get()이 위 의존성을 자동으로 연결
}
```
* 의존성 주입 프레임워크는 다음과 같은 장점이 있다.
  * **컴포넌트가 어떻게 생성되어야 하는지를 한 번만 정의하면 된다. 그 후에는 의존성을 정의하기만 하면 의존성 주입 프레임워크에서 의존성을 생성한다.**
  * **의존성을 다른 구현체로 쉽게 대체할 수 있다. 의존성 생성 방법에 대한 정의만 변경하면 된다.**
  * **테스트에서 의존성을 쉽게 모킹할 수 있다. 테스트를 위한 별도의 의존성 구현체를 제공할 수 있다.**
  * **다른 컨텍스트에서 클래스를 쉽게 재사용할 수 있다. 컨텍스트에 따라 서로 다른 의존성 구현체를 제공할 수 있다.**
  * **한 번 생성되면 재사용되는 의존성인 싱글톤을 쉽게 생성할 수 있다.**
