# ElasticSearch - Commonly Used Search APIs

#### 검색 키워드가 포함된 데이터를 조회하고 싶을 때

* `match` 쿼리는 검색어를 analyzer로 분석(형태소 분석 등)한 뒤, 그 토큰이 포함된 도큐먼트를 조회한다.
* `text` 타입 필드에서만 의미가 있다. (`keyword` 타입은 애초에 분석되지 않고 통째로 저장되기 때문)

```shell
GET /products/_search
{
  "query": {
    "match": {
      "name": {
        "query": "삼성 노트북"
      }
    }
  }
}
```

#### 특정 값과 정확하게 일치하는 데이터를 조회하고 싶을 때

* `term` 쿼리는 **분석 과정 없이** 필드 값과 정확히 일치하는 도큐먼트를 조회한다.
* 그래서 `text`를 제외한 `keyword`, `integer`, `boolean` 등의 타입에서 사용한다.
* `text` 타입에도 사용은 가능하지만, 색인 시 이미 토큰 단위로 쪼개져 저장되어 있기 때문에 의도한 대로 동작하지 않을 수 있어 권장하지 않는다.

```shell
GET /products/_search
{
  "query": {
    "term": {
      "brand.keyword": {
        "value": "samsung"
      }
    }
  }
}
```

* 여러 값 중 하나라도 일치하는 도큐먼트를 찾고 싶다면 `terms` 쿼리를 사용한다.

```shell
GET /products/_search
{
  "query": {
    "terms": {
      "brand.keyword": ["samsung", "lg"]
    }
  }
}
```

#### 2가지 이상의 조건을 만족시키는 데이터를 조회하고 싶을 때

* `bool` 쿼리는 여러 조건을 조합할 때 사용하며, 절(clause)에 따라 의미가 다르다.

|절|SQL 대응|점수(score) 영향|
|:-----:|:-----:|:-----:|
|`must`|AND|O|
|`filter`|AND|X|
|`must_not`|NOT|X|
|`should`|OR (만족하면 가점, 안 해도 무관)|O|

* **filter vs must** : 조건은 동일하게 AND지만, `filter`는 관련도 점수 계산에 관여하지 않고 캐싱까지 되어 더 빠르다. 그래서 정확히 일치해야 하는 조건(Ex. 카테고리, 재고 여부)은 `filter`로, 검색어의 관련도가 중요한 조건(Ex. 상품명 매칭)은 `must`로 사용하는 것이 일반적이다.

#### 특정 조건을 만족하지 않는 데이터를 조회하고 싶을 때

* 위 `bool` 쿼리의 `must_not` 절을 사용하면 된다. `must_not`은 점수 계산에 관여하지 않고 단순히 조건에 해당하는 도큐먼트를 결과에서 제외한다.

```shell
GET /products/_search
{
  "query": {
    "bool": {
      "must_not": [
        { "term": { "status.keyword": "SOLD_OUT" } }
      ]
    }
  }
}
```

#### 숫자/날짜의 값에 대해 범위 조건으로 데이터를 조회하고 싶을 때

* `range` 쿼리는 `gte`(이상), `gt`(초과), `lte`(이하), `lt`(미만)를 조합해서 숫자나 날짜 범위로 데이터를 조회한다.

```shell
GET /products/_search
{
  "query": {
    "range": {
      "price": {
        "gte": 100000,
        "lte": 500000
      }
    }
  }
}
```
```shell
GET /products/_search
{
  "query": {
    "range": {
      "created_at": {
        "gte": "2026-01-01",
        "lte": "2026-01-31"
      }
    }
  }
}
```

#### 특정 조건을 만족하는 데이터 위주로 상위 노출시키고 싶을 때

* `bool` 쿼리의 `should` 절을 사용하면 된다. `must`/`filter`와 함께 쓰이는 `should`는 조건 충족 여부와 무관하게 도큐먼트를 결과에 포함시키되, 조건을 만족한 도큐먼트의 점수만 올려준다.
* 즉, 검색 결과 자체를 걸러내는 것이 아니라 **정렬 순서(노출 우선순위)** 를 조정하고 싶을 때 사용한다.

```shell
GET /products/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "노트북" } }
      ],
      "should": [
        { "term": { "is_ad.keyword": "true" } }
      ]
    }
  }
}
```

* `bool` 쿼리에 `must`나 `filter`가 없이 `should`만 있는 경우에는 동작이 달라진다. 이때는 `should` 중 하나 이상을 만족해야 결과에 포함된다.(OR 조건처럼 동작)

#### 오타가 있더라도 유사한 단어를 포함한 데이터를 조회하고 싶을 때

