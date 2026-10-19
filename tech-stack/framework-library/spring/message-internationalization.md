# Spring - Message & Internationalization

#### 메시지

* 다양한 메시지를 한 곳에서 관리할 수 있도록 하는 기능을 메시지 기능이라고 한다.
* 이렇게 하면 HTML 파일에 메시지가 하드코딩된 부분을 일일이 수정하지 않아도 된다.
* 메시지 관리 기능을 사용하려면 스프링이 제공하는 `MessageSource`를 스프링 빈으로 등록하면 되는데, `MessageSource`는 스프링 인터페이스이다. 따라서 구현체인 `ResourceBundleMessageSource`를 스프링 빈으로 등록하면 된다.

```java
@Configuration
public class MessageSourceConfig {

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource resourceBundleMessageSource = new ResourceBundleMessageSource();
        resourceBundleMessageSource.setBasenames("messages", "errors"); // 설정 파일 이름 지정
        resourceBundleMessageSource.setDefaultEncoding("utf-8"); // 인코딩 정보
        return resourceBundleMessageSource;
    }
}
```

```properties
# 한국어 메시지(messages.properties)
hello=안녕
hello.name=안녕 {0}

# 영어 메시지(messages_en.properties)
hello=hello
hello.name=hello {0}

# application.properties
spring.messages.basename=messages,config.i18n.messages
```

#### 국제화

* Spring의 국제화는 단순히 텍스트 번역을 넘어, 사용자의 지역(Locale) 정보를 바타응로 메시지, 숫자, 날짜 등을 동적으로 처리하는 핵심 기능이다.
* `MessageSource` : 실제 메시지 파일에서 설정된 키 값을 읽어오는 인터페이스
* `LocaleResolver` : 현재 HTTP 요청이 어떤 언어/국가 정보를 담고 있는지 판단하는 전략 인터페이스
* `LocaleChangeInterceptor` : URL 파라미터 등을 통해 사용자가 직접 언어를 변경할 수 있도록 가로채는 인터페이스
