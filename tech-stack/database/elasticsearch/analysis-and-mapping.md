# ElasticSearch - Analysis and Mapping

#### Analysis란

* **텍스트 데이터를 효율적으로 검색할 수 있는 형태로 변환하는 과정**을 가리킨다.
* 이 과정은 **토큰화(tokenization), 필터링(filtering), 정규화(normalization)** 등 여러 단계로 이루어진다.
* 목표는 텍스트를 **색인하고 질의할 수 있는 term(토큰)으로 분해**하는 것이다.

#### Analyzer의 세 가지 구성요소

| 구성요소 | 역할 | 예시 |
|---|---|---|
| **Character Filters** | 토큰화 **이전**에 입력 텍스트를 전처리한다. 문자나 문자열을 **제거하거나 치환**한다 | HTML Stripping, Mapping, Pattern Replacement |
| **Tokenizers** | 텍스트를 **개별 term으로 쪼갠다**. 공백·구두점 등 어떤 기준으로 나눌지 정의한다 | Standard, Whitespace, Keyword Tokenizer |
| **Token Filters** | 토큰을 수정한다. 주로 **정규화**하거나(소문자 변환, 불용어 제거) 추가 변환을 적용한다 | Lowercase, Stop, Stemmer Token Filter |

* 순서가 정해져 있다 — **Character Filters → Tokenizer → Token Filters**.
* Character Filter는 **문자 단위**로 원문을 손보고, Tokenizer는 **문자열을 토큰 배열로** 바꾸며, Token Filter는 **토큰 단위**로 다듬는다. 다루는 대상이 단계마다 달라진다.

#### 실제 변환 과정

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/58e8d747-81a2-4ce7-9e0c-21c841a6b5f8.png)

* **Character Filter** 단계에서 `<p>` 태그가 제거된다. 아직 하나의 문자열이다.
* **Tokenizer** 단계에서 공백 기준으로 잘려 **배열**이 된다.
* **Token Filter** 단계에서 각 토큰이 소문자로 바뀐다. `Elasticsearch` → `elasticsearch`.

#### 역색인이란

* 역색인은 Elasticsearch를 포함한 검색 엔진에서 **빠르고 효율적인 전문 검색을 가능하게 하는 기본 자료구조**다.
* **term(단어 또는 토큰)을 문서 집합 내의 위치에 매핑**하여, 특정 term을 포함한 문서를 빠르게 찾을 수 있게 한다.

#### 핵심 개념

* **Terms** — 색인되는 개별 단어나 토큰이다.
* **Posting List** — 각 term에 대해, **그 term이 등장하는 문서들의 목록**이다(문서 내 위치 정보 포함).
* **Documents** — 색인되고 검색되는 텍스트 데이터의 집합이다.

#### 역색인이 만들어지는 과정

* **Document Parsing** — 문서를 파싱해 개별 term으로 분해한다. 이때 **analyzer**(character filter, tokenizer, token filter)를 사용한다.
* **Term Extraction** — 문서에서 term을 추출하고, **각 term을 문서 식별자와 연결**한다.
* **Index Creation** — 각 term마다 역색인에 항목을 만든다. 이 항목은 **term과 posting list**로 구성되며, posting list는 그 term을 포함한 모든 문서에 대한 참조를 담는다.

#### 역색인 메커니즘

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b2b61fd9-bef2-49c1-a70f-c51a5237c044.png)

**원본 문서**

```
Document 1: "Elasticsearch is a search engine"
Document 2: "Search engines are powerful"
```

**분석 후 토큰**

```
Document 1: ["elasticsearch", "is", "a", "search", "engine"]
Document 2: ["search", "engines", "are", "powerful"]
```

* 방향이 뒤집혀 있다. 원래 데이터는 `문서 → 단어들`이지만, 역색인은 **`단어 → 문서들`** 이다. 그래서 "역"색인이다.
* `search`는 두 문서 모두에 나오므로 posting list가 `1, 2`다.