* `fuzzy` 쿼리는 [Levenshtein 편집 거리](https://www.elastic.co/docs/reference/query-languages/query-dsl/query-dsl-fuzzy-query#_fuzziness)(글자를 몇 번 삽입/삭제/치환해야 같은 단어가 되는지)를 기준으로 유사한 단어를 찾는다.
* `fuzziness` 값을 직접 지정하거나 `AUTO`로 두면 단어 길이에 따라 자동으로 허용 편집 거리를 정해준다.

```shell
GET /products/_search
{
  "query": {
    "fuzzy": {
      "name": {
        "value": "갤럭시",
        "fuzziness": "AUTO"
      }
    }
  }
}
```

* `match` 쿼리에도 `fuzziness` 옵션을 추가해서 오타를 허용하는 검색을 할 수 있다.

```shell
GET /products/_search
{
  "query": {
    "match": {
      "name": {
        "query": "겔럭시",
        "fuzziness": "AUTO"
      }
    }
  }
}
```

#### 여러 필드에서 검색 키워드가 포함된 데이터를 조회하고 싶을 때

* `multi_match` 쿼리는 하나의 검색어로 여러 필드를 동시에 검색할 때 사용한다. (Ex. 검색창 하나로 상품명, 브랜드명, 설명을 동시에 검색)
* `fields`에 가중치(`^`)를 줘서 특정 필드의 매칭 점수를 더 높게 반영할 수도 있다.

```shell
GET /products/_search
{
  "query": {
    "multi_match": {
      "query": "삼성 노트북",
      "fields": ["name^2", "brand", "description"]
    }
  }
}
```

#### 검색한 키워드를 하이라이팅 처리하고 싶을 때

* `highlight`를 사용하면 검색어와 일치한 부분을 태그로 감싸서 반환해준다. 프론트에서 검색어를 강조 표시할 때 사용한다.

```shell
GET /products/_search
{
  "query": {
    "match": { "name": "노트북" }
  },
  "highlight": {
    "fields": {
      "name": {}
    }
  }
}
```

* 응답 결과의 `highlight.name`에 기본적으로 `<em>` 태그로 감싸진 문자열이 내려온다. (Ex. `삼성 <em>노트북</em>`)
* `pre_tags` / `post_tags`로 감싸는 태그를 직접 지정할 수도 있다.

#### 페이지네이션

* `from` / `size`로 페이지 단위 조회를 할 수 있다. `from`은 건너뛸 문서 수, `size`는 가져올 문서 수를 의미한다.

```shell
GET /products/_search
{
  "from": 20,
  "size": 10,
  "query": {
    "match_all": {}
  }
}
```
* `from` + `size`가 커질수록(깊은 페이지로 갈수록) 내부적으로 `from + size`만큼의 문서를 모두 정렬한 뒤 잘라내기 때문에 성능이 급격히 나빠진다. 기본적으로 `from + size`는 10,000을 넘을 수 없다.
* 무한 스크롤처럼 깊은 페이지네이션이 필요하다면 [search_after](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/paginate-search-results#search-after) 방식을 사용하는 것이 좋다. (이전 페이지 마지막 도큐먼트의 정렬 기준값을 다음 요청의 커서로 사용)

#### 정렬

* 기본적으로 검색 결과는 관련도 점수(`_score`) 순으로 정렬된다. `sort`를 지정하면 원하는 필드 기준으로 정렬할 수 있다.
* `text` 타입은 정렬에 사용할 수 없고, `keyword`나 숫자/날짜 타입 필드로만 정렬이 가능하다.

```shell
GET /products/_search
{
  "query": {
    "match_all": {}
  },
  "sort": [
    { "price": "asc" },
    { "created_at": "desc" }
  ]
}
```

#### 하나의 필드에 text와 keyword 타입을 동시에 사용하고 싶을 때

* `fields` 옵션을 사용하면 원본 필드는 `text`로 색인하면서, 같은 값을 `keyword` 서브필드로도 함께 색인할 수 있다.
* 하나의 필드로 "유연한 검색"(match)과 "정확한 일치/정렬/집계"(term, sort, aggregation)를 모두 지원하고 싶을 때 사용한다.

```shell
PUT /products
{
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "fields": {
          "keyword": {
            "type": "keyword"
          }
        }
      }
    }
  }
}
```

* 조회할 때는 `필드명.서브필드명` 형태로 접근한다.

```shell
GET /products/_search
{
  "query": {
    "term": {
      "name.keyword": {
        "value": "삼성 갤럭시북4"
      }
    }
  }
}
```

#### 검색 키워드를 일부 입력했을 때 검색어를 추천하는 자동 완성 기능

* `search_as_you_type` 타입은 자동 완성 기능을 쉽게 구현할 수 있도록 ElasticSearch가 제공하는 전용 데이터 타입이다.
* `text` 타입처럼 analyzer를 거쳐 토큰으로 분리되지만, 매핑 시 내부적으로 `_2gram`, `_3gram`, `._index_prefix` 등의 서브필드를 자동으로 함께 생성해준다.

```shell
PUT /products
{
  "mappings": {
    "properties": {
      "name": {
        "type": "search_as_you_type"
      }
    }
  }
}
```

* 검색할 때는 `multi_match` 쿼리에 `type: bool_prefix`를 사용해서, 원본 필드와 `_2gram`, `_3gram` 서브필드를 함께 조회한다.

```shell
GET /products/_search
{
  "query": {
    "multi_match": {
      "query": "삼성 노트",
      "type": "bool_prefix",
      "fields": ["name", "name._2gram", "name._3gram"]
    }
  }
}
```

#### _2gram , _3gram

* `search_as_you_type` 매핑 시 자동으로 생성되는 서브필드로, 검색어를 몇 개의 단어(토큰) 단위로 묶어서 인덱싱할지를 나타낸다.
* `_2gram`은 인접한 두 단어를 하나의 토큰으로 묶어서 색인한다. (Ex. "삼성 노트북 케이스" → `삼성 노트북`, `노트북 케이스`)
* `_3gram`은 인접한 세 단어를 하나의 토큰으로 묶어서 색인한다. (Ex. "삼성 노트북 케이스" → `삼성 노트북 케이스`)
* 사용자가 입력을 이어갈수록 더 긴 n-gram 서브필드가 매칭에 관여하게 되어, 입력 길이에 맞는 자연스러운 자동완성 결과를 제공할 수 있다.
