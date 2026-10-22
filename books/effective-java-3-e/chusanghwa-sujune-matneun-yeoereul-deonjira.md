# (Effective Java Item 73) Java ‐ 추상화 수준에 맞는 예외를 던지라

* 상위 계층에서 저수준 예외를 잡아 자신의 추상화 수준에 맞는 예외로 바꿔 던져야 한다.
* 이를 예외 번역(Exception Translation)이라 한다.

```java
try {
    // ... 저수준 추상화
} catch (LowerLevelException e) {
    // 추상화 수준에 맞게 번역한다.
    throw new HigherLevelException(...);
}
```

```java
// 예시 코드
// ✅ 올바른 방식 - 데이터 접근 레이어에서 번역
@Repository
public class UserRepository {
    
    private JdbcTemplate jdbcTemplate;
    
    public User save(User user) {
        try {
            // ...  JDBC 코드
        } catch (SQLException e) {
            // 여기서 번역!  Repository 레이어에서 처리
            if (e.getErrorCode() == 1062) {
                throw new DuplicateKeyException("중복된 키", e);
            }
            throw new DataAccessException("저장 실패", e);
        }
    }
}

@Service
public class UserService {
    
    private UserRepository userRepository;
    
    public User createUser(User user) {
        try {
            return userRepository. save(user);
        } catch (DuplicateKeyException e) {  // ✅ 추상화된 예외 처리
            throw new DuplicateUserException("이미 존재하는 사용자", e);
        }
    }
}
```

* 예외를 번역할 때, 저수준 예외가 디버깅에 도움이 된다면 예외 연쇄(exception-chaining)를 사용하는게 좋다.
* 예외 연쇄란, 문제 근본 원인(cause)인 저수준 예외를 고수준 예외에 실어 보내는 방식이다.
* 그러면 별도의 접근자 메서드(`Throwable`의 `getCause()`)를 통해 필요하다면 언제든 저수준 예외를 꺼내 볼 수 있다.

```java
try {
    // ... 저수준 추상화를 이용한다.
} catch (LowerLevelException cause) {
    // 저수준 예외를 고수준 예외에 실어보낸다.
    throw new HigherLevelException(cause);
}
```

* 대부분의 표준 예외는 예외 연쇄용 생성자를 갖추고 있다.
* 그렇지 않은 예외라도 `Throwable`의 `initCause()`를 이용해 "원인"을 직접 못 박을 수 있다.
* 무턱대고 예외를 전파하는 것보다 예외 번역이 우수한 방법이지만 그렇다고 해서 남용해선 곤란하다.
* 차선책으로 아래 계층에서 예외를 피할 수 없다면 상위 계층에서 그 예외를 처리해 문제를 API 호출자에게까지 전파하지 않는 방법이 있는데 바로 적절한 로깅 기능이다.
* 이렇게 해두면 클라이언트 코드와 사용자에게 문제를 전파하지 않으면서도 개발자가 로그를 분석해 추가 조치를 취할 수 있게 해준다.

#### Controller-Service-Repository (Layered Architecture)

```
Controller (상위)
    ↓
Service (중간)
    ↓
Repository (하위)
```

* 레이어드 아키텍처 (계층형 아키텍처)
* 비즈니스 로직을 계층별로 분리
* 백엔드 구조를 세분화한 패턴
