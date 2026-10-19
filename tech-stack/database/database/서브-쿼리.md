# Database - Sub Query

## 📚 서브 쿼리

* 서브 쿼리는 말 그대로 하나의 SQL 쿼리 문 안에 포함된 또 다른 SELECT 쿼리를 말한다.
* 괄호 ( ) 안에 있는 서브 쿼리가 먼저 실행되고 데이터베이스는 서브 쿼리 실행 결과를 바깥쪽 메인 쿼리에게 전달해 메인 쿼리가 그 결과를 활용해 최종 작업을 수행한다.

```sql
SELECT
    name,
    price
FROM
    products
WHERE
    price > (SELECT AVG(price) FROM products);  # 괄호 ( ) 안의 서브 쿼리 먼저 실행
```

## 📚 스칼라 서브 쿼리

* 서브 쿼리의 결과가 하나의 값으로 정해지는 것을 스칼라 서브 쿼리라고 한다.
* 결과가 하나의 값으로 정해지기 때문에 단일 행 비교 연산자들과 함께 사용할 수 있다.

## 📚 다중 행 서브 쿼리

* 서브 쿼리가 2개 이상의 행을 반환할 때 사용되는 것이 바로 다중 행 서브 쿼리이다.
* 다중 행 서브 쿼리 결과를 처리하기 위해서 단일 행 연산자가 아닌 목록을 다룰 수 있는 특별한 연산자가 지원된다.
  * Ex. IN, ANY, ALL
#### IN 연산자 : 목록에 포함된 값과 일치하는지 확인

```sql
SELECT 
    * 
FROM 
    orders
WHERE 
    product_id IN (SELECT product_id FROM products WHERE category = '전자기기')
ORDER BY 
    order_id;
```

#### ANY/ALL 연산자 : 목록의 모든/일부 값과 비교

* ANY/ALL은 주로 >, <과 같은 비교 연산자와 함께 사용되며 서브 쿼리가 반환한 여러 값들과 비교하는 역할을 한다.
* `> ANY` : 서브 쿼리가 반환한 여러 결과값 중 어느 하나보다만 크면 참이다.
* `> ALL` : 서브 쿼리가 반환한 여러 결과값 모두보다 커야만 참이다.
* `< ANY` : 최대값보다 작으면 참이다.
* `< ALL` : 최소값보다 작으면 참이다.
* `= ANY` : IN과 완전히 동일한 의미다.
 
## 📚 다중 컬럼 서브 쿼리

* 서브쿼리의 SELECT 절에 2개 이상의 컬럼이 포함되는 경우를 말한다. 메인 쿼리의 WHERE 절에서 여러 컬럼을 동시에 비교해야 할 때 매우 유용하다.
```sql
SELECT 
    order_id, 
    user_id, 
    status, 
    order_date
FROM 
    orders
WHERE (user_id, status) = (SELECT user_id, status FROM orders WHERE order_id = 3);
```

#### IN 연산자

* 다중 컬럼 서브 쿼리 역시 서브 쿼리 결과가 여러 행일 수 있다. 이 때, IN 연산자를 사용해야 한다.
```sql
SELECT
    o.order_id,
    o.user_id,
    o.order_date
FROM 
    orders o
WHERE (o.user_id, o.order_date) IN (SELECT user_id, MIN(order_date) FROM orders GROUP BY user_id);
```

## 📚 상관 서브 쿼리

* 서브 쿼리가 메인 쿼리에서 현재 처리 중인 행의 특정 값을 알아야만 계산을 수행할 수 있을 때, 상관 서브 쿼리를 사용해야 한다.
* 상관이란 메인쿼리와 서브쿼리가 서로 영향을 준다는 뜻이다.

#### IN 연산자 - 특정 컬럼의 값이 괄호 안에 있는 목록에 포함되는가를 확인하는 역할 수행

```sql
SELECT
    product_id,
    name,
    price
FROM
    products
WHERE
    product_id IN (SELECT DISTINCT product_id FROM orders);
```

#### EXISTS 연산자 - 서브 쿼리의 결과로 행이 하나라도 존재하는가를 확인하는 역할 수행

