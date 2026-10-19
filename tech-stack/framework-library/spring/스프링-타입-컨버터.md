# Spring - Spring Type Converter

#### 스프링 타입 컨버터

```java
@FunctionalInterface
public interface Converter<S, T extends @Nullable Object> {

    /**
     * 소스 타입 S의 객체를 타겟 타입 T로 변환합니다.
     * @param source 변환할 소스 객체 (S의 인스턴스여야 하며, null이면 안 됨)
     * @return 변환된 객체 (T의 인스턴스이며, null일 수 있음)
     * @throws IllegalArgumentException 소스를 원하는 타겟 타입으로 변환할 수 없는 경우 발생
     */
    T convert(S source);

    /**
     * 현재 Converter를 적용한 후, 그 결과에 'after' Converter를 연쇄적으로 적용하는 
     * 합성(Composed) Converter를 생성합니다.
     * @since 5.3
     */
    default <U> Converter<S, @Nullable U> andThen(Converter<? super T, ? extends @Nullable U> after) {
        Assert.notNull(after, "'after' Converter must not be null");
        return (S s) -> {
            T initialResult = convert(s);
            // 첫 번째 변환 결과가 null이 아니면 두 번째 변환을 수행하고, null이면 null을 반환
            return (initialResult != null ? after.convert(initialResult) : null);
        };
    }
}
```

#### Converter 예시 코드 작성

```java
@Slf4j
public class IntegerToStringConverter implements Converter<Integer, String> {

    @Override
    public String convert(Integer source) {
        log.info("converter source={}", source);
        
        // Integer 타입을 String 타입으로 변환하여 반환
        return String.valueOf(source);
    }
}
```
```java
@Slf4j
public class StringToIntegerConverter implements Converter<String, Integer> {

    @Override
    public Integer convert(String source) {
        log.info("converter source={}", source);

        // String 타입을 Integer 타입으로 변환하여 반환
        // 문자가 숫자가 아닌 경우 NumberFormatException이 발생할 수 있음
        return Integer.valueOf(source);
    }
}
```

* A → B, B → A 타입으로 변환하는 컨버터들을 직접 개발하면 매우 불편하다.
* 이를 위해 스프링은 ConversionService를 제공한다. ConversionService는 컨버터를 모아두고 묶어서 편리하게 사용할 수 있는 기능을 제공한다.

#### ConversionService

```java
public interface ConversionService {

    /**
     * sourceType에서 targetType으로 변환이 가능한지 여부를 확인합니다.
     */
    boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType);

    /**
     * TypeDescriptor를 사용하여 변환 가능 여부를 확인합니다.
     * 필드나 속성의 애노테이션 정보 등 더 구체적인 컨텍스트를 포함할 수 있습니다.
     */
    boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

    /**
     * 주어진 소스 객체를 지정한 targetType으로 변환합니다.
     * @return 변환된 객체 (targetType의 인스턴스)
     */
    <T> @Nullable T convert(@Nullable Object source, Class<T> targetType);

    /**
     * 소스 객체를 타겟의 TypeDescriptor 정보에 맞춰 변환합니다. (Spring 6.1 이상)
     */
    default @Nullable Object convert(@Nullable Object source, TypeDescriptor targetType) {
        return convert(source, TypeDescriptor.forObject(source), targetType);
    }

    /**
     * 소스와 타겟의 상세 정보(TypeDescriptor)를 모두 사용하여 정밀하게 변환합니다.
     * 컬렉션이나 맵의 요소 타입까지 고려해야 할 때 주로 사용됩니다.
     */
    @Nullable Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType, 
                             TypeDescriptor targetType);
}
```

```java
// 등록 -> 추가적으로 여러 컨버터들 등록 가능
DefaultConversionService conversionService = new DefaultConversionService();
conversionService.addConverter(new IntegerToStringConverter());
conversionService.addConverter(new IpPortToStringConverter());

//사용
IpPort ipPort = conversionService.convert("127.0.0.1:8080", IpPort.class);
assertThat(ipPort).isEqualTo(new IpPort("127.0.0.1", 8080));

String ipPortString = conversionService.convert(new IpPort("127.0.0.1", 8080), String.class);
assertThat(ipPortString).isEqualTo("127.0.0.1:8080");
```

* 위와 같이 등록해도 실제로 스프링에 등록되지 않는다.
* 스프링에 등록하기 위해서는 `WebMvcConfigurer`가 제공하는 `addFormatter()`를 사용해서 컨버터를 스프링에 등록해주면 된다.

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 주석: 개발자가 직접 만든 컨버터를 등록합니다.
        // 스프링은 내부적으로 ConversionService를 사용하며, 
        // 등록된 순서와 타입을 고려하여 적절한 변환기를 선택합니다.
        
        registry.addConverter(new StringToIntegerConverter());
        registry.addConverter(new IntegerToStringConverter());
        
        // 추가로 IpPortConverter 같은 사용자 정의 객체 변환기도 여기에 등록할 수 있습니다.
        // registry.addConverter(new StringToIpPortConverter());
    }
}
```

#### Formatter

* Converter는 문자를 객체로, 객체를 문자로 변환하는 등의 범용적인 타입 변환 기능을 제공한다.
* Formatter는 객체를 특정한 포맷에 맞추어 문자로 출력하거나 그 반대 기능을 할 수 있도록 한다.

```java
public interface Formatter<T> extends Printer<T>, Parser<T> {

}
```

* Printer 인터페이스의 `print()` 메서드 : 객체를 문자로 변환
* Parser 인터페이스의 `parser()` 메서드 : 문자를 객체로 변환

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 1. 컨버터 등록: 소스 객체(S)를 타겟 객체(T)로 변환하는 범용적인 기능
        registry.addConverter(new StringToIntegerConverter());
        registry.addConverter(new IntegerToStringConverter());
        
        // 2. 포맷터 등록: 객체를 문자로, 문자를 객체로 변환하는 '문자 특화' 기능 (Locale 고려 가능)
        // 예: 날짜 형식을 "yyyy-MM-dd"로 출력하거나 숫자에 쉼표(,)를 찍는 등의 처리
        registry.addFormatter(new MyNumberFormatter()); 
        // 참고: 제공해주신 예시의 DateFormatter는 스프링 제공 구현체 또는 커스텀 구현체일 수 있습니다.
    }
}
```

* Formatter 역시 위와 같이 `WebMvcConfigurer` 인터페이스에서 `addFormatter()` 메서드로 추가하면 된다.
* 스프링에서는 이미 수많은 포맷터를 기본으로 제공해준다. 따라서 직접 만들어 사용할 일이 거의 없다.
