# ElasticSearch - All About Search

#### 검색하는 두 가지 방법

* **URI Searches** — **HTTP 요청의 URL 안에서 직접 검색**하는 간단한 방법이다. DSL 질의보다 제한적이지만, **빠르고 단순한 검색에 편리**하다.
* **DSL (Domain Specific Language) Queries** — URI 검색보다 **강력하고 유연**하다. Elasticsearch의 query DSL은 **복잡한 질의를 만들 수 있는 JSON 기반 언어**이며, **전문 검색, boolean 질의, 필터링, 집계** 등을 지원한다.

#### URI Search

```shell
# 문서 색인
POST /my_index/_doc/
{
  "content": "quick book",
  "publish_date": "2024-02-01"
}

POST /my_index/_doc/
{
  "content": "quick walk",
  "publish_date": "2024-03-01"
}

# URI Search
GET /my_index/_search?q=content:quick
GET /my_index/_search?q=content:quick AND content:walk
```
* `q` 파라미터에 `필드:값` 형태로 조건을 적는다.
* `AND` 같은 연산자도 쓸 수 있다.

#### DSL Queries

* **Basic Search** — `content` 필드에 `"quick"`이 담긴 문서를 찾는다.

```shell
GET /my_index/_search
{
  "query": {
    "match": {
      "content": "quick"
    }
  }
}
```

* **Full-Text Search with Query String** — `query_string` 질의로 **여러 필드에 걸친 전문 검색**을 고급 문법과 함께 수행한다.

```shell
GET /my_index/_search
{
  "query": {
    "query_string": {
      "query": "quick"
    }
  }
}
```

```shell
GET /my_index/_search
{
  "query": {
    "query_string": {
      "query": "quick OR brown",
      "fields": ["content"]
    }
  }
}
```

* **Boolean Queries** — `must`, `should`, `must_not` 같은 논리 연산자로 **여러 질의를 조합**한다.

```shell
GET /my_index/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "content": "quick" } },
        { "match": { "content": "walk" } }
      ]
    }
  }
}
```

* **Filtering Results** — 정형 조건으로 문서를 걸러낼 때 쓴다. **연관도 스코어에 영향을 주지 않는다.**

```shell
GET /my_index/_search
{
  "query": {
    "bool": {
      "filter": {
        "term": { "content": "quick" }
      }
    }
  }
}
```

* **Range Queries** — 필드 값이 **특정 범위 안에 드는** 문서를 찾는다. (숫자 범위, 날짜 범위 등)

```shell
GET /my_index/_search
{
  "query": {
    "range": {
      "publish_date": {
        "gte": "2024-01-01",
        "lte": "2024-12-31"
      }
    }
  }
}
```

* **Phrase Search** — **단어의 정확한 순서**를 찾는다.

```shell
GET /my_index/_search
{
  "query": {
    "match_phrase": {
      "content": "quick brown fox"
    }
  }
}
```

* **Wildcard Search** — **`*`(임의의 문자열)** 과 **`?`(임의의 한 문자)** 패턴으로 검색한다.

```shell
GET /my_index/_search
{
  "query": {
    "wildcard": {
      "content": "qui*"
    }
  }
}
```

#### Term-level Query란?

* Elasticsearch의 term-level query는 **숫자, 날짜, keyword 같은 정형 데이터에서 정확히 일치하는 값을 찾는** 질의다.
* 텍스트를 **분석(analyze)하고 토큰화(tokenize)하는 전문 검색 질의와 달리**, term-level query는 **색인에서 그 값 그대로를 찾는다.**
* 주로 **keyword, numeric, date, boolean** 같은 **분석되지 않는 필드**에 쓴다.

#### 주요 특징

* **Exact Matches** — 입력 텍스트를 **분석하지 않는다.** 지정한 필드에서 **정확히 일치하는 값**을 찾는다.
* **Non-Analyzed Fields** — 분석되지 않는 필드(keyword, integer, date 등)에 주로 쓴다.
* **Case Sensitivity** — keyword 필드에 쓸 때는 **대소문자를 구분한다.**

#### 대표 질의

**term Query** — 값 하나와 정확히 일치하는 문서를 찾는다.

```shell
GET /my_index/_search
{
  "query": {
    "term": {
      "status": "published"
    }
  }
}
```

**terms Query** — 나열한 값들 중 **하나라도** 일치하는 문서를 찾는다.

```shell
GET /my_index/_search
{
  "query": {
    "terms": {
      "status": ["published", "draft"]
    }
  }
}
```

#### Prefix Search

* prefix search는 **필드 값이 지정한 접두사로 시작하는 문서를 찾는** term-level query다.
* **특정 문자로 시작하는 term을 찾고 싶은데 전체 값을 모를 때** 특히 유용하다.

```shell
GET /my_index/_search
{
  "query": {
    "prefix": {
      "product_name": "iph"
    }
  }
}
```

