# Spring ‐ Spring Exception Abstraction

#### 스프링 예외 추상화

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/42e41c61-3f99-49d8-95c2-80684c6108ba.png)

* 예외의 최고 상위는 `DataAccessException`이다.
* `RuntimeException`을 상속받기 때문에 데이터 접근 계층의 예외는 모두 런타임 예외이다.
* 크게 `NonTransientDataAccessException`과 `TransientDataAccessException`으로 나뉜다.
  * `Transient`
    * 일시적이라는 뜻으로 하위 예외는 동일한 SQL을 다시 시도했을 때 성공할 가능성이 존재한다.
    * 데이터베이스 상태가 좋아지거나 락이 풀렸을 때 재시도하면 성공할 수 있다.
  * `NonTransient`
    * 일시적이지 않다는 뜻으로 SQL을 그대로 다시 시도했을 경우 실패한다.
    * SQL 문법 오류, 데이터베이스 제약 조건 위배 등이 이에 해당한다.
