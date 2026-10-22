# ElasticSearch - Basic Search Features & Architecture

#### ElasticSearch - 기본 검색 기능 및 작동 원리에 대한 이해

* 역인덱스는 필드 값을 단어마다 쪼개서 찾기 쉽게 정리해놓은 목록이다.

```shell
POST /products/_doc
{
    "name" : "Apple 2025 맥북 에어 13 M4 10코어"
}
```
* 이 문서가 색인되면 `name` 필드는 다음과 같은 형태로 분리되어 저장된다.

| 토큰 | 문서 ID |
|---|---|
| apple | 1 |
| 2025 | 1 |
| 맥북 | 1 |
| 에어 | 1 |
| 13 | 1 |
| m4 | 1 |
| 10코어 | 1 |

* 이렇게 **토큰 → 문서 ID** 방향으로 뒤집어 정리한 목록을 역인덱스라고 한다.
* RDB의 `LIKE '%맥북%'`이 모든 행을 훑는 것과 달리, 역인덱스는 `맥북` 항목만 찾으면 해당 문서를 바로 얻는다.

#### 애널라이저(Analyzer)

* `products` 인덱스의 `name` 필드값을 토큰으로 분리해 역인덱스로 저장하는 과정을 거친다.
* 이 때, 문자열을 토큰으로 변환시켜주는 장치를 애널라이저(Analyzer)라고 한다.

```shell
POST /products/_doc
{
    "name" : "Apple 2025 맥북 에어 13 M4 10코어"
}
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8ba53168-58ec-40f3-9087-17305dd741f4.png)

* **캐릭터 필터** : 문자열을 토큰으로 자르기 전에 문자열을 다듬는 역할을 수행한다.(0개 이상)
* **토크나이저** : 문자열을 토큰으로 자르는 역할을 수행한다.(**정확히 1개, 필수**)
* **토큰 필터** : 잘린 토큰을 최종적으로 다듬는 역할을 수행한다.(0개 이상)
* 순서는 항상 `캐릭터 필터 → 토크나이저 → 토큰 필터`이며, 각 단계는 앞 단계의 출력을 입력으로 받는다.

#### `_analyze` API로 먼저 확인하기

* 애널라이저 설정은 인덱스를 만들지 않고도 `_analyze` API로 바로 테스트할 수 있다.
* 인덱스를 만들어놓고 결과를 확인하는 것보다 훨씬 빠르므로, 설정을 확정하기 전에 이 API로 검증하는 습관을 들이는 것이 좋다.

```shell
POST /_analyze
{
  "tokenizer": "standard",
  "filter": ["lowercase"],
  "text": "Apple 2025 맥북 에어"
}
```

#### ElasticSearch에 기본값으로 설정되어 있는 애널라이저(Analyzer)

* 별도 설정을 하지 않으면 `text` 타입 필드에는 **Standard Analyzer**가 적용된다.
* 구성은 `캐릭터 필터 없음 + standard 토크나이저 + lowercase 토큰 필터`이다.

#### Standard Analyzer

* 아래 케이스에서 `Apple`이 `apple`로 바뀐 것은 `lowercase` 토큰 필터가 적용되었기 때문이다.
* 토큰 타입(`<ALPHANUM>`, `<NUM>`, `<HANGUL>`)은 토크나이저가 문자 종류를 구분해 붙여준 값이다.

```shell
$ GET /_analyze
{
    "text" : "Apple 2025 맥북 에어",
    "analyzer" : "standard"
}

// Result

{
  "tokens": [
    {
      "token": "apple",
      "start_offset": 0,
      "end_offset": 5,
      "type": "<ALPHANUM>",
      "position": 0
    },
    {
      "token": "2025",
      "start_offset": 6,
      "end_offset": 10,
      "type": "<NUM>",
      "position": 1
    },
    {
      "token": "맥북",
      "start_offset": 11,
      "end_offset": 13,
      "type": "<HANGUL>",
      "position": 2
    },
    {
      "token": "에어",
      "start_offset": 14,
      "end_offset": 16,
      "type": "<HANGUL>",
      "position": 3
    }
  ]
}
```

#### Custom Analyzer

* 기본 애널라이저로 부족할 때 캐릭터 필터, 토크나이저, 토큰 필터를 조합해 직접 정의할 수 있다.

```shell
PUT /products_custom
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_custom_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "char_filter": [
            "html_strip"
          ],
          "filter": [
            "lowercase",
            "asciifolding"
          ]
        }
      }
    }
  }
}
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e8d46baf-1d63-4746-8bab-8ad408919e26.png)