#### Wildcard Search

* wildcard search는 **와일드카드 문자를 써서 필드 값이 지정한 패턴에 맞는 문서를 찾는** term-level query다.
* **`*` (asterisk)** — **임의의 문자열**과 일치한다. **빈 문자열도 포함**한다.
* **`?` (question mark)** — **임의의 한 문자**와 일치한다.

```shell
GET /my_index/_search
{
  "query": {
    "wildcard": {
      "product_name": "iph*ne"
    }
  }
}
```

#### Regular Expression Search

* regex search는 **필드 값이 지정한 정규식에 맞는 문서를 찾는** 강력한 term-level query다.

| 패턴 | 의미 |
|---|---|
| `.` | 임의의 한 문자 |
| `.*` | 임의의 문자열 (없어도 됨) |
| `[abc]` | a, b, c 중 한 글자 |
| `[a-z]` | a부터 z까지 소문자 중 한 글자 |
| `[0-9]` | 0부터 9까지 숫자 중 하나 |

```shell
GET /my_index/_search
{
  "query": {
    "regexp": {
      "product_name": "iph[o0]ne.*"
    }
  }
}
```

#### Full Text Search란?

* full-text search는 문서, 이메일, 기사, 웹 페이지 같은 **대량의 텍스트 데이터를 훑어 내용 기반으로 연관된 정보를 찾는** 강력한 검색 기법이다.
* **정확한 일치를 찾는 term-level query와 달리**, full-text search 질의는 **텍스트를 분석해** 더 유연하고 연관성 높은 결과를 제공한다.

#### 동작 방식

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/38c2f845-c323-44ae-82dc-1668e1e4ac79.png)

* **Text Analysis** — 토큰화, 소문자 변환, stemming, stop word 제거
* **Inverted Index** — 빠른 검색을 위해 term을 저장한다
* **Search Query** — 검색 질의가 수행되면 Elasticsearch는 **질의 텍스트도 같은 방식으로 분석한 뒤**, 역색인에서 일치하는 term을 찾는다
* **Relevance Scoring** — **term frequency, document frequency, field length** 같은 요소를 바탕으로 문서마다 연관도 점수를 계산한다

#### Relevance Scoring

* Elasticsearch는 연관도 점수 계산에 **TF-IDF(Term Frequency-Inverse Document Frequency)** 모델, **BM25**, 또는 이들의 조합을 사용한다.
* **Term Frequency (TF)**
  * 한 문서 안에서 **그 term이 얼마나 자주 등장하는지**를 잰다.
  * 빈도가 높을수록 연관도가 올라간다.
* **Inverse Document Frequency (IDF)**
  * **인덱스 전체에서 그 term이 얼마나 희귀한지**를 잰다.
  * 드문 term일수록 연관도 점수에 더 크게 기여한다.
* **Field Length Norm**
  * **짧은 필드일수록 더 연관성이 높은 경향**이 있으므로, 필드 길이가 짧으면 연관도 점수가 올라간다.

#### Leaf Queries

* leaf query는 Elasticsearch에서 **가장 단순한 형태의 질의**다.
* **특정 필드에서 특정 값을 검색**하는 역할을 한다.
  * 이 질의들은 **안에 다른 질의를 담지 않는다.**
  * **조건을 데이터와 직접 대조**한다.
* **Field-Level Operations** — 문서의 **특정 필드에 직접 작용**한다.
* **Single Condition** — term 일치, 범위, 존재 여부 같은 **단일 조건 하나를 평가**한다.
* **No Nesting** — leaf query는 **다른 질의를 담거나 조합하지 않는다.**

```shell
GET /my_index/_search
{
  "query": {
    "term": { "status": "published" }
  }
}
```

```shell
GET /my_index/_search
{
  "query": {
    "match": { "content": "Elasticsearch" }
  }
}
```

```shell
GET /my_index/_search
{
  "query": {
    "range": {
      "publish_date": {
        "gte": "2023-01-01",
        "lte": "2023-12-31"
      }
    }
  }
}
```

#### Compound Queries

* compound query는 **여러 질의를 조합하는 상위 레벨 질의**다. 조합 대상은 **leaf query일 수도, 다른 compound query일 수도** 있다.
* 다른 질의를 **조합·변형하거나 실행을 제어**해서 **더 복잡하고 정교한 검색 로직을 구성**하는 데 쓴다.
* **Combining Queries** — AND, OR, NOT 같은 **논리 연산자로 여러 질의를 조합**한다.
* **Nesting** — compound query 안에 **다른 compound query나 leaf query를 담을 수 있어**, 깊고 복잡한 검색 구조를 만들 수 있다.
* **Execution Control** — **하위 질의가 어떻게 실행되고 결과가 어떻게 결합될지**(스코어링, 필터링 등)를 제어할 수 있다.

