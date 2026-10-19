# ElasticSearch - Tips for Improving Search Results

#### Proximity Search란?

* proximity search는 **특정 단어들이 지정한 거리 안에서 서로 가까이 있는 문서를 찾는** 검색이다.
* **`match_phrase` Query** — 지정한 필드에 **단어들이 정확한 순서 그대로** 담긴 문서를 찾는다.
* **`slop` Parameter** — **구문 안의 term 사이에 단어가 몇 개까지 끼어들어도 매치로 인정할지** 정한다. **`slop`이 0이면 구문이 정확히 일치**해야 하고, **값이 커질수록 검색어 사이에 더 많은 단어**를 허용한다.

#### 질의 예시

```shell
GET /library/_search
{
  "query": {
    "match_phrase": {
      "content": {
        "query": "quick fox",
        "slop": 2
      }
    }
  }
}
```

#### Fuzzy Match Query란?

* fuzzy match query는 **정확히 일치하는 것이 아니라 근사적으로 일치하는 문서를 찾을 때** 쓴다.
* **오타와 철자 변형을 다루는 데** 유용하다.
* **`fuzziness`** — **검색어와 문서의 term 사이에 허용할 편집 거리(변경 횟수)** 를 정한다.
* **`prefix_length`** — **fuzziness를 적용하기 전에 정확히 일치해야 하는 앞쪽 글자 수**를 지정한다. **오탐(false positive)을 줄이는 데** 도움이 된다.

#### Fuzziness Levels

* **`fuzziness` 0** — fuzziness 없음. 정확히 일치해야 한다.
* **`fuzziness` 1** — **한 번의 편집**을 허용한다. (글자 치환, 삽입, 삭제)
* **`fuzziness` 2** — **두 번의 편집**을 허용한다.
* **`fuzziness` "AUTO"** — **검색어의 길이에 따라 fuzziness를 자동으로 조정**한다.

#### 편집 거리란?

```
검색어: "elasticsearch"

치환  elasticsaerch   →  a를 e로 바꾼다(편집 1회)
삭제  elasticsearh    →  c를 넣는다(편집 1회)
삽입  elasticssearch  →  s를 뺀다(편집 1회)
전치  elasticsaerch   →  ae를 ea로 맞바꾼다(편집 1회)
```
* 검색어를 문서의 term으로 **바꾸는 데 필요한 최소 편집 횟수**가 편집 거리다.
* 치환·삽입·삭제에 더해 **인접한 두 글자의 자리바꿈(전치)** 도 편집 1회로 센다.

**AUTO의 기준**

```
글자 수 0 ~ 2   →  fuzziness 0(오타 허용 안 함)
글자 수 3 ~ 5   →  fuzziness 1
글자 수 6 이상   →  fuzziness 2
```

* 짧은 단어에 오타를 허용하면 **전혀 다른 단어까지 걸리기 때문**이다. `cat`에 편집 2를 허용하면 `dog`만 빼고 거의 다 걸린다.

#### 질의 예시

```shell
GET /library/_search
{
  "query": {
    "match": {
      "title": {
        "query": "elasticsaerch",
        "fuzziness": "AUTO",
        "prefix_length": 2
      }
    }
  }
}
```

* `prefix_length: 2`이면 **앞 두 글자 `el`은 반드시 맞아야** 하고, 그 뒤부터 fuzziness가 적용된다.

#### Synonym Search란?

* synonym search는 **검색할 때 동등하게 취급할 단어나 구문을 정의**할 수 있게 해준다.
* 예: **`laptop`과 `notebook`을 같은 단어로 취급**한다.
* **Define a Synonym File or Inline Synonyms** — 동의어로 다룰 **단어 목록을 만든다.**
* **Create a Custom Analyzer** — **synonym 필터를 가진 custom analyzer**를 정의해 원하는 필드에 동의어를 적용한다.
* **Apply the Custom Analyzer to a Field** — synonym 검색을 쓸 **필드의 매핑을 갱신**해 그 analyzer를 적용한다.

```shell
PUT /products
{
  "settings": {
    "analysis": {
      "filter": {
        "my_synonyms": {
          "type": "synonym",
          "synonyms": [
            "laptop, notebook",
            "휴대폰, 핸드폰, 스마트폰"
          ]
        }
      },
      "analyzer": {
        "synonym_analyzer": {
          "tokenizer": "standard",
          "filter": ["lowercase", "my_synonyms"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "analyzer": "synonym_analyzer"
      }
    }
  }
}
```