```sql
SELECT
    product_id,
    name,
    price
FROM
    products p
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.product_id = p.product_id);
```

> 선택의 기준
> * 서브 쿼리의 대상이 되는 테이블이 크다면 EXISTS가 유리하다.
> * IN은 목록 전체를 비교해야 하지만, EXISTS는 조건을 만족하는 데이터의 유무를 따진다.
 
#### NOT EXISTS 연산자 - 존재하지 않음을 확인하는 역할 수행

```sql
SELECT
    product_id,
    name,
    price,
    stock_quantity
FROM
    products p
WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.product_id = p.product_id);
```

* IN, EXISTS, NOT EXISTS 등 여러 연산자를 활용할 수 있는 상관 서브 쿼리는 메인 쿼리의 행 수만큼 반복될 수 있기 때문에 메인 쿼리가 다루는 데이터의 양이 많다면 쿼리 전체 성능이 급격히 저하될 수 있다.
* 많다면 JOIN으로 동일한 결과를 얻도록 할 수 있으니 JOIN 사용을 권장한다.
* EXISTS는 특정 조건에 맞는 데이터가 있는지 확인만 하고 넘어가기 때문에 IN이나 JOIN보다 훨씬 효율적으로 동작하는 상황도 많다.

## 📚 SELECT 서브 쿼리

```sql
SELECT
    name,
    price,
    (SELECT AVG(price) FROM products) AS avg_price # 서브 쿼리가 외부 컬럼을 참조하지 않아 독립적으로 실행될
FROM
    products;
```

* 서브 쿼리의 중요한 특징은 바깥쪽 메인 쿼리의 각 행마다 개별적으로 반복적으로 실행될 수 있다는 점이다.

```sql
SELECT
    p.product_id,
    p.name,
    p.price,
    order_count
    (SELECT COUNT(*) FROM orders o WHERE o.product_id = p.product_id) AS
FROM
    products p;
```

## 📚 서브쿼리와 JOIN 비교

#### 성능 측면에서 비교한다면... -> 일반적으로 데이터베이스는 JOIN이 서브 쿼리보다 성능이 더 좋거나 최소한 동일한 경우가 많다.
* 왜냐하면 데이터베이스의 쿼리 옵티마이저(Query Optimizer) 때문이다.
* JOIN은 옵티마이저에게 많은 정보를 제공한다. 옵티마이저는 인덱스를 어떻게 활용하고 어떤 테이블을 먼저 읽을지 등 가장 효율적인 실행 계획을 선택할 수 있는 더 넓은 선택지를 가진다.
* 반면, 서브 쿼리는 단계적으로 실행되는 경우가 많다. 서브 쿼리를 먼저 실행해서 나온 결과를 메모리에 담아두고 그 다음 메인 쿼리가 그 결과를 참조하는 방식으로 동작하기 때문에 비효율성이 지적된다.

#### 가독성 측면에서 비교한다면... -> 주관적인 영역이다. 쿼리의 유지보수 측면에서 성능만큼이나 중요하다.
* 서브 쿼리는 쿼리의 논리적 단계를 명확히 구분해주어 복잡한 로직을 더 쉽게 이해할 수 있도록 해준다.
* JOIN은 쿼리에 필요한 모든 데이터 소스를 한 눈에 보여주고 여러 테이블의 컬럼을 함께 조회해야 할 때는 구조적으로 더 깔끔하다.

#### 최종 결론
1) JOIN을 우선적으로 고려하라. -> 일반적인 성능 우위와 범용성을 고려할 때, JOIN이 좋다.
2) JOIN으로 표현하기 너무 복잡하거나, 서브 쿼리 가독성이 훨씬 좋다면 서브 쿼리를 사용하라.
* 성능이 아주 중요한 것이 아니라면 서브 쿼리를 추천한다.
3) EXISTS를 활용하라.
* IN 서브 쿼리 대안으로 EXISTS라는 서브 쿼리 연산자가 존재한다.
* 해당 연산자는 행의 존재 유무만을 따지기 때문에 특정 상황에서 더 효율적으로 동작하기도 한다.
4) 성능이 의심된다면 반드시 측정하라.
* EXPLAIN과 같은 실행 계획을 조회해본다.