#### Bool Query

* bool query는 **가장 강력하고 다재다능한 compound query 중 하나**다.
* **SQL이 AND, OR, NOT을 쓰는 것과 매우 비슷하게 논리 연산자로 여러 질의를 조합**할 수 있다.
* **SQL의 WHERE 절에 들어가는 boolean 로직에 해당**한다고 생각하면 된다.

| 절 | SQL 대응 | 의미 |
|---|---|---|
| **must** | `AND` | must 절의 **모든 질의가 일치해야** 문서가 매치로 간주된다 |
| **must_not** | `NOT` | must_not 절의 질의가 **일치하지 않아야** 매치로 간주된다 |
| **should** | `OR` | 일치하면 **연관도 점수가 올라간다.** must나 filter 절이 없으면 **should 중 최소 하나는 일치해야** 한다 |
| **filter** | `AND` | must와 비슷하지만 **연관도 점수에 기여하지 않는다.** true/false 같은 이진 조건으로 문서를 걸러낼 때 쓴다 |

```shell
GET /products/_search
{
  "query": {
    "bool": {
      "must": [
        { "term": { "category": "electronics" } },
        { "term": { "in_stock": true } }
      ]
    }
  }
}
```

#### Boosting Query란?

* boosting query는 **특정 질의에 가중치를 주거나**, **어떤 조건에 맞는 문서의 연관도를 낮춤(demoting)** 으로써 **문서의 연관도 점수에 영향을 주는** 질의다.
* **Positive Query** — **끌어올리고 싶은(promote·boost)** 문서를 매칭한다.
* **Negative Query** — **끌어내리고 싶은(demote)** 문서, 즉 연관도를 낮출 문서를 매칭한다.

```json
{
  "query": {
    "boosting": {
      "positive": {
        "match": { "field_name": "positive_value" }
      },
      "negative": {
        "match": { "field_name": "negative_value" }
      },
      "negative_boost": 0.5
    }
  }
}
```

* **positive** : 결과에 포함될 문서를 정한다.
* **negative** : 점수를 깎을 문서를 정한다.
* **negative_boost** — negative 질의에 일치하는 문서의 **점수를 얼마나 줄일지 계수를 지정**한다. **0과 1 사이의 값이 일반적**이며, **0이면 결과에서 완전히 제거**되고 **1이면 감점이 없다.**

#### Disjunction Max Query란?

* Disjunction Max Query(줄여서 **`dis_max` query**)는 **여러 질의를 실행하되, 일치한 질의들의 점수를 합치는 대신 가장 잘 맞은 질의 하나의 점수를 돌려주는** compound query다.

#### 동작 방식

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8ffe25f8-5959-45ae-92ff-560923f619c9.png)

* **Best Score Wins** — `dis_max`는 **하위 질의 중 가장 높은 점수를 골라** 그 문서의 연관도 점수로 삼는다. 이렇게 하면 **문서가 여러 필드나 조건에 걸릴 때 점수가 부풀려지는 것을 막는다.**
* **Tie Breaking** — 선택 파라미터인 **`tie_breaker`** 로 **최고점이 아닌 하위 질의들의 점수 일부를 더할 수 있다.** 여러 필드·조건에 걸린 문서를 **약간 밀어 올리되, 여전히 최고 매치를 우선**하게 한다.

#### multi_match와의 비교

* **multi_match Query** — 여러 필드의 점수를 **모드(`best_fields`, `most_fields` 등)에 따라 결합**한다. **점수를 합하거나 평균 내고 싶은** 다중 필드 검색에 주로 쓴다.
* **dis_max Query** — **하위 질의 중 최고 점수에 집중**한다. **최고 매치를 강조하고 싶을 때** 쓰며, 보조 매치를 위한 tie breaker를 선택적으로 둘 수 있다.

```shell
GET /products/_search
{
  "query": {
    "dis_max": {
      "queries": [
        { "match": { "name": "Speaker" } },
        { "match": { "category": "electronics" } }
      ],
      "tie_breaker": 0.3
    }
  }
}
```
* 문서 A — name 점수 8.0, category 점수 5.0
* dis_max (tie_breaker 없음)  →  8.0
* dis_max (tie_breaker 0.3)  →  8.0 + (5.0 × 0.3) = 9.5
* bool.should (점수 합산)      →  8.0 + 5.0 = 13.0
* `tie_breaker`가 `0`이면 최고점만 쓴다. `1`이면 전부 합산해 `bool.should`와 같아진다.

#### Nested Query란