* **주의** : `analysis` 설정은 인덱스 생성 시점에만 지정할 수 있다. 이미 존재하는 인덱스에 `PUT`을 다시 보내면 `resource_already_exists_exception`이 발생한다.
* 기존 인덱스의 애널라이저를 바꾸려면 인덱스를 닫고(`_close`) 설정을 변경하거나, 새 인덱스를 만들어 `_reindex` 해야 한다.
* 정의한 애널라이저는 다음과 같이 확인한다.

```shell
POST /products_custom/_analyze
{
  "analyzer": "my_custom_analyzer",
  "text": "<p>Apple 맥북</p>"
}
```

#### HTML strip character filter

* HTML 태그를 제거한 뒤 토크나이저로 넘긴다.
* `<p>Apple <b>맥북</b></p>` → `Apple 맥북`
* 토크나이저가 `keyword`이므로 문자열 전체가 토큰 하나로 남는다는 점에 유의한다.

```shell
PUT /html_strip_test
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "keyword",
          "char_filter": [
            "html_strip"
          ]
        }
      }
    }
  }
}
```

#### STOP(불용어) Token Filter

* 검색에 의미가 없는 단어를 제거한다.
* **필터를 정의하는 것만으로는 아무 일도 일어나지 않는다.** 반드시 애널라이저의 `filter` 배열에 등록해야 적용된다.

```shell
PUT /stop_test
{
  "settings": {
    "analysis": {
      "filter": {
        "my_stop_filter": {
          "type": "stop",
          "stopwords": [
            "in",
            "the",
            "days"
          ]
        }
      },
      "analyzer": {
        "my_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": [
            "lowercase",
            "my_stop_filter"
          ]
        }
      }
    }
  }
}
```

#### Stemmer(어간 추출) Token Filter

* 시제나 활용형이 달라도 같은 어간으로 정규화해 검색되도록 한다.
* `running runs` → `run`, `run`
* **한글에는 동작하지 않는다.** 기본 `stemmer`는 영어(`english`)를 대상으로 하므로 `달리기`, `달렸다`는 그대로 남는다.
* 한글 활용형 처리는 아래 형태소 분석기 항목을 참고한다.

```shell
PUT /stemmer_test
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "tokenizer": "whitespace",
          "filter": [ "stemmer" ]
        }
      }
    }
  }
}
```

#### Synonym(동의어) Token Filter

* 서로 다른 단어를 같은 의미로 묶어 검색되도록 한다.
* `synonym`은 **내장 필터 이름이 아니다.** `filter` 배열에 `"synonym"`이라고만 적으면 다음 오류가 발생한다.
* 반드시 `filter` 블록에서 `type: synonym`으로 정의하고 동의어 목록을 지정한 뒤, 그 이름을 애널라이저에 등록해야 한다.
* `맥북 노트북` → `맥북`, `macbook`, `노트북`, `랩탑`
* 동의어가 많아지면 `synonyms` 대신 `synonyms_path`로 파일을 참조한다.

```shell
PUT /synonym_test
{
  "settings": {
    "analysis": {
      "filter": {
        "my_synonym_filter": {
          "type": "synonym",
          "synonyms": [
            "맥북, macbook",
            "노트북, 랩탑"
          ]
        }
      },
      "analyzer": {
        "my_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": [
            "lowercase",
            "my_synonym_filter"
          ]
        }
      }
    }
  }
}
```

#### 색인 시점 vs 검색 시점 애널라이저

* 애널라이저는 **색인할 때**와 **검색할 때** 각각 적용되며, 기본적으로 같은 것이 쓰인다.
* 둘을 다르게 지정하려면 매핑에서 `analyzer`와 `search_analyzer`를 나눠 설정한다.
* 동의어 필터는 보통 **검색 시점에만** 적용한다. 색인 시점에 적용하면 동의어 목록이 바뀔 때마다 전체 문서를 다시 색인해야 하기 때문이다.

```shell
PUT /products_search
{
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "analyzer": "standard",
        "search_analyzer": "my_analyzer"
      }
    }
  }
}
```

#### 한글 검색의 한계와 형태소 분석기

* Standard Analyzer는 공백 기준으로만 한글을 나누므로, **붙여 쓴 단어를 쪼개지 못한다.**
* 결과는 `맥북에어` 토큰 하나뿐이다. 따라서 `맥북`으로 검색해도 이 문서는 **검색되지 않는다.**
* 한글을 제대로 다루려면 형태소 분석기인 **nori** 플러그인이 필요하다.

```shell
POST /_analyze
{
  "analyzer": "standard",
  "text": "맥북에어"
}
```
