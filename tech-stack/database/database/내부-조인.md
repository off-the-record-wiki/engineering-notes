# Database - Inner Join

>❗조인(JOIN)이 필요한 이유
> * 데이터 중복(Redundancy)
> * 갱신 이상(Update Anomaly)
> * 삽입 이상(Insert Anomaly)
> * 삭제 이상(Delete Anomaly)
* 하나의 테이블에 모두 다 저장하면 위와 같은 문제들을 겪게 될 것이기 때문에 데이터베이스를 설계할 때 정규화라는 과정을 거쳐야 하는 것이다.
* 데이터의 중복을 막고 일관성을 지키기 위해서 조인(JOIN)을 사용하는 것이다.

## 📚 내부 조인(INNER JOIN)

* 내부 조인은 두 테이블을 연결할 때, 양쪽 테이블에 모두 공통으로 존재하는 데이터만을 결과로 보여준다.

```sql
SELECT 컬럼1, 컬럼2, ...
FROM 테이블A
INNER JOIN 테이블B
ON 테이블A.연결컬럼 = 테이블B.연결컬럼;
```
* FROM : 기준이 되는 첫 번째 테이블 지정
* INNER JOIN : 연결할 두 번째 테이블 지정
* ON : 두 테이블을 어떤 조건으로 연결할지 명시하는 연결고리

>❗INNER 생략 가능하다.
> * 내부 조인에서 INNER는 생략할 수 있다.
> * 그냥 JOIN이라고만 쓰면 자동으로 INNER JOIN으로 작동한다.
> * 어떤 테이블의 필드인지 명시하는 것이 가독성을 높여주고, 컬럼 소속을 쉽게 인지할 수 있기 때문에 사용하는 것을 권장한다.

### JOIN의 작동 순서

```sql
SELECT
    users.user_id,
    users.name,
    orders.order_date
FROM 
    orders
INNER JOIN 
    users ON orders.user_id = users.user_id
WHERE 
    orders.status = 'COMPLETED';
```
1) FROM/JOIN : 가장 먼저 FROM절의 orders 테이블과 INNER JOIN으로 연결된 users 테이블을 연결하기 위해 각 테이블의 user_id 필드를 결합해 하나의 큰 가상 테이블을 만들어낸다.
2) WHERE : JOIN을 통해 생성된 가상 테이블에서 WHERE절의 조건인 COMPLETED를 만족하는 행들만 필터링한다.
3) SELECT : 필터링된 결과에서 SELECT 절에 명시된 컬럼을 추출해 최종 결과를 반환한다.

### 내부 조인과 조인 방향

* 내부 조인은 양방향이다. A 테이블과 B 테이블이 있다고 가정할 때, A -> B로 조인할 수 있다면 반대로 B -> A로도 조인할 수 있다는 의미이다. 즉, 그 결과는 항상 동일하다.

### 내부 조인의 순서

* 내부 조인에서는 결과가 항상 같기 때문에 어떤 순서로 작성해도 무방하나 쿼리를 읽는 사람 입장에서 어떤 데이터가 중심이 되는지를 정하면 가독성이 높아진다.
  * ex1) 주문 목록을 중심으로 고객 정보를 추가하고 싶다면 -> `FROM orders JOIN users`
  * ex2) 고객 목록을 중심으로 주문 정보를 조회하고 싶다면 -> `FROM users JOIN orders`

### 가독성을 높이는 테이블 별칭(Alias)을 활용하자.

* 테이블에 별칭으로 사용하는 AS는 주로 생략한다.
* 컬럼에 별칭으로 사용하는 AS는 가독성을 위해 생략하지 않고 사용한다.
>❗회사에서 소스 코드의 쿼리문을 작성할 때, 테이블과 필드명 모두에 AS를 사용했었는데 이런 부분을 알게 되어 적용해보려고 노력해야겠다.