* nested query는 **nested object 안의 필드를 질의**할 때 쓴다. nested object는 **배열 안의 각 객체를 별개의 개체로 다뤄야 하는** 경우를 위한 특별한 object 타입이다.
* nested query는 **중첩 문서와 부모 문서의 관계를 유지한 채로** 그 안을 검색할 수 있게 해준다.
* **일반 object 필드의 문제**
  * 일반 object 필드를 쓰면 Elasticsearch는 **객체들을 평탄화(flatten)하면서 필드 간의 관계를 잃어버린다.**
  * 예를 들어 `name`과 `price` 필드를 가진 상품 배열이 있을 때, 일반 질의는 **한 상품의 name과 다른 상품의 price를 같은 문서 안에서 잘못 매칭**할 수 있다.
* **해결책**
  * **Nested Object** — 필드를 nested로 정의하면 **배열의 각 객체가 별개의 숨겨진 문서로 색인**된다. 이 문서들은 **부모 문서와 연결**되어 있다.
  * **Nested Query** — nested query로 이 중첩 문서들 안을 검색하되, **부모 문서와의 관계를 유지**한다.

#### 평탄화가 무엇을 망가뜨리는가?

```json
{
  "products": [
    { "name": "Speaker", "price": 100 },
    { "name": "Cable",   "price": 10  }
  ]
}
```
* 위의 문서가 일반 object로 색인되면 다음과 같아진다.

```
products.name  : ["Speaker", "Cable"]
products.price : [100, 10]
```

* 두 배열이 **따로 저장되어 짝이 사라진다.(즉, 인덱스에 의한 암묵적 의존이 되어버림)**
* 그래서 **"Speaker이면서 price가 10"** 을 검색하면 **없는 조합인데도 걸린다.** `name` 배열에 Speaker가 있고 `price` 배열에 10이 있기 때문이다.
* 그래서 nested로 색인되면 다음과 같아진다.

```
{ name: "Speaker", price: 100 }
{ name: "Cable",   price: 10  }
```

#### 사용 예시

**매핑**

```shell
PUT /orders
{
  "mappings": {
    "properties": {
      "products": {
        "type": "nested",
        "properties": {
          "name":  { "type": "keyword" },
          "price": { "type": "integer" }
        }
      }
    }
  }
}
```

**질의**

```shell
GET /orders/_search
{
  "query": {
    "nested": {
      "path": "products",
      "query": {
        "bool": {
          "must": [
            { "term":  { "products.name": "Speaker" } },
            { "range": { "products.price": { "lte": 50 } } }
          ]
        }
      }
    }
  }
}
```

* `path`로 어느 nested 필드 안에서 찾을지 지정한다.
* `query` 안의 조건들은 **같은 숨겨진 문서 하나**에서 모두 만족해야 한다.

#### Inner Hits란

* Elasticsearch에서 inner hits는 **nested query 안에서 중첩 문서나 개별 hit의 상세 정보를 함께 돌려받는** 기능이다.
* nested query를 쓸 때는 **어떤 부모 문서가 걸렸는지뿐 아니라, 그 부모를 매치시킨 구체적인 중첩 문서(하위 문서)가 무엇인지**도 궁금할 수 있다. inner hits가 그것을 알려준다.

#### 왜 필요한가?

* nested query의 기본 응답은 **부모 문서 전체**다. 배열에 원소가 여러 개일 때 **그중 어느 것이 조건에 맞았는지는 응답에 나오지 않는다.**
* 예를 들어, 질의 `products 중 name="Speaker" 이고 price <= 150 인 것`을 추출한다고 가정하면 다음과 같이 기본 응답이 나오게 된다.

```shell
{
  "_source": {
    "products": [
      { "name": "Speaker", "price": 100 },   ← 이것 때문에 걸렸는데
      { "name": "Cable",   "price": 10  },
      { "name": "Speaker", "price": 300 }    ← 이건 조건에 안 맞는데
    ]
  }
}
```
* 애플리케이션에서 다시 걸러내면 되지만, **질의 조건을 코드에 한 번 더 구현**해야 하고 두 곳이 어긋날 수 있다.

```shell
GET /orders/_search
{
  "query": {
    "nested": {
      "path": "products",
      "query": {
        "bool": {
          "must": [
            { "term":  { "products.name": "Speaker" } },
            { "range": { "products.price": { "lte": 150 } } }
          ]
        }
      },
      "inner_hits": {}
    }
  }
}
```
```shell
"hits": [
  {
    "_source": { "products": [ … 전체 배열 … ] },
    "inner_hits": {
      "products": {
        "hits": {
          "hits": [
            {
              "_nested": { "field": "products", "offset": 0 },
              "_score": 1.8,
              "_source": { "name": "Speaker", "price": 100 }
            }
          ]
        }
      }
    }
  }
]
```
* **`_nested.offset`** : 배열에서 **몇 번째 원소**가 걸렸는지 알려준다.
* **`_score`** : 그 중첩 문서 자체의 점수다.
* 조건에 맞은 원소만 담기므로, **애플리케이션이 다시 걸러낼 필요가 없다.**
