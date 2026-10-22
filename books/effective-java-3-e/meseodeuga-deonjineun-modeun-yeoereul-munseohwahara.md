# (Effective Java Item 74) Java ‐ 메서드가 던지는 모든 예외를 문서화하라

* 메서드가 던지는 예외는 그 메서드를 올바르게 사용하는 데 아주 중요한 정보다.
* 따라서, 각 메서드가 던지는 예외 하나하나를 문서화하는 데 충분한 시간을 쏟아야 한다. 

#### 검사 예외(Checked Exception) 문서화

* 검사 예외는 항상 따로따로 선언하고, 각 예외가 발생하는 상황을 자바독의 @throws 태그를 사용해 정확히 문서화해야한다.
* 공통 상위 클래스 하나로 뭉뚱그려 선언하는 일은 자제해야 한다.
* 극단적인 예로 메서드가 `Exception`이나 `Throwable`을 던진다고 선언해선 안 된다. 

#### 나쁜 예제 (공통 상위 클래스로 뭉뚱그린 경우)

```java
/**
 * 파일을 읽어서 처리합니다.
 * @throws Exception 뭔가 문제가 생기면 발생
 */
public void processFile(String path) throws Exception {
    // 구현
}
```

#### 좋은 예제 (각 예외를 구체적으로 문서화)

```java
/**
 * 파일을 읽어서 처리합니다.
 * 
 * @param path 처리할 파일의 경로
 * @throws FileNotFoundException 파일을 찾을 수 없을 때
 * @throws IOException 파일 읽기 중 I/O 오류가 발생할 때
 * @throws ParseException 파일 형식이 올바르지 않을 때
 */
public void processFile(String path) 
        throws FileNotFoundException, IOException, ParseException {
    File file = new File(path);
    if (!file.exists()) {
        throw new FileNotFoundException("파일을 찾을 수 없습니다: " + path);
    }
    
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        String line = reader.readLine();
        parseContent(line);
    }
}
```

#### 비검사 예외(Unchecked Exception) 문서화

* 자바 언어가 요구하는 것은 아니지만 비검사 예외도 검사 예외처럼 문서화해두면 좋다. 
* 메서드가 던질 수 있는 예외를 각각 `@throws` 태그로 문서화하되, 비검사 예외는 메서드 선언의 `throws` 목록에 넣지 말아야 한다. 

#### 좋은 예제 (비검사 예외 문서화)

```java
/**
 * 두 수를 나눕니다.
 * 
 * @param dividend 피제수
 * @param divisor 제수
 * @return 나눗셈 결과
 * @throws ArithmeticException divisor가 0일 때
 * @throws IllegalArgumentException dividend가 음수일 때
 */
public double divide(int dividend, int divisor) {
    if (dividend < 0) {
        throw new IllegalArgumentException("피제수는 음수일 수 없습니다: " + dividend);
    }
    if (divisor == 0) {
        throw new ArithmeticException("0으로 나눌 수 없습니다");
    }
    return (double) dividend / divisor;
}
```

#### 현실적 제약

* 비검사 예외도 문서화해두는 것이 좋다고 했지만 현실적으로 불가능할 때도 있다.
* 클래스를 수정하면서 새로운 비검사 예외를 던지게 되어도 소스 호환성과 바이너리 호환성이 그대로 유지된다는게 가장 큰 이유다. 

#### 예제: 외부 라이브러리를 사용하는 경우

```java
/**
 * JSON 문자열을 파싱하여 User 객체를 생성합니다. 
 * 
 * @param json JSON 형식의 문자열
 * @return 파싱된 User 객체
 * @throws JsonParseException JSON 파싱 실패 시
 * 
 * 참고: 이 메서드는 내부적으로 외부 JSON 라이브러리를 사용하며,
 * 라이브러리 버전에 따라 추가적인 RuntimeException이 발생할 수 있습니다.
 */
public User parseUser(String json) {
    // Jackson, Gson 등의 라이브러리 사용
    return objectMapper.readValue(json, User.class);
}
```

#### 클래스 수준 예외 문서화

* 한 클래스에 정의된 많은 메서드가 같은 이유로 같은 예외를 던진다면 그 예외를 각각의 메서드가 아닌 클래스 설명에 추가하는 방법도 있다.

#### 예제: 클래스 수준 문서화

```java
/**
 * 사용자 정보를 관리하는 리포지토리입니다. 
 * 
 * <p>이 클래스의 모든 메서드는 다음 예외를 던질 수 있습니다:
 * <ul>
 *   <li>{@link NullPointerException} - 필수 매개변수가 null일 때
 *   <li>{@link DatabaseException} - 데이터베이스 연결 실패 시
 * </ul>
 */
public class UserRepository {
    
    /**
     * ID로 사용자를 조회합니다.
     * 
     * @param id 사용자 ID
     * @return 사용자 정보
     * @throws UserNotFoundException 해당 ID의 사용자가 없을 때
     */
    public User findById(Long id) {
        validateNotNull(id, "id");
        // 구현
    }
    
    /**
     * 사용자를 저장합니다.
     * 
     * @param user 저장할 사용자 정보
     * @throws DuplicateUserException 이미 존재하는 사용자일 때
     */
    public void save(User user) {
        validateNotNull(user, "user");
        // 구현
    }
    
    /**
     * 사용자를 삭제합니다.
     * 
     * @param id 삭제할 사용자 ID
     * @throws UserNotFoundException 해당 ID의 사용자가 없을 때
     */
    public void delete(Long id) {
        validateNotNull(id, "id");
        // 구현
    }
    
    private void validateNotNull(Object obj, String name) {
        if (obj == null) {
            throw new NullPointerException(name + "은(는) null일 수 없습니다");
        }
    }
}
```

#### 핵심 정리

1. **검사 예외는 개별적으로 선언**하고 각각 `@throws`로 문서화
2.  **비검사 예외도 문서화**하되, `throws` 절에는 포함하지 않음
3. **공통 상위 예외로 뭉뚱그리지 말 것** (Exception, Throwable 금지)
4. **클래스 전체에 공통된 예외**는 클래스 수준에서 문서화 가능
5. 예외 문서화는 **API 사용자를 위한 중요한 계약**임을 명심
