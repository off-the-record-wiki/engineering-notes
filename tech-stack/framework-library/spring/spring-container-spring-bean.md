# Spring - Spring Container & Spring Bean

#### IoC(Inversion of Control), DI(Dependency Injection), 그리고 컨테이너

* **IoC(Inversion of Control)** : 프로그램 제어 흐름을 직접 제어하는 것이 아니라 외부에서 관리하는 것
* 프레임워크(Framework)와 라이브러리(Library)의 차이점
  * 둘의 차이는 제어 흐름에 대한 주도권이 어디에 있는가의 차이이다.
  * 프레임워크란 응용 프로그램이나 소프트웨어 솔루션 개발을 수월하기 위해 구조, 틀이 제공된 소프트웨어 환경이다.
  * 라이브러리란 특정 기능을 수행하는 코드의 집합이다.
* 프레임워크의 경우 제어 흐름을 스스로 가지고 있는 반면에 라이브러리의 경우 제어 흐름을 개발자가 가지고 있다.

> [📚IoC(Inversion of Control, 제어의 역전)](https://en.wikipedia.org/wiki/Inversion_of_control)
> 
> * IoC란, 객체의 생성부터 소멸까지의 주도권을 개발자가 아닌 프레임워크(Container)가 가져가는 설계 패러다임이다.
> 
> **1. IoC의 개념적 로직**
> * 전통적 제어 : 필요한 도구를 직접 선택하고 new 키워드로 생성하며 다 쓰면 버리는 과정을 코드로 직접 작성한다.
> * 역전된 제어 : 도구의 규격을 정의하고 실제 도구를 가져와서 조립하고 관리하는 역할을 프레임워크에 맡긴다.
>
> **2. 구현의 실체 : Singleton과 Lifecycle**
> * 싱글톤 기반의 효율적 관리 : 대부분의 백엔드 객체가 상태를 가지지 않는 경우가 많은데 프레임워크는 IoC 컨테이너 내에서 이들을 싱글톤으로 생성하여 메모리 낭비를 줄이고, 어디서든 동일한 인스턴스를 공유하도록 보장한다.
> * 컨테이너 생명 주기 종속 : 애플리케이션 시작 시 컨테이너가 빈을 스캔하고 생성, 애플리케이션 종료 시 컨테이너가 자원을 반납하고 소멸한다.

#### 스프링 컨테이너 생성

* `ApplicationContext`를 스프링 컨테이너라고 한다.
* 스프링 컨테이너를 구성 후 구성 정보 `AppConfiguration`클래스를 구성 정보로 지정한다.
* 스프링 컨테이너는 설정 정보(Ex. AppConfiguration Class)를 참고해서 의존관계를 주입한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ece1465c-6ef6-44c4-881e-961085ba5e76.png)

```java
ApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfiguration.class)
```

```java
@Configuration
public class AppConfiguration {

	@Bean
	public MemberRepository memberRepository() {
		return new MemberRepositoryImpl();
	}

	@Bean
	public MemberService memberService() {
		return new MemberServiceImpl(memberRepository());
	}

	@Bean
	public OrderService orderService() {
		return new OrderServiceImpl(memberRepository(), discountPolicy());
	}

	@Bean
	public DiscountPolicy discountPolicy() {
		return new FixDiscountPolicy();
		// return new RateDiscountPolicy();
	}
}
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/1858d81d-ecb0-4bb6-b4c8-26619e9efa80.png)

<br>

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/0eaa9b6e-64bd-4b7b-825b-e4f1e8a145b9.png)

<br>

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/bd4dd540-d0d7-400f-a2ee-64a65c0cfbfe.png)

#### 컨테이너에 등록된 모든 빈 조회

```java
class ApplicationContextInfoTest {

	//AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(AppConfiguration.class);
	ApplicationContext ac = new AnnotationConfigApplicationContext(AppConfiguration.class);

	@Test
	@DisplayName("모든 빈을 조회한다.")
	void findAllBean() {

		String[] beanDefinitionNames = ac.getBeanDefinitionNames();
		for (String beanDefinitionName : beanDefinitionNames) {
			Object bean = ac.getBean(beanDefinitionName);
			System.out.println("name = " + beanDefinitionName + ", object = " + bean);
		}
	}
}
```

> 실행 결과

```
// 스프링 내부에서 사용하는 빈
name = org.springframework.context.annotation.internalConfigurationAnnotationProcessor, object = org.springframework.context.annotation.ConfigurationClassPostProcessor@66ea1466
name = org.springframework.context.annotation.internalAutowiredAnnotationProcessor, object = org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor@f415a95
name = org.springframework.context.annotation.internalCommonAnnotationProcessor, object = org.springframework.context.annotation.CommonAnnotationBeanPostProcessor@cf65451
name = org.springframework.context.event.internalEventListenerProcessor, object = org.springframework.context.event.EventListenerMethodProcessor@724f138e
name = org.springframework.context.event.internalEventListenerFactory, object = org.springframework.context.event.DefaultEventListenerFactory@37eeec90

