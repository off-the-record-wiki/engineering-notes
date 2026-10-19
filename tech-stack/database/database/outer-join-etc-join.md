# Database - Outer Join & Etc Join

## 📚 외부 조인(OUTER JOIN)

### LEFT OUTER JOIN

* 왼쪽 테이블을 기준으로 삼는다.
* LEFT JOIN 왼쪽 절에 있는 테이블이 기준이 된다.
* 왼쪽 테이블의 결과는 모두 포함시키고 ON 조건에 맞는 데이터를 오른쪽 테이블에서 찾아 옆에 붙인다.
* 만약 오른쪽 테이블에 짝이 맞는 데이터가 없다면 그 자리는 NULL 값으로 채워진다.
* LEFT JOIN = LEFT OUTER JOIN

### RIGHT OUTER JOIN(잘 사용되지 않음)

* 오른쪽 테이블을 기준으로 삼는다.
* RIGHT JOIN 오른쪽 절에 있는 테이블이 기준이 된다.
* 오른쪽 테이블의 결과는 모두 포함시키고 ON 조건에 맞는 데이터를 왼쪽 테이블에서 찾아 옆에 붙인다.
* 만약 왼쪽 테이블에 짝이 맞는 데이터가 없다면 그 자리는 NULL 값으로 채워진다.

## 📚 조인 연산의 특징

* 자식 -> 부모 조인(FK -> PK 참조) : 행 개수가 늘어나지 않는다.(=다대일 조인)
* 부모 -> 자식 조인(PK -> FK 참조) : 행 개수가 N개만큼 늘어난다.(=일대다 조인)

## 📚 셀프 조인(SELF JOIN)

* 자기 자신을 조인하는 경우를 셀프 조인이라고 한다.
* SQL이 이 기법을 가능케하는 핵심 원리는 바로 테이블 별칭에 있다. 하나의 테이블에 서로 다른 별칭을 부여함으로써, 데이터베이스가 이들을 마치 다른 두 개의 테이블인 것처럼 인식하게 만드는 것이다.

1) INNER JOIN을 사용한 SELF JOIN 방법

```sql
SELECT
    e.name AS employee_name
    m.name AS manager_name
FROM
    employees e
JOIN
    employees m ON e.manager_id = m.employee_id;
```

2) LEFT JOIN을 사용한 SELF JOIN 방법

```sql
SELECT
    e.name AS employee_name
    m.name AS manager_name
FROM
    employees e
LEFT JOIN
    employees m ON e.manager_id = m.employee_id;    
```

## 📚 CROSS JOIN

* CROSS JOIN은 조인 조건 없이 한쪽 테이블의 모든 행을 다른 쪽 테이블의 모든 행과 하나씩 전부 연결하는 조인 방식이다.
* 해당 조인은 연결고리가 없기 때문에 ON 절을 사용하지 않는다.
* CROSS JOIN의 결과를 수학 용어로 카테시안 곱이라고 부른다. 결과는 `M * N`개의 행을 가진다.