#### 역색인의 장점

* **효율성(Efficiency)** — term과 그에 연결된 문서를 빠르게 조회할 수 있어 검색이 매우 빠르다.
* **확장성(Scalability)** — 대량의 텍스트 데이터를 효율적으로 다룰 수 있다.
* **연관성(Relevance)** — Boolean 질의, 구문(phrase) 질의, 근접(proximity) 검색 같은 고급 질의를 지원한다.

#### Mapping이란

* Elasticsearch에서 mapping은 **인덱스 안의 문서에 대한 스키마 정의**다.
* 문서와 그 필드들이 **어떻게 저장되고 색인되는지**를 정의한다. 데이터 타입, analyzer, 그 밖에 데이터가 처리되고 질의되는 방식에 영향을 주는 설정들이 포함된다.
* **관계형 데이터베이스의 스키마에 해당**하는 개념이다.

#### 구성요소

| 구성요소 | 의미 |
|---|---|
| **Index** | 유사한 특성을 가진 문서들의 모음 |
| **Document** | 인덱스에 저장되는 JSON 객체 |
| **Field** | 문서 안의 각 key-value 쌍 |
| **Data Type** | 필드가 담는 데이터의 종류를 지정 (text, keyword, date, integer 등) |

#### Mapping 정의 예시

```shell
PUT /my_index
{
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "standard"
      },
      "tags": {
        "type": "keyword"
      },
      "views": {
        "type": "integer"
      },
      "published_at": {
        "type": "date"
      }
    }
  }
}
```

* **`"analyzer": "standard"`** — 해당 텍스트를 standard analyzer로 처리한다.
* **`"type": "keyword"`** — 이 필드는 **정확한 값**을 담는다. 분석 없이 **필터링, 정렬, 집계**에 적합하다.
* **`"type": "integer"`** — 정수 값이다.
* **`"type": "date"`** — Elasticsearch는 다양한 날짜 형식을 처리할 수 있다.

#### Data Type

**1. Core Data Type**

* **Text** — 분석되는 텍스트다. 전문 검색에 적합하며, **analyzer를 거쳐 term으로 만들어져 색인**된다.
* **Keyword** — **하나의 term으로 통째로 다뤄야 할** 정형 데이터에 쓴다.
* **Numeric** — 숫자를 위한 여러 타입이다. `long`, `integer`, `short`, `byte` 등.
* **Date** — 날짜와 시간에 쓴다.
* **Boolean** — true/false 값에 쓴다.
* **Range** — 숫자, 날짜, IP의 범위에 쓴다.

**2. Complex Data Type**

* **Object** — 문서 안의 중첩 객체에 쓴다.

```
{
  "type": "object",
  "properties": {
    "name": { "type": "text" },
    "age":  { "type": "integer" }
  }
}
```

* **Nested** — object의 특별한 형태로, **중첩된 객체들을 별개의 문서처럼 질의**할 수 있게 한다.

```
{
  "type": "nested",
  "properties": {
    "comments": {
      "type": "nested",
      "properties": {
        "user":    { "type": "keyword" },
        "message": { "type": "text" },
        "date":    { "type": "date" }
      }
    }
  }
}
```

**3. Special Data Type**

* **Geo-Data** — 지리 데이터에 쓴다.
* **IP** — IP 주소 저장에 쓴다.
* **Completion** — 자동완성이나 추천 기능에 쓴다.
* **Token Count** — 텍스트의 토큰 개수를 세는 데 쓴다.
* **Percolator** — **문서에 대해 실행할 쿼리를 저장**하는 데 쓴다.

**4. Binary Data Type**

* **Binary** — Base64로 인코딩된 바이너리 데이터 저장에 쓴다.

#### Date Detection

* 문서를 색인할 때, 값이 **내장 date 형식 중 하나를 따르면 Elasticsearch가 date 필드를 자동으로 감지**한다.
* 다만 **모호함을 피하려면 mapping에 date 형식을 명시적으로 정의하는 편이 낫다.**

