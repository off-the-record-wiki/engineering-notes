# Database - Data Processing

## 📚 산술 연산

```sql
SELECT NAME, PRICE, STOCK_QUANTITY, PRICE * STOCK_QUANTITY AS TOTAL_STOCK_VALUE
```
* 위와 같이 곱셈뿐만 아니라 다양한 사칙 연산 적용 가능

## 📚 문자열 함수

**CONCAT** : 문자열 합치기
**CONCAT_WS** : 인자 사이를 구분자를 받아 각 문자열 사이에 자동으로 넣어준다.

## 📚 NULL 함수

**IFNULL** : 2개의 인자를 받는데 첫 번째 인자는 NULL인지 아닌지 검사할 컬럼이나 값이 오고 두 번째 인자에서 첫 번째 인자1이 NULL일 경우 대신 반환할 값이 온다.
**COALESCE** : 괄호 안에 여러 인자를 전달할 수 있으며 왼쪽부터 차례대로 확인해서 처음으로 NULL이 아닌 값을 반환한다.

> [참고 - MySQL 다양한 함수들](https://dev.mysql.com/doc/refman/8.4/en/functions.html)
