# Database - Aggregation & Grouping

## 📚 집계 함수

* **AVG** : 표현식의 평균값
* **COUNT(expression)** : 표현식의 결과가 NULL이 아닌 행의 수를 반환
* **COUNT(*)** : 테이블 전체 행 수
* **MAX(expression)** : 표현식의 최댓값
* **MIN(expression)** : 표현식의 최솟값
* **SUM** : 지정한 숫자 컬럼의 모든 값을 더해 합계를 계산
> [참고 - MySQL Aggregate Functions](https://dev.mysql.com/doc/refman/8.4/en/aggregate-functions.html)

## 📚 GROUP BY

* 특정 컬럼의 값이 같은 행들을 하나의 그룹으로 묶어주는 함수
* GROUP BY 절에 그룹화 기준이 될 컬럼을 지정하면 전체에 대한 통계가 아닌 각 그룹에 대한 통계를 낼 수 있다.

```sql
SELECT 
    CATEGORY,
    COUNT(*) AS `카테고리별 주문 건수`
FROM
    ORDER_STAT
GROUP BY
    CATEGORY;
```
* GROUP BY는 NULL값 또한 하나의 독립된 그룹으로 취급하여 집계한다. 
* 이런 NULL 그룹을 통해 데이터가 누락되었음을 발견할 수 있고 데이터 정제의 필요성을 인지할 수 있는 좋은 기회가 된다.

> ❗GROUP BY 주의사항 정리
> * GROUP BY를 사용할 때, SELECT절에는 GROUP BY에 사용된 컬럼과 집계 함수만 사용할 수 있다.

## 📚 HAVING

* HAVING절은 GROUP BY절 바로 뒤에 위치하며, 그룹화된 결과에 대한 조건을 지정하는 역할을 한다.
* WHERE절은 그룹화 이전에 개개인을 걸러내는 조건이고 HAVING은 그룹화 이후에 그룹 자체를 걸러내는 조건이다.

```sql
SELECT
    CATEGORY,
    COUNT(*) AS PREMIUM_ORDER_COUNT
FROM
    ORDER_STAT
WHERE
    PRICE >= 10000
GROUP BY
    CATEGORY
HAVING
    COUNT(*) >= 2;
```

## 📚 SQL 실행 순서

### SQL 쿼리의 논리적인 실행 순서

1. FROM : 가장 먼저 실행된다. 어떤 테이블에서 데이터를 가져올지 결정한다.
2. WHERE : FROM에서 가져온 테이블의 개별 행을 필터링한다. GROUP BY로 묶이기 전 날 것 그대로의 데이터를 1차로 거른다.
3. GROUP BY : WHERE절의 필터링을 통과한 행들을 기준으로 그룹을 형성한다.
4. HAVING : GROUP BY 절을 통해 만들어진 그룹들을 필터링한다. 집계 함수를 이용한 필터링이 여기서 이루어진다.
5. SELECT : HAVING절까지 통과한 최종 그룹들에 대해 우리가 최종적으로 보고자 하는 컬럼을 선택하고 집계 함수 계산, 별칭 부여 등이 이 단계에서 이루어진다.
6. ORDER BY : SELECT절에서 최종 결과 후보들을 지정된 순서대로 정렬한다.
7. LIMIT : 정렬된 결과 중에서 최종적으로 사용자에게 반환될 행의 개수를 제한한다.
