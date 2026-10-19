# Spring - Validation

#### FieldError, ObjectError

* `FieldError` 클래스는 아래와 같이 두 가지 생성자를 제공한다.
* `FieldError` 클래스는 특정 필드에서의 예외 처리에 적합하다.

```java
public FieldError(String objectName, String field, String defaultMessage) {
    this(objectName, field, (Object)null, false, (String[])null, (Object[])null, defaultMessage);
}

public FieldError(String objectName, String field, @Nullable Object rejectedValue, 
                  boolean bindingFailure, @Nullable String[] codes, @Nullable Object[] arguments, 
                  @Nullable String defaultMessage) {
    
    super(objectName, codes, arguments, defaultMessage);
    Assert.notNull(field, "Field must not be null");
    this.field = field;
    this.rejectedValue = rejectedValue;
    this.bindingFailure = bindingFailure;
}
```

* `FieldError` 각 파라미터 설명
  * `objectName` : 오류가 발생한 객체 이름
  * `field` : 오류 필드
  * `rejectedValue` : 사용자가 입력한 값(거절된 값)
  * `bindingFailure` : 타입 오류 같은 바인딩 실패인지 아니면 검증 실패인지를 구분하는 값
  * `codes` : 메시지 코드
  * `arguments` : 메시지에서 사용하는 인자
  * `defaultMessage` : 기본 오류 메시지
* `ObjectError` 클래스는 두 가지 생성자를 제공한다.
* `ObjectError` 클래스는 특정 필드 예외가 아닌 전체 예외 처리에 적합하다.

```java
public ObjectError(String objectName, @Nullable String defaultMessage) {
    this(objectName, (String[])null, (Object[])null, defaultMessage);
}

public ObjectError(String objectName, @Nullable String[] codes, @Nullable Object[] arguments, 
                   @Nullable String defaultMessage) {
    
    super(codes, arguments, defaultMessage);
    Assert.notNull(objectName, "Object name must not be null");
    this.objectName = objectName;
}
```

#### 오류 코드와 메시지 처리

```properties
# application.properties
spring.messages.basename=messages,config.i18n.messages,errors
```
```properties
# errors.properties
required.item.itemName=상품 이름은 필수입니다.
range.item.price=가격은 {0} ~ {1} 까지 허용합니다.
max.item.quantity=수량은 최대 {0} 까지 허용합니다.
totalPriceMin=가격 * 수량의 합은 {0}원 이상이어야 합니다. 현재 값 = {1}
```

* 스프링에서 제공해주는 메시지를 이용해서도 오류 코드와 메시지를 처리할 수 있다.

```
객체 오류의 경우
1순위 : code.objectname  
2순위 : code

예시
앞서 위쪽에서 item 객체에 totalPriceMin이라는 코드로 작성했다.
totalPriceMin.item=상품의 가격 * 수량의 합은 {0}원 이상이어야 합니다. 현재 값 = {1}
totalPriceMin=전체 가격은 {0}원 이상이어야 합니다. 현재 값 = {1}

필드 오류의 경우
1순위 : code.objectname.field
2순위 : code.field
3순위 : code.field type
4순위 : code

예시
앞서 위쪽에서 item 객체의 quantity 필드에 max라는 코드를 작성했다.
max.item.quantity = 아이템 수량은 최대 {0} 까지 허용합니다.
max.quantity = 수량은 최대 {0}까지 허용합니다.
max.java.lang.Integer = {0} 까지의 숫자를 허용합니다.
max = {0} 까지 허용합니다.

타입 미스매치로 스프링이 자동으로 bindingResult에 넣어주는 규칙
예) 오류 코드: typeMismatch, object name "user", field "age", field type: int 인 경우
1순위 : typeMismatch.user.age
2순위 : typeMismatch.age
3순위 : typeMismatch.int
4순위 : typeMismatch
```

* 메시지를 단순하게 작성하면 범용성이 좋아 여러 곳에서 활용할 수 있겠지만 섬세한 메시지를 작성하긴 어렵다.
* 우선순위는 섬세하게 작성할수록 높다.

#### BindingResult & Validator 분리

* 검증 예외 처리를 위한 많은 방법이 존재한다.
* `BindingResult`, `FieldError`, `ObjectError` ... 등등 여러 방법이 있었는데 현재 컨트롤러 코드 측에 한 번에 전부 작성된 것을 볼 수 있다.

```java
@PostMapping("/add")
public String addItemV3(@ModelAttribute Item item, BindingResult bindingResult){
  ...
}
```

* 컨트롤러에서 `@ModelAttribute` 어노테이션이 붙은 인자의 바로 뒤에 BindingResult 인자를 주어야 `@ModelAttribute` 어노테이션이 붙은 값에 대해서 실패한 에러들이 bindingResult에 담기게 된다.
* 허나 이 실패한 오류들을 컨트롤러에 한 번에 작성하게 되면 컨트롤러 본연의 책임(HTTP 요청과 응답을 처리함)을 질 수 없게 된다.
* 권장하는 방법은 검증을 책임지는 클래스를 별도로 만들어 분리를 하고 이를 컨트롤러 측에서 활용하는 것이다.

```java
// 검증을 담당하는 클래스 분리
@Component
public class ItemValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return Item.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Item item = (Item) target;

        // ... 검증 로직 생략

        // 특정 필드 검증 (FieldError 생성)
        errors.rejectValue("quantity", "max", new Object[]{9999}, null);        

        // 특정 필드가 아닌 복합 룰 검증 (ObjectError 생성)
        errors.reject("totalPriceMin", new Object[]{10000, resultPrice}, null);
    }
}
```
```java
@Controller
@RequestMapping("/validation")
@RequiredArgsConstructor
public class ValidationItemController {

    private final ItemValidator itemValidator;

    /**
     * WebDataBinder에 검증기를 추가한다.
     * 해당 컨트롤러 내의 모든 요청에 대해 이 검증기가 적용된다.
     */
    @InitBinder
    public void init(WebDataBinder dataBinder) {
        dataBinder.addValidators(itemValidator);
    }

    @PostMapping("/add")
    public String addItemV6(@Validated @ModelAttribute Item item, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // 에러 발생 시 처리 로직
            return "validation/addForm";
        }
        
        // ... 성공 로직 생략
        return "redirect:/validation/items";
    }
}
```

[Spring - @InitBinder](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-initbinder.html)
