# ElasticSearch - Korean Optimized Search

#### 한글 최적화 검색 기능을 개발하고 싶다면?

* 영어는 띄어쓰기로 단어가 명확하게 구분되다보니 `standard tokenizer`(공백 또는 `,`, `.`, `!`, `?`와 같은 문장 부호를 기준으로 자름)로도 잘 나눌 수 있다.
* 허나 한국어는 조사(-는, -를), 어미(-다, -해요)를 붙여서 쓰는 말이 많다. 그리고 띄어쓰기가 비교적 자유롭다. 그러다보니 `standard tokenizer`로 단어를 나눠보면 제대로 잘 나누지 못하는 문제가 생긴다.
* 이 문제를 해결하려면 한글에 맞는 전용 Analyzer인 Nori(노리) Analyzer를 써야 한다.

#### [Nori Analyzer](https://www.elastic.co/docs/reference/elasticsearch/plugins/analysis-nori)

* 한글 최적화 검색 기능을 개발하기 위해서는 별도의 플러그인이 필요하다.
* [참고](https://www.elastic.co/docs/reference/elasticsearch/plugins/analysis-nori)

```Dockerfile
FROM docker.elastic.co/elasticsearch/elasticsearch:8.17.4 

# Nori Analyzer 플러그인 설치
RUN bin/elasticsearch-plugin install analysis-nori 
```

```yml
services:
  elastic:
    build:
      context: .
      dockerfile: Dockerfile 
    ports:
      - 9200:9200 # 9200번 포트에서 Elasticsearch 실행
    environment:
      # 아래 설정은 개발/테스트 환경에서 간단하게 테스트하기 위한 옵션 (운영 환경에서는 설정하면 안 됨)
      - discovery.type=single-node # 단일 노드 (지금은 알 필요 없음)
      - xpack.security.enabled=false # 보안 설정
      - xpack.security.http.ssl.enabled=false # 보안 설정
  kibana:
    image: docker.elastic.co/kibana/kibana:8.17.4 # 8.17.4 버전
    ports:
      - 5601:5601 # 5601번 포트에서 kibana 실행
    environment:
      - ELASTICSEARCH_HOSTS=http://elastic:9200 # kibana에게 통신할 Elasticsearch 주소 알려주기
```

#### Nori Analyzer의 구성 요소

* Nori Analyzer도 다른 Analyzer와 마찬가지로 `character filter` → `tokenizer` → `token filter` 순서로 동작한다.
* **tokenizer : `nori_tokenizer`**
  * 형태소 분석 기반으로 문장을 의미 있는 단위(명사, 조사, 어미 등)로 쪼갠다.
  * `decompound_mode` 옵션으로 복합명사(둘 이상의 단어가 합쳐진 명사)를 어떻게 다룰지 결정한다.

|옵션|설명|
|:-----:|:-----|
|`none`|복합명사를 분리하지 않고 그대로 하나의 토큰으로 유지한다.|
|`discard`|복합명사를 구성 단어로 분리하고, 원본 복합명사 토큰은 버린다.|
|`mixed`|복합명사를 구성 단어로 분리하면서, 원본 복합명사 토큰도 함께 유지한다.(둘 다 검색 가능)|

* 검색 서비스에서는 대부분 `mixed`를 사용한다. 예를 들어 "삼성노트북"으로 검색해도, "노트북"으로 검색해도 모두 찾을 수 있어야 하기 때문이다.
* **token filter : `nori_part_of_speech`**
  * 형태소 분석 결과에서 검색에 불필요한 품사(조사, 어미, 접속사 등)를 걸러낸다.
  * 걸러낼 품사 목록은 [품사 태그표](https://www.elastic.co/docs/reference/elasticsearch/plugins/analysis-nori-tokenizer)에서 확인할 수 있다.
* **token filter : `nori_readingform`**
  * 한자로 된 단어를 한글 발음으로 변환한다. (Ex. 漢字 → 한자)

#### 인덱스 생성 시 Nori Analyzer 적용하기

* 커스텀 analyzer를 만들어서 인덱스의 `settings`에 등록하고, 실제 필드의 `mappings`에서 해당 analyzer를 사용하도록 지정하면 된다.

```shell
PUT /products
{
  "settings": {
    "analysis": {
      "tokenizer": {
        "nori_mixed_tokenizer": {
          "type": "nori_tokenizer",
          "decompound_mode": "mixed" # 복합명사 분리 + 원본 유지
        }
      },
      "analyzer": {
        "korean_analyzer": {
          "type": "custom",
          "tokenizer": "nori_mixed_tokenizer",
          "filter": ["nori_part_of_speech", "lowercase"] # lowercase로 영어도 대소문자 구분 없이 검색되게
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "product_name": {
        "type": "text",
        "analyzer": "korean_analyzer" # 이 필드는 색인/검색 시 korean_analyzer를 사용
      }
    }
  }
}
```

#### `_analyze` API로 토큰화 결과 비교해보기

* 인덱스를 만들지 않고도 `_analyze` API로 analyzer가 문장을 어떻게 쪼개는지 바로 확인할 수 있다.
* standard tokenizer와 nori tokenizer가 같은 문장을 어떻게 다르게 처리하는지 비교해보면 왜 Nori가 필요한지 체감할 수 있다.

```shell
# standard tokenizer -> 조사/어미가 단어에 그대로 붙어서 분리된다.
GET /_analyze
{
  "analyzer": "standard",
  "text": "삼성노트북을 검색해요"
}
# 결과 토큰 예시: [삼성노트북을, 검색해요]
```

```shell
# nori tokenizer(mixed) -> 형태소 단위로 분리되고, 복합명사는 원본과 구성 단어 모두 토큰으로 남는다.
GET /products/_analyze
{
  "analyzer": "korean_analyzer",
  "text": "삼성노트북을 검색해요"
}
# 결과 토큰 예시: [삼성노트북, 삼성, 노트북, 검색]
```

* `standard tokenizer`로 색인하면 "삼성노트북을"이라는 토큰으로만 검색이 가능하지만, nori tokenizer(mixed)로 색인하면 "삼성", "노트북", "삼성노트북" 중 어떤 키워드로 검색해도 해당 도큐먼트를 찾을 수 있다.

#### [한글과 영어가 혼용된 문장에서의 검색 기능 개발](https://www.elastic.co/docs/reference/elasticsearch/plugins/analysis-nori-analyzer)

* 한글(korean)과 영어(english)가 섞여있는 글이라면 Nori Analyzer를 활용하면 된다. 거기서 필드 값의 특징에 따라 character filter나 token filter를 추가해서 사용하면 된다.
* ElasticSearch에서 특정 필드에 사용할 분석기를 지정하기 위해 일반적으로 인덱스 생성 시 설정(Settings) 및 매핑(Mapping)에서 정의한다. (위 예시의 `korean_analyzer` 참고)
* nori_tokenizer는 한글 형태소만 분석 대상으로 삼기 때문에, 영어 단어는 공백을 기준으로 별도의 토큰으로 분리된다. 이때 영어 대소문자를 구분 없이 검색하고 싶다면 `lowercase` 토큰 필터를, 영어 단어의 오탈자까지 허용하고 싶다면 `match` 쿼리의 `fuzziness` 옵션을 함께 사용하면 된다.