// 사용자가 정의한 빈
name = appConfiguration, object = com.jwj.springPrinciple.config.AppConfiguration$$SpringCGLIB$$0@32fe9d0a
name = memberRepository, object = com.jwj.springPrinciple.repository.MemberRepositoryImpl@c9413d8
name = memberService, object = com.jwj.springPrinciple.service.MemberServiceImpl@64da2a7
name = orderService, object = com.jwj.springPrinciple.service.OrderServiceImpl@46074492
name = discountPolicy, object = com.jwj.springPrinciple.discount.FixDiscountPolicy@d78795
```

* 애플리케이션 빈을 구분해서 확인하고 싶다면 아래와 같이 작성하면 된다.

```java
class ApplicationContextInfoTest {

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(AppConfiguration.class);

    @Test
    @DisplayName("모든 빈을 조회한다.")
    void findAllBean() {
        String[] beanDefinitionNames = ac.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = ac.getBean(beanDefinitionName);
            System.out.println("name = " + beanDefinitionName + ", object = " + bean);
        }
    }

    @Test
    @DisplayName("빈을 선택적으로 조회한다.")
    void findApplicationBean() {
        String[] beanDefinitionNames = ac.getBeanDefinitionNames();

        for (String beanDefinitionName : beanDefinitionNames) {
            BeanDefinition beanDefinition = ac.getBeanDefinition(beanDefinitionName);

            // ROLE_APPLICATION: 일반적으로 사용자가 정의한 빈
            // ROLE_INFRASTRUCTURE: 스프링이 내부에서 사용하는 빈
            if (beanDefinition.getRole() == BeanDefinition.ROLE_APPLICATION) {
                Object bean = ac.getBean(beanDefinitionName);
                System.out.println("name = " + beanDefinitionName + ", object = " + bean);
            }
        }
    }
}
```

> ❓AnnotationConfigApplicationContext vs ApplicationContext 차이점
> 
> **1. ApplicationContext(인터페이스)**
> * 스프링 컨테이너의 핵심 규격
> * 빈 관리, 이벤트 발행, 메시지 처리 등 스프링의 핵심 기능을 정의
> * 한계 : 범용적인 인터페이스이기 때문에 특정 설정 방식(자바 설정, XML 등)에 특화된 상세 메서드는 포함하지 않는다.
>
> **2. AnnotationConfigApplicationContext(구현체)**
> * 자바 설정 클래스(@Configuration)와 어노테이션(@Component, @Bean)을 읽어 빈을 생성하는 실제 클래스이다.
> * `GenericApplicationContext`를 상속받으며, 내부적으로 `BeanDefinitionRegistry` 인터페이스를 구현하고 있다.
> * `getBeanDefinition()` 메서드는 빈 정보를 조회하기 위한 기능으로 이 메서드는 `BeanDefinitionRegistry`에 정의되어 있고 `AnnotationConfigApplicationContext`가 이를 구현하고 있기 때문에 호출이 가능하다.

```java
public class AnnotationConfigApplicationContext extends GenericApplicationContext implements AnnotationConfigRegistry {

	private final AnnotatedBeanDefinitionReader reader;

	private final ClassPathBeanDefinitionScanner scanner;


	/**
	 * Create a new AnnotationConfigApplicationContext that needs to be populated
	 * through {@link #register} calls and then manually {@linkplain #refresh refreshed}.
	 */
	public AnnotationConfigApplicationContext() {
		StartupStep createAnnotatedBeanDefReader = getApplicationStartup().start("spring.context.annotated-bean-reader.create");
		this.reader = new AnnotatedBeanDefinitionReader(this);
		createAnnotatedBeanDefReader.end();
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * Create a new AnnotationConfigApplicationContext with the given DefaultListableBeanFactory.
	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
	 */
	public AnnotationConfigApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
		this.reader = new AnnotatedBeanDefinitionReader(this);
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * Create a new AnnotationConfigApplicationContext, deriving bean definitions
	 * from the given component classes and automatically refreshing the context.
	 * @param componentClasses one or more component classes &mdash; for example,
	 * {@link Configuration @Configuration} classes
	 */
	public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
		this();
		register(componentClasses);
		refresh();
	}

	/**
	 * Create a new AnnotationConfigApplicationContext, scanning for components
	 * in the given packages, registering bean definitions for those components,
	 * and automatically refreshing the context.
	 * @param basePackages the packages to scan for component classes
	 */
	public AnnotationConfigApplicationContext(String... basePackages) {
		this();
		scan(basePackages);
		refresh();
	}


