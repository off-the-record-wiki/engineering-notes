# Database - UNION

## 📚 UNION

#### UNION 연산자 - 2개의 SELECT문의 결과를 하나로 합쳐준다.
* UNION으로 연결되는 모든 SELECT문은 컬럼 개수가 반드시 동일해야 한다.
* 각 SELECT문의 같은 위치에 있는 컬럼들은 서로 호환 가능한 데이터 타입이어야 한다.(숫자는 숫자, 문자는 문자)
```sql
SELECT
    name,
    email
FROM
    users
UNION
SELECT
    name,
    email
FROM
    retired_users;
```

## 📚 UNION ALL

* UNION : 두 결과 집합을 합친 후 중복된 행을 제거한다.
* UNION ALL : 두 결과 집합을 합친 후 발생하는 중복 제거 과정 없이 그대로 처리한다.

```sql
SELECT
    name,
    email
FROM
    users
UNION ALL
SELECT
    name,
    email
FROM
    retired_users;
```

## 📚 UNION 정렬

* UNION 또는 UNION ALL을 사용해 SELECT문의 결과를 합칠 때, ORDER BY절의 위치가 상당히 중요하다.

```sql
SELECT
    name,
    email,
    created_at AS event_date
FROM
    users
UNION ALL
SELECT
    name,
    email,
    retired_date AS event_date
FROM retired_users
ORDER BY
    event_date DESC;  # 전체 쿼리의 맨 끝에 위치!
```