#### Date Formats

* Elasticsearch는 여러 date 형식을 지원한다.
* 기본 형식은 **`strict_date_optional_time||epoch_millis`** 다.
  * 먼저 `strict_date_optional_time` 형식으로 파싱을 시도한다.
  * 실패하면 **epoch 기준 밀리초**로 파싱을 시도한다.

#### Mapping Date Fields

* 인덱스의 mapping을 정의할 때 **`format` 파라미터로 date 필드의 형식을 지정**할 수 있다.
* 이렇게 하면 색인할 때와 질의할 때 Elasticsearch가 날짜를 올바르게 파싱한다.

#### 예시 — 서로 다른 형식이 하나로 저장된다

```shell
POST /date_playground/_doc/1
{
  "date_default":       "2024-06-07T12:00:00Z",
  "date_custom_format": "2024-06-07 12:00:00",
  "date_iso8601":       "2024-06-07T12:00:00.000Z",
  "date_millis":        1717761600000
}
```
* 네 필드의 표기법은 전부 다르지만, 가리키는 시각은 같다.
* Elasticsearch는 이들을 파싱해 **내부적으로 epoch 밀리초(long) 하나로 저장**한다.
* 그래서 형식이 달라도 **범위 검색, 정렬, 집계가 동일하게 동작**한다.

#### 매핑 파라미터란

필드의 타입 외에, **그 필드를 어떻게 다룰지** 지정하는 옵션들이다.

| 파라미터 | 역할 | 비고 |
|---|---|---|
| **index** | 필드를 **검색 가능하게 할지** 결정한다 | 끄면 공간 절약과 처리량 개선. 예: 시계열 데이터 |
| **format** | date 필드에서 **허용할 날짜 형식**을 지정한다 | |
| **coerce** | true면 입력 데이터를 **필드 타입에 맞게 변환**하려 시도한다 | 예: `"100"` → `100` |
| **doc_values** | **효율적인 정렬·집계·접근 패턴**을 지원한다 | 끄면 공간 절약. 예: `price`, `release_date` |
| **norms** | **스코어링에 사용**된다 | 끄면 공간 절약. 예: `tags` |
| **null_value** | null 값을 대체할 값을 지정한다 | 예: `unknown` |
| **copy_to** | 지정한 필드들을 **추가 필드에 함께 색인**한다 | 예: `first_name`, `last_name` → `full_name`으로 검색 |

#### 각 파라미터 상세

* `index` — 이 필드로 검색할 일이 없다면 끈다. 역색인을 만들지 않으므로 저장 공간이 줄고 색인 처리량이 오른다. 시계열 데이터처럼 **저장은 하되 검색 조건으로는 쓰지 않는** 필드가 대상이다.
* `format` — date 필드에서 받아들일 날짜 형식을 지정한다.
* `coerce` — 타입이 어긋나는 입력을 자동 변환한다. 문자열 `"100"`이 integer 필드로 들어오면 숫자 `100`으로 바꿔 저장한다.
* `doc_values` — 정렬과 집계를 위한 구조다. 검색은 역색인(단어 → 문서)을 쓰지만, 정렬·집계는 반대 방향(문서 → 값)이 필요해 별도로 만든다. `price`, `release_date`처럼 정렬·집계에 쓰는 필드에 필요하고, 쓰지 않는 필드는 꺼서 공간을 아낀다.
* `norms` — 스코어링에 쓰이는 정보다. `tags`처럼 **연관도 점수가 의미 없는** 필드는 꺼도 된다.
* `null_value` — 값이 null일 때 대신 색인할 값을 지정한다. null은 원래 색인되지 않아 검색으로 찾을 수 없는데, `unknown` 같은 대체값을 두면 "값 없음"도 검색 대상이 된다.
* `copy_to` — 여러 필드의 값을 하나의 추가 필드에 모아 색인한다. **데이터를 따로 저장하는 것이 아니라**, 여러 필드를 하나인 것처럼 검색할 수 있게 해준다. `first_name`과 `last_name`을 `full_name`으로 모으면 이름 전체로 한 번에 검색된다.