	/**
	 * Propagate the given custom {@code Environment} to the underlying
	 * {@link AnnotatedBeanDefinitionReader} and {@link ClassPathBeanDefinitionScanner}.
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(environment);
		this.scanner.setEnvironment(environment);
	}

	/**
	 * Provide a custom {@link BeanNameGenerator} for use with {@link AnnotatedBeanDefinitionReader}
	 * and/or {@link ClassPathBeanDefinitionScanner}, if any.
	 * <p>Default is {@link AnnotationBeanNameGenerator}.
	 * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
	 * and/or {@link #scan(String...)}.
	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
	 * @see AnnotationBeanNameGenerator
	 * @see FullyQualifiedAnnotationBeanNameGenerator
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.reader.setBeanNameGenerator(beanNameGenerator);
		this.scanner.setBeanNameGenerator(beanNameGenerator);
		getBeanFactory().registerSingleton(
				AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
	}

	/**
	 * Set the {@link ScopeMetadataResolver} to use for registered component classes.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
	 * and/or {@link #scan(String...)}.
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.reader.setScopeMetadataResolver(scopeMetadataResolver);
		this.scanner.setScopeMetadataResolver(scopeMetadataResolver);
	}


	//---------------------------------------------------------------------
	// Implementation of AnnotationConfigRegistry
	//---------------------------------------------------------------------

	/**
	 * Register one or more component classes to be processed.
	 * <p>Note that {@link #refresh()} must be called in order for the context
	 * to fully process the new classes.
	 * @param componentClasses one or more component classes &mdash; for example,
	 * {@link Configuration @Configuration} classes
	 * @see #scan(String...)
	 * @see #refresh()
	 */
	@Override
	public void register(Class<?>... componentClasses) {
		Assert.notEmpty(componentClasses, "At least one component class must be specified");
		StartupStep registerComponentClass = getApplicationStartup().start("spring.context.component-classes.register")
				.tag("classes", () -> Arrays.toString(componentClasses));
		this.reader.register(componentClasses);
		registerComponentClass.end();
	}

	/**
	 * Perform a scan within the specified base packages.
	 * <p>Note that {@link #refresh()} must be called in order for the context
	 * to fully process the new classes.
	 * @param basePackages the packages to scan for component classes
	 * @see #register(Class...)
	 * @see #refresh()
	 */
	@Override
	public void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		StartupStep scanPackages = getApplicationStartup().start("spring.context.base-packages.scan")
				.tag("packages", () -> Arrays.toString(basePackages));
		this.scanner.scan(basePackages);
		scanPackages.end();
	}


	//---------------------------------------------------------------------
	// Adapt superclass registerBean calls to AnnotatedBeanDefinitionReader
	//---------------------------------------------------------------------

	@Override
	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass,
			@Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

		this.reader.registerBean(beanClass, beanName, supplier, customizers);
	}

}
```
```java
public interface BeanDefinitionRegistry extends AliasRegistry {

	/**
	 * Register a new bean definition with this registry.
	 * Must support RootBeanDefinition and ChildBeanDefinition.
	 * @param beanName the name of the bean instance to register
	 * @param beanDefinition definition of the bean instance to register
	 * @throws BeanDefinitionStoreException if the BeanDefinition is invalid
	 * @throws BeanDefinitionOverrideException if there is already a BeanDefinition
	 * for the specified bean name and we are not allowed to override it
	 * @see GenericBeanDefinition
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 */
	void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException;

	/**
	 * Remove the BeanDefinition for the given name.
	 * @param beanName the name of the bean instance to register
	 * @throws NoSuchBeanDefinitionException if there is no such bean definition
	 */
	void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * Return the BeanDefinition for the given bean name.
	 * @param beanName name of the bean to find a definition for
	 * @return the BeanDefinition for the given name (never {@code null})
	 * @throws NoSuchBeanDefinitionException if there is no such bean definition
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * Check if this registry contains a bean definition with the given name.
	 * @param beanName the name of the bean to look for
	 * @return if this registry contains a bean definition with the given name
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * Return the names of all beans defined in this registry.
	 * @return the names of all beans defined in this registry,
	 * or an empty array if none defined
	 */
	String[] getBeanDefinitionNames();

	/**
	 * Return the number of beans defined in the registry.
	 * @return the number of beans defined in the registry
	 */
	int getBeanDefinitionCount();

