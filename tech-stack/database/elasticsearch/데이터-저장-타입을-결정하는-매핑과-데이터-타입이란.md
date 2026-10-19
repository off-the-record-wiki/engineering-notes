# ElasticSearch - Defining Data Types: Understanding Mappings

#### 매핑(mapping)이란? / 데이터 타입(Data Type)

* 매핑이란 도큐먼트의 각 필드가 어떤 데이터 타입을 가지고 있는가를 정의하는 설정을 의미한다.
* MySQL에서 테이블을 만들 때 어떤 유형의 데이터를 넣을지를 스키마로 정의하는 것과 같이 ElasticSearch에서 인덱스를 만들 때 어떤 유형의 데이터를 넣을지 매핑을 정의할 수 있다.
* ElasticSearch에서 지원하는 데이터 타입 종류는 많은데 자주 사용하는 데이터 타입은 아래와 같다.
* **숫자**
  * 10억 이하 정수만 저장하면 되는 경우 : `integer`
  * 10억이 넘어가는 정수를 저장해야 될 수도 있는 경우 : `long`
  * 실수를 저장해야 될 수도 있는 경우 : `double`
* **문자**
  * 문자열을 토큰으로 쪼개 저장하고 싶은 경우 : `text`
  * 문자열을 토큰으로 쪼개지 않고 그대로 저장하고 싶은 경우(그대로 일치할 때) : `keyword`
* **기타**
  * 날짜 데이터 : `date`
  * true, false를 저장하는 경우 : `boolean`
* 계산에서 쓰는 값이 아니라면 숫자가 아닌 문자로 저장해야 한다.(Ex. 휴대폰 번호, 주민등록번호)

#### 매핑을 정의하는 두 가지 방법 : 동적 매핑(Dynamic) vs 명시적 매핑(Explicit)

* **동적 매핑(Dynamic Mapping)**
  * 매핑을 미리 정의하지 않고 도큐먼트를 색인하면, ElasticSearch가 필드 값을 보고 타입을 자동으로 추론해서 매핑을 만들어준다.
  * 이때 문자열 값은 `text`(형태소 분석용) + `keyword`(정확히 일치 검색용)[multi-field](https://www.elastic.co/docs/reference/elasticsearch/mapping-reference/multi-fields)로 자동 매핑된다.
  * 편하긴 하지만 실제로 필요 없는 `keyword` 서브필드까지 같이 저장되어 디스크 공간을 더 쓰게 되고, 의도한 타입(Ex. `date`, `integer`)으로 정확히 잡히지 않을 수도 있다. 그래서 운영 환경에서는 명시적 매핑을 권장한다.
* **명시적 매핑(Explicit Mapping)**
  * 인덱스를 생성할 때 `mappings`에 필드와 타입을 직접 정의하는 방식이다. MySQL의 `CREATE TABLE`과 비슷하다.

```shell
PUT /products
{
  "mappings": {
    "properties": {
      "name": { "type": "text" },
      "price": { "type": "integer" },
      "created_at": { "type": "date" },
      "is_sold_out": { "type": "boolean" }
    }
  }
}
```

* 이미 만들어진 인덱스의 매핑이 궁금할 때는 아래 API로 조회할 수 있다.

```shell
GET /products/_mapping
```

#### 매핑의 특징

* **null을 허용한다.**
  * 필드 값이 없어도 색인이 가능하다. (RDB의 nullable 컬럼과 유사)
* **배열을 허용한다.**
  * ElasticSearch에는 별도의 array 타입이 없다. 어떤 필드든 값을 배열로 넣으면 그 자체로 다중 값 필드가 된다.
  * 단, 배열 안의 값은 모두 같은 데이터 타입이어야 한다.
* **한 번 정의된 필드의 타입은 변경할 수 없다.**
  * MySQL은 `ALTER TABLE`로 컬럼 타입을 바꿀 수 있지만, ElasticSearch는 기존 필드의 타입을 바꾸는 것이 불가능하다.
  * 타입을 바꾸고 싶다면 새로운 매핑으로 인덱스를 새로 만들고, 기존 데이터를 [reindex](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/reindex-data) API로 옮겨야 한다.
  * 그렇기 때문에 처음 인덱스를 설계할 때 데이터 타입을 신중하게 정하는 것이 중요하다.

#### [데이터 타입](https://www.elastic.co/docs/reference/elasticsearch/mapping-reference/field-data-types)

* 자세한 내용들은 공식문서에도 참 정리가 잘 되어있다.
* 범위에 맞게끔 적용해서 개발하면 된다.