#### 매핑은 바꿀 수 없다

* Elasticsearch에서는 인덱스에 매핑이 한번 만들어지면, **그 매핑의 특정 부분은 변경할 수 없다.**
* 이 **불변성(immutability)** 은 주로 **Elasticsearch가 데이터를 저장하고 색인하는 방식** 때문에 생긴다.

#### 바꿀 수 없는 다섯 가지 이유

* **Lucene Index Structure** — Lucene 세그먼트를 **다시 써야 한다.**
* **Data Integrity and Consistency** — 필드 타입을 string에서 integer로 바꾸는 것과 같은 문제다.
* **Performance Considerations** — 매핑 변경을 반영하려고 대량의 데이터를 재색인하는 일은 **자원과 시간을 많이 소모**한다.
* **Field Type Incompatibility** — 저장된 데이터를 다시 해석하는 것은 **전체 데이터셋을 재색인하지 않고는 불가능**하다. (예: integer → date)
* **Index Structure Optimization** — Elasticsearch는 **최초 매핑을 기준으로 인덱스 구조를 최적화**해 둔다.

#### 해결 방법

* **새 인덱스를 만든다** — 원하는 매핑으로 새 인덱스를 정의하고, **Reindex API**로 기존 인덱스의 데이터를 새 인덱스로 옮긴다.
  * 원하는 매핑으로 새 인덱스를 생성한다.
  * Reindex API로 기존 데이터 이전한다.
  * 애플리케이션이 새 인덱스를 바라보게 전환한다.

#### Multi-fields란

* Elasticsearch의 multi-fields mapping은 문서의 **하나의 필드를 여러 방식으로 색인**할 수 있게 해준다. 그래서 같은 데이터에 대해 **서로 다른 종류의 검색**을 수행할 수 있다.
* **같은 텍스트를 여러 방식으로 분석하고 싶을 때** 유용하다. 예를 들어 하나의 필드를 **text(전문 검색용)와 keyword(정확히 일치 검색용)로 동시에** 색인하는 경우다.

#### Multi-Fields의 이점

* **Versatility** — 같은 데이터를 **서로 다른 종류의 질의에 사용**할 수 있다.
* **Efficiency** — 질의 목적별로 **인덱스에 데이터를 중복 저장할 필요를 줄인다.**
* **Flexibility** — 같은 필드에 **서로 다른 analyzer를 적용**할 수 있어, 검색 요구사항별로 최적화가 가능하다.

#### 정의 예시