	/**
	 * Determine whether the bean definition for the given name is overridable,
	 * i.e. whether {@link #registerBeanDefinition} would successfully return
	 * against an existing definition of the same name.
	 * <p>The default implementation returns {@code true}.
	 * @param beanName the name to check
	 * @return whether the definition for the given bean name is overridable
	 * @since 6.1
	 */
	default boolean isBeanDefinitionOverridable(String beanName) {
		return true;
	}

	/**
	 * Determine whether the given bean name is already in use within this registry,
	 * i.e. whether there is a local bean or alias registered under this name.
	 * @param beanName the name to check
	 * @return whether the given bean name is already in use
	 */
	boolean isBeanNameInUse(String beanName);

}
```

#### 스프링 빈 조회 - 기본

```java
@Test
@DisplayName("특정 타입의 빈을 조회한다. - 1")
void findBeanByName() {
    // ApplicationContext.getBean(타입)
    Object bean = ac.getBean("discountPolicy");
    assertThat(bean).isInstanceOf(DiscountPolicy.class);
}

@Test
@DisplayName("특정 타입의 빈을 조회한다. - 2")
void findBeanByType() {
    // ApplicationContext.getBean(빈 이름, 타입)
    MemberService memberService = ac.getBean("memberService", MemberService.class);
    assertThat(memberService).isInstanceOf(MemberService.class);
}

@Test
@DisplayName("빈을 찾을 수 없다면 예외가 발생한다.")
void findBeanNotFound() {
    // ac.getBean() 호출 시 존재하지 않는 빈 이름을 넘기면 예외가 발생해야 함
    assertThrows(NoSuchBeanDefinitionException.class, 
            () -> ac.getBean("XXXXX", MemberService.class));
}
```

#### 스프링 빈 조회 - 동일한 타입이 둘 이상인 경우

```java
public class ApplicationContextSameBeanFindTest {

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(SameBeanConfigClass.class);

    @Test
    @DisplayName("동일한 타입의 빈이 두 개 이상 조회될 경우 중복 오류가 발생한다.")
    void findBeanByTypeDuplicate() {
        // NoUniqueBeanDefinitionException 예외가 발생한다.
        assertThrows(NoUniqueBeanDefinitionException.class, 
                () -> ac.getBean(DiscountPolicy.class));
    }

    @Test
    @DisplayName("동일한 타입의 빈을 모두 조회한다.")
    void findAllBeanByType() {
        Map<String, DiscountPolicy> map = ac.getBeansOfType(DiscountPolicy.class);
        for (String key : map.keySet()) {
            System.out.println("key = " + key + ", value = " + map.get(key));
        }
        System.out.println("beansOfType = " + map);
        assertThat(map.size()).isEqualTo(2);
    }

    @Configuration
    static class SameBeanConfigClass {
        @Bean
        public DiscountPolicy fixDiscountPolicy() {
            return new FixDiscountPolicy();
        }

        @Bean
        public DiscountPolicy rateDiscountPolicy() {
            return new RateDiscountPolicy();
        }
    }
}
```

#### 스프링 빈 조회 - 상속 관계

* 인터페이스의 구현도 넓은 의미에선 상속으로 볼 수 있다.
* 따라서 할인 정책 인터페이스를 조회하게 되면 할인 정책 인터페이스를 구현한 구현체 클래스 빈을 조회할 수 있게 된다.

```java
public class ApplicationContextExtendTest {

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(TestConfig.class);

    @Test
    @DisplayName("부모 타입으로 모두 조회한다.")
    void findAllBeanParentType() {
        Map<String, DiscountPolicy> map = ac.getBeansOfType(DiscountPolicy.class);
        assertThat(map.size()).isEqualTo(2);
        for (String key : map.keySet()) {
            System.out.println("key = " + key + " value = " + map.get(key));
        }
    }

    @Configuration
    static class TestConfig {

        @Bean
        public DiscountPolicy rateDiscountPolicy() {
            return new RateDiscountPolicy();
        }

        @Bean
        public DiscountPolicy fixDiscountPolicy() {
            return new FixDiscountPolicy();
        }
    }
}
```

#### BeanFactory와 ApplicationContext

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8ea54d85-4fb7-4fd9-b998-01d6c52fdcd4.png)

* `BeanFactory` : 스프링 컨테이너의 최상위 인터페이스로 스프링 빈을 관리하고 조회하는 역할을 담당한다.
* `ApplicationContext` : `BeanFactory` 기능을 모두 상속받아서 제공하며, `BeanFactory` 기능뿐만 아니라 수많은 부가 기능을 `ApplicationContext`에서 제공한다.

#### 스프링 빈 설정 메타 정보 - BeanDefinition

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/79ea7715-e63e-4ee4-abe8-71b989288883.png)

* 스프링 컨테이너 자체는 `BeanDefinition`만 알고 있어도 다양한 형식의 설정 정보를 지원한다.
