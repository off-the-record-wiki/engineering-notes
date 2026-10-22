# ElasticSearch - Elasticsearch Core Concepts

#### ElasticSearch란?

* ElasticSearch는 오픈 소스 분산, Restful 검색 및 분석 엔진, 확장 가능한 데이터 저장소 및 벡터 데이터베이스를 말한다.
* 쉽게 말해 ElasticSearch는 검색, 데이터 분석에 최적화된 데이터베이스이다.
* 내부적으로는 루씬(Lucene)을 기반으로 하며, 문서를 역인덱스(Inverted Index)로 저장해 대용량 데이터에서도 빠른 검색이 가능하다.
> [참고](https://www.elastic.co/docs/solutions/search)

#### ElasticSearch 주요 활용 사례

* 데이터 수집 및 분석
  * ElasticSearch는 대규모 데이터를 수집 및 분석하는 데 최적화되어 있다.
  * 주로 ElasticSearch(데이터 저장), Logstash(데이터 수집 및 가공), Kibana(데이터 시각화)를 같이 활용해 데이터를 수집 및 분석한다.(ELK Stack)
* 검색 최적화
  * ElasticSearch는 데이터가 많더라도 뛰어난 검색 속도를 가지고 있고, 오타나 동의어를 고려해서 유연하게 검색할 수 있는 기능을 가지고 있다.

#### Docker로 ElasticSearch, Kibana 사용하기

* Kibana는 ElasticSearch에 저장된 데이터를 조회/시각화할 수 있는 GUI 툴이다.
* `docker-compose.yml`에 ElasticSearch와 Kibana를 함께 정의하면 두 서비스를 한 번에 띄울 수 있다.

```yml
services:
  elastic:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.17.4 # Elastic 8.17.4 버전의 이미지
    ports:
      - 9200:9200 # 9200번 포트에서 Elasticsearch 실행
    environment:
      ### 아래 설정은 개발/테스트 환경에서 간단하게 테스트하기 위한 옵션 (운영 환경에서는 설정하면 안 됨)
      - discovery.type=single-node # 단일 노드 (지금은 알 필요 없음)
      - xpack.security.enabled=false # 보안 설정
      - xpack.security.http.ssl.enabled=false # 보안 설정
  kibana:
    image: docker.elastic.co/kibana/kibana:8.17.4 # 8.17.4 버전
    ports:
      - 5601:5601 # 5601번 포트에서 kibana 실행
    environment:
      - ELASTICSEARCH_HOSTS=http://elastic:9200 # kibana가 통신할 Elasticsearch 주소 알려주기
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c2f2a912-7d85-4a82-b528-605d86a4a9c9.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f741a192-6eaf-4d89-92da-630d02373169.png)

> * ❗MySQL과 통신하려면 SQL을 사용하듯이 ElasticSearch와 통신하려면 REST API를 사용한다.

#### ElasticSearch의 기본 용어 정리

* 데이터를 저장하기 위해 인덱스(Index)를 만든다.
* 인덱스를 만들 때 어떤 유형의 데이터를 넣을지 매핑(Mapping)을 정의한다.
* 필드(Field)에 맞게끔 데이터를 저장한다.

|MySQL|ElasticSearch|
|:----:|:----:|
|테이블(Table)|인덱스(Index)|
|컬럼(Column)|필드(Field)|
|레코드(Record), 로우(Row)|도큐먼트(Document)|
|스키마(Schema)|매핑(Mapping)|
 
#### 인덱스 생성 / 매핑 정의 / 도큐먼트 삽입

```shell
# 인덱스 생성
# PUT /{인덱스명}
PUT /users
 
# 인덱스 생성됐는 지 확인하기
# GET /{인덱스명}
GET /users
 
# 없는 인덱스를 조회했을 때 응답값 확인하기
GET /abc
```

#### 인덱스 삭제
 
```shell
# 인덱스 생성
PUT /boards
 
# 인덱스 생성됐는 지 확인하기
GET /boards
 
# 인덱스 삭제
# DELETE /{인덱스명}
DELETE /boards
 
# 삭제됐는 지 확인하기
GET /boards
```

#### 매핑 정의
 
```shell
# 매핑 정의하기
# PUT /{인덱스명}/_mapping
PUT /users/_mapping
{
  "properties": {
    "name": { "type": "keyword" },
    "age": { "type": "integer" },
    "is_active": { "type": "boolean" }
  }
}
 
# 정의한 매핑 확인하기
GET /users
```

* 매핑(Mapping)은 MySQL에서 테이블 스키마를 정의하는 것과 같다.

#### 도큐먼트 삽입 및 조회

```
# 도큐먼트 삽입
# POST /{인덱스명}/_doc
POST /users/_doc
{
  "name": "Alice",
  "age": 28,
  "is_active": true
}
 
POST /users/_doc
{
  "name": "Bob",
  "age": 30,
  "is_active": false
}
 
# 모든 도큐먼트 조회
# GET /{인덱스명}/_search
GET /users/_search
```
> 도큐먼트를 삽입하는 것은 MySQL에서 테이블의 레코드를 삽입하는 것과 같다.
 
```shell
// 도큐먼트 조회 시 응답값
{
  "took": 1,
  "timed_out": false,
  "_shards": {
    "total": 1,
    "successful": 1,
    "skipped": 0,
    "failed": 0
  },
  "hits": {
    "total": {
      "value": 2,
      "relation": "eq"
    },
    "max_score": 1,
    "hits": [
      {
        # _index : 인덱스명
        "_index": "users",
        
        # 데이터를 저장하면서 자동으로 생성된 '랜덤 고유 ID'
        "_id": "0imoJ5YBVwYHNUofQSQc",
        
        "_score": 1,
 
        # _source : 저장한 데이터가 들어있는 필드
        "_source": {
          "name": "Alice",
          "age": 28,
          "is_active": true
        }
      },
      {
        "_index": "users",
        "_id": "0ymoJ5YBVwYHNUofRSRX",
        "_score": 1,
        "_source": {
          "name": "Bob",
          "age": 30,
          "is_active": false
        }
      }
    ]
  }
}
```

#### 도큐먼트 저장
 
* id를 자동으로 생성해서 저장하는 방법
* id를 직접 지정해서 저장하는 방법
* id를 직접 지정해서 저장하는 방법(이미 id가 존재한다면 데이터를 덮어 씌움)
```shell
# 도큐먼트 저장 (id 자동 생성)
# POST /{인덱스명}/_doc
POST /users/_doc
{
  "name": "Jaeseong",
  "age": 20,
  "is_active": true
}
 
# 잘 저장됐는 지 확인하기
GET /users/_search 
 
# 도큐먼트 저장 (id 직접 지정)
# POST /{인덱스명}/_create/{id}
POST /users/_create/1
{
  "name": "jscode",
  "age": 30,
  "is_active": true
}
 
# 잘 저장됐는 지 확인하기
GET /users/_search 
 
# 이미 존재하는 id로 데이터 저장하면 어떻게 되는 지 확인하기
POST /users/_create/1
{
  "name": "jscode",
  "age": 30,
  "is_active": true
}
 
# 도큐먼트 저장 및 업데이트
# PUT /{인덱스명}/_doc/{id}
PUT /users/_doc/2
{
  "name": "jason",
  "age": 30,
  "is_active": true
}
 
# 잘 저장됐는 지 확인하기
GET /users/_search 
 
# 이미 존재하는 id로 데이터 저장하면 어떻게 되는 지 확인하기
POST /users/_doc/2
{
  "name": "jason2", 
  "age": 30,
  "is_active": true
}
 
# 어떻게 됐는 지 확인하기
GET /users/_search 
```
> * ❗`_create`는 이미 같은 id의 도큐먼트가 존재하면 `409 Conflict` 에러를 반환하며 저장에 실패한다.
> * ❗`_doc`(PUT)은 같은 id로 다시 요청하면 기존 도큐먼트 전체를 새 내용으로 완전히 덮어쓴다. 즉 요청 본문에 없는 필드는 사라진다. (신규 저장 + 수정 겸용)

#### 도큐먼트 조회
 
* 특정 인덱스의 모든 도큐먼트 조회
* id로 특정 도큐먼트 조회
```shell
# GET /{인덱스명}/_search
GET /users/_search
 
# 특정 도큐먼트 조회
# GET /{index}/_doc/{id}
GET /users/_doc/1
GET /users/_doc/2
```

#### 도큐먼트 수정
 
* 도큐먼트를 통째로 덮어씌우기
* 일부 필드만 수정
```shell
# 기존 도큐먼트 확인
GET /users/_doc/1
 
# 특정 도큐먼트 수정
# 이 API는 데이터 저장 시에도 사용하는 API이다. (바로 위에서 언급했었음)
# PUT /{인덱스명}/_doc/{id}
PUT /users/_doc/1
{
  "name": "new"
}
 
# 수정됐는 지 확인하기
GET /users/_doc/1
 
# 기존 도큐먼트 확인
GET /users/_doc/2
 
# 특정 도큐먼트 수정
# POST /{인덱스명}/_update/{id}
POST /users/_update/2
{
  "doc": {
    "age": 10,
    "is_active": false
  }
}
 
# 수정됐는 지 확인하기
GET /users/_doc/2
```
> * ❗위의 `PUT /_doc/{id}`는 도큐먼트 전체를 덮어써서 `name`처럼 요청에 없는 필드는 사라지지만, `POST /_update/{id}`는 `doc`에 넘긴 필드만 병합(merge)해서 수정하고 나머지 필드는 그대로 유지한다.

#### 도큐먼트 삭제
 
```shell
# 기존 도큐먼트 확인
GET /users/_doc/2
 
# id로 도큐먼트 삭제하기
# DELETE /{인덱스명}/_doc/{id} 
DELETE /users/_doc/2
 
# 삭제됐는지 확인하기
GET /users/_doc/2
```

#### Bulk API로 여러 도큐먼트 한 번에 처리하기

* 도큐먼트를 한 건씩 저장/삭제하면 요청 수가 많아져 성능이 떨어진다.
* `_bulk` API를 사용하면 여러 건의 색인/수정/삭제 작업을 한 번의 요청으로 처리할 수 있다.
* 대량의 데이터를 수집·적재할 때(위에서 언급한 '데이터 수집 및 분석' 활용 사례) 실제로 가장 많이 쓰이는 방식이다.

```shell
# POST /{인덱스명}/_bulk
# 짝수 줄: 어떤 작업(index/create/update/delete)을 어떤 id에 수행할지 지정
# 홀수 줄: 실제 도큐먼트 내용 (delete는 생략)
POST /users/_bulk
{ "index": { "_id": "10" } }
{ "name": "Charlie", "age": 25, "is_active": true }
{ "index": { "_id": "11" } }
{ "name": "David", "age": 40, "is_active": false }
{ "delete": { "_id": "10" } }
 
# 잘 반영됐는 지 확인하기
GET /users/_search
```
> * ❗각 줄은 반드시 개행(\n)으로 구분된 한 줄짜리 JSON이어야 하며, 마지막 줄도 개행으로 끝나야 한다.
> * ❗[Document API](https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/java-docs.html)