```shell
PUT /my_index
{
  "mappings": {
    "properties": {
      "title": {
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
* `title` — analyzer를 거쳐 토큰으로 쪼개진다. 전문 검색용이다.
* `title.keyword` — 통째로 하나의 term이 된다. 정확히 일치하는 값 찾기, 정렬, 집계용이다.

#### Index Template이란

* Elasticsearch의 index template은 **새 인덱스가 생성될 때 자동으로 적용될 settings, mappings, aliases를 미리 정의**해 두는 설정이다.
* **비슷한 구조를 따르는 인덱스들을 관리할 때** 특히 유용하다. 로깅이나 메트릭 용도의 **시계열 인덱스**가 대표적이다.

#### 핵심 개념

* **Index Patterns** — index template은 **특정 패턴에 일치하는 인덱스에 적용**된다. 예를 들어 `log-*` 패턴은 이름이 `log-`로 시작하는 모든 인덱스에 적용된다.
* **Priority** — 템플릿은 우선순위 값을 가질 수 있고, 이 값이 **여러 템플릿이 적용되는 순서**를 결정한다. **우선순위가 높은 템플릿이 낮은 템플릿의 설정을 덮어쓴다.**
* **Settings** — 샤드 개수, 레플리카 개수 등 **인덱스 수준의 설정**이다.
* **Mappings** — 인덱스 안 필드들의 **데이터 타입과 설정**을 정의한다.
* **Aliases** — 인덱스의 **대체 이름**이다. 질의와 인덱스 관리를 단순하게 해준다.

#### Reserved index patterns

* Elasticsearch에는 **내장 index template**이 있고, 각각 **우선순위 100**을 가진다. 대상 패턴은 다음과 같다.

```
logs-*-*
metrics-*-*
synthetics-*-*
profiling-*
```

* 참고: <https://www.elastic.co/guide/en/elasticsearch/reference/current/index-templates.html>

#### 정의 예시

```shell
PUT _index_template/my_log_template
{
  "index_patterns": ["log-*"],
  "priority": 200,
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1
    },
    "mappings": {
      "properties": {
        "timestamp": { "type": "date" },
        "level":     { "type": "keyword" },
        "message":   { "type": "text" }
      }
    },
    "aliases": {
      "logs-all": {}
    }
  }
}
```

```
log-2026-08-01  ─┐
log-2026-08-02  ─┼─ 이름이 log-* 이므로 생성 시 템플릿이 자동 적용
log-2026-08-03  ─┘   (settings + mappings + aliases)
```

#### Dynamic Mapping이란

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/7a0c9c1d-4756-4f21-800c-30dbcf07a363.png)

* Elasticsearch의 dynamic mapping은 **새 문서가 색인될 때 인덱스의 필드 매핑을 자동으로 만들고 갱신**해 주는 기능이다.
* 덕분에 **매핑을 미리 명시하지 않아도 새 필드를 즉석에서 처리**할 수 있다. **들어올 데이터의 구조를 미리 알 수 없는 상황**에서 특히 유용하다.
* **편리함의 대가가 있다.** dynamic mapping은 **첫 문서를 보고 타입을 결정**하는데, 그 판단이 늘 옳지는 않다. 우편번호 `"01234"`가 숫자로 잡혀 앞의 0이 사라지거나, 버전 문자열 `"1.0"`이 float으로 잡히는 식이다. 그리고 매핑 변경 문서에서 본 대로 **한번 굳은 타입은 재색인 없이 바꿀 수 없다.**
* **그래서 운영 인덱스는 매핑을 명시하는 편이 낫다.** 구조를 아는 데이터라면 직접 정의하고, 매일 새 인덱스가 생기는 시계열이라면 **index template**으로 매핑을 미리 걸어 둔다. dynamic mapping은 데이터 구조를 아직 모르는 탐색 단계나 로그 수집처럼 필드가 계속 늘어나는 경우에 쓴다.

#### 기본 동작

* **Strings** — 값이 텍스트로 보이면 **`text` 타입에 `keyword` 하위 필드**를 함께 붙여 매핑한다.
* **Numbers** — 감지된 형식에 따라 `long`, `double` 등으로 매핑한다.
* **Floating point** — `float`으로 매핑한다.
* **Dates** — 날짜처럼 보이는 문자열은 `date`로 매핑한다.
* **Booleans** — true/false처럼 보이는 값은 `boolean`으로 매핑한다.
* **Object** — `object`로 매핑한다.

#### dynamic 파라미터란

* Elasticsearch에서 매핑의 **`dynamic` 파라미터**는 **매핑에 명시적으로 정의되지 않은 필드를 어떻게 처리할지** 정한다.

#### 세 가지 값

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/25b58415-e3ea-482c-832f-1a881ccb3065.png)

* **`true`** — (기본값) 새 필드를 만나면 **자동으로 매핑에 추가**한다.
* **`false`** — 매핑에 명시되지 않은 새 필드를 **무시**한다. 이 필드들은 **색인되지도, 저장되지도 않는다.**
* **`strict`** — 매핑에 정의되지 않은 필드가 담긴 문서를 **거부하고 에러를 반환**한다.

#### Dynamic Template이란

* Elasticsearch의 dynamic template은 **동적으로 추가되는 필드에 대해 직접 규칙을 정의**할 수 있게 해준다.
* **`dynamic_templates`** 는 **필드의 이름, 타입, 그 밖의 속성을 기준으로** 특정 필드에 대해 더 세밀한 규칙을 정의하게 해준다.
* **Flexibility** — 필드가 동적으로 추가될 때 **어떻게 색인되고 분석될지를 타입별로 지정**할 수 있다.
* **Control** — **필드명이나 데이터 타입 같은 패턴·조건에 따라** 특정 매핑을 적용할 수 있다.
* **Efficiency** — **모든 필드를 미리 정의하지 않고도** analyzer나 색인 옵션 같은 설정을 자동으로 적용할 수 있다.

#### 정의 구조

```shell
PUT /my_index
{
  "mappings": {
    "dynamic_templates": [
      {
        "template_name": {
          "match": "pattern",
          "match_mapping_type": "type",
          "mapping": {
            "type": "desired_type",
            "other_options": "..."
          }
        }
      }
    ]
  }
}
```

| 항목 | 역할 |
|---|---|
| `template_name` | 템플릿 이름 |
| `match` | 필드명 패턴으로 대상을 고른다 |
| `match_mapping_type` | 동적으로 감지된 타입으로 대상을 고른다 |
| `mapping` | 조건에 맞은 필드에 적용할 매핑 |

* `dynamic_templates`는 **배열**이다. 여러 규칙을 순서대로 나열하고, **먼저 일치하는 규칙이 적용**된다.

#### Stemming

* stemming은 자연어 처리(NLP)와 정보 검색에서 **단어를 어간(base or root) 형태로 줄이는 과정**이다.
* 목적은 **한 단어의 여러 형태를 하나로 묶어 단일 항목으로 분석**하는 것이다.

```
Original Words : "running", "runner", "ran", "runs"
Stemmed Form   : "run"
```

**Stemming Algorithms**

* **Porter Stemmer** — 가장 널리 쓰이는 stemming 알고리즘 중 하나다. **일련의 규칙을 적용**해 단어를 어근 형태로 변환한다.
* **Snowball Stemmer** — Porter Stemmer의 **더 발전되고 설정 가능한 버전**이다.

#### Stop Words

* stop words는 검색 엔진이나 NLP 작업에서 **처리 전에 제거되는 흔한 단어들**이다.
* 이 단어들은 대개 **매우 흔하고 고유한 의미를 거의 담지 않으므로**, 제외하면 **인덱스 크기를 줄이고 검색 성능을 높일 수 있다.**

```
Sentence                  : "The quick brown fox jumps over the lazy dog."
After Removing Stop Words : "quick brown fox jumps over lazy dog"
```

#### 내장 Analyzer

* **standard** — Unicode 텍스트 분할 규칙에 따라 텍스트를 단어로 쪼갠다.
  * Tokenizer: `standard`
  * Token Filters: `lowercase`, stop 지원
* **simple** — 글자가 아닌 문자를 기준으로 나누고 토큰을 소문자로 만든다.
  * Tokenizer: `lowercase`
* **whitespace** — 공백(스페이스, 탭 등)을 기준으로 텍스트를 토큰으로 나눈다.
  * Tokenizer: `whitespace`
* **stop** — simple analyzer와 비슷하되 **stopword 필터링이 추가**된다.
  * Tokenizer: `whitespace`
* **keyword** — **텍스트를 토큰화하지 않는다.** 입력 전체를 **하나의 토큰**으로 다룬다. **정확히 일치하는 검색**에 주로 쓴다.
  * Tokenizer: `keyword`
* **pattern** — **정규식 패턴**을 기준으로 텍스트를 나눈다.
  * Tokenizer: `pattern`
* 더 많은 정보: <https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-analyzers.html>
