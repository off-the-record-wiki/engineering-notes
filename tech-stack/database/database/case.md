# Database - CASE

## 📚 CASE문

* 단순 CASE문(Simple CASE Expression) & 검색 CASE문(Searched CASE Expression)으로 구분할 수 있다.

#### 단순 CASE문(Simple CASE Expression)
* 하나의 컬럼이나 표현식 결과 값에 따라 결과를 다르게 하고 싶을 때 사용한다.
```sql
CASE 비교대상_컬럼_또는_표현식
    WHEN 값1 THEN 결과1
    WHEN 값2 THEN 결과2
    ...
    ELSE 그_외의_경우_결과
END
```
* 단순 CASE문도 위에서 아래로 순서대로 조건을 평가하며, 가장 먼저 일치하는 WHEN절을 만나는 순간 그 THEN의 결과를 반환하고 즉시 평가를 종료하게 된다.

#### 검색 CASE문(Searched CASE Expression)
* 단순 CASE문처럼 하나의 특정 값을 비교하는 대신, 각 WHEN절에 독립적인 조건식을 사용해 복잡한 논리를 구현할 때 사용한다.
```sql
CASE
    WHEN 조건1 THEN 결과1
    WHEN 조건2 THEN 결과2
    ...
    ELSE 그_외의_경우_결과
END
```
* 검색 CASE문도 위에서 아래로 순서대로 조건을 평가하며, 가장 먼저 참이 되는 WHEN절을 만나는 순간 그 THEN의 결과를 반환하고 즉시 평가를 종료한다. 순서가 매우 중요하게 작동한다.

> ❗검색 CASE문 사용 시 주의사항: WHEN절의 순서
> * 더 포괄적인(=범위가 넓은) 조건보다는 더 구체적인(=범위가 좁은) 조건을 먼저 배치하는 것이 좋다.

## 📚 CASE문 - 그룹핑

* CASE문 분류(Classification) : CASE문을 사용해 하나의 라벨을 붙여준다.
* CASE문 집계(Aggregation) : 만들어진 라벨을 기준으로 GROUP BY하고 COUNT() 함수를 사용해 라벨의 속성값에 대한 값을 구한다.
```sql
SELECT
    CASE
        WHEN YEAR(birth_date) >= 1990 THEN '1990년대생'
        WHEN YEAR(birth_date) >= 1980 THEN '1980년대생'
        ELSE '그 이전 출생'
    END AS birth_decade,
    COUNT(*) AS customer_count
FROM
    users
GROUP BY
    CASE
        WHEN YEAR(birth_date) >= 1990 THEN '1990년대생'
        WHEN YEAR(birth_date) >= 1980 THEN '1980년대생'
        ELSE '그 이전 출생'
    END;
```

## 📚 CASE문 - 조건부 집계

#### 서브 쿼리를 사용하는 방법 - 각 상태별 카운트를 서브 쿼리로 미리 구해 메인 쿼리에서 이 값들을 조인하거나 활용하는 방법
> 결론부터 말하자면 해당 방식보다 CASE문을 활용한 다른 조건부 집계 방식이 권장된다.(높은 쿼리 성능)

```sql
SELECT
    (SELECT COUNT(*) FROM orders) AS total_orders,
    (SELECT COUNT(*) FROM orders WHERE status = 'COMPLETED') AS completed_count,
    (SELECT COUNT(*) FROM orders WHERE status = 'SHIPPED') AS shipped_count,
    (SELECT COUNT(*) FROM orders WHERE status = 'PENDING') AS pending_count;
```

#### COUNT(CASE ...) - COUNT 함수는 NULL이 아닌 모든 값을 센다는 특징을 사용하는 방법

```sql
COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END)
```

#### SUM(CASE ...) - SUM 함수는 숫자들의 합계를 구한다.

```sql
SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END)
```
