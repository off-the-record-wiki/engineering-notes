# ElasticSearch - Query Joins

#### RDBMS의 전통적인 Join

* **Parent - Child 관계**를 다룬다.
* **데이터를 정규화(normalize)** 하려 한다. 즉 **저장 공간을 최적화**하는 방향이다.

#### Elasticsearch의 Join Query

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/0cc51f81-eddc-44bf-b6eb-21fa8736b04d.png)

* 마찬가지로 **Parent - Child 관계**를 다룬다.
* Elasticsearch는 이 관계를 **`join` 필드 타입**으로 관리한다. 이 타입으로 **부모 문서와 자식 문서 사이의 관계를 정의**한다.
  * **Parent Document** — **주된 개체**에 해당하는 문서다. (예: author)
  * **Child Document** — **부모와 연관된** 문서다. (예: author의 book들)
* RDBMS와 달리 **같은 인덱스 안에** 부모 문서와 자식 문서가 함께 들어간다.

#### 매핑과 색인

* **매핑 — `join` 필드로 관계를 선언한다**

```shell
PUT /library
{
  "mappings": {
    "properties": {
      "my_join": {
        "type": "join",
        "relations": { "author": "book" }
      }
    }
  }
}
```

* **부모 색인**

```shell
PUT /library/_doc/1
{
  "name": "김작가",
  "my_join": { "name": "author" }
}
```

* **자식 색인 — `routing`이 필수다**

```shell
PUT /library/_doc/2?routing=1
{
  "title": "엘라스틱서치 입문",
  "my_join": { "name": "book", "parent": "1" }
}
```

#### 질의

* **`has_child` — 조건에 맞는 자식을 가진 부모를 찾는다**

```shell
GET /library/_search
{
  "query": {
    "has_child": {
      "type": "book",
      "query": { "match": { "title": "엘라스틱서치" } }
    }
  }
}
```

* **`has_parent` — 조건에 맞는 부모를 가진 자식을 찾는다**

```shell
GET /library/_search
{
  "query": {
    "has_parent": {
      "parent_type": "author",
      "query": { "match": { "name": "김작가" } }
    }
  }
}
```
* 둘 다 `inner_hits`를 붙이면 **어느 자식·부모가 걸렸는지** 함께 받을 수 있다.
