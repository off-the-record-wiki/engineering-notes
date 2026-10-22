# Monitoring - Log Collection with ELK Stack

#### ELK

* 여러 서버에 흩어진 로그를 하나의 거대한 중앙 저장소로 모아두는 기술을 말한다.
* ELK 스택이란, 방대한 로그 데이터를 실시간으로 수집, 검색, 분석 및 시각화하는 오픈소스 플랫폼을 말한다.
* **E(Elasticsearch)** : 수많은 로그를 저장하고, 빛의 속도로 검색할 수 있게 지원해준다.
* **L(Logstash)** : 각 서버에서 로그를 수집, 변환하여 Elasticsearch로 전달하는 도구이다.
* **K(Kibana)** : Elasticsearch에 쌓인 데이터를 시각화하여 그래프와 대시보드로 보여주는 분석 도구다.

#### Logstash

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ca5725bb-58b0-4280-ae36-89ca2901f495.png)

* L(Logstash) : 각 서버에서 로그를 수집, 변환하여 Elasticsearch로 전달하는 도구이다.
* Logstash는 세 가지 단계(입력, 필터, 출력 구조)를 통해 움직인다.
* Input(입력) : `.log` 파일을 한 줄씩 실시간으로 읽어들인다.
* Filter(필터) : 로그를 그냥 통째로 텍스트로 넣지 않고 가공한다. 시간, 로그 레벨, Trace ID등 정보에 라벨링을 해서 구분한다.
* Output(출력) : Logstash의 최종 목적지인 Elasticsearch로 데이터를 보낸다.

> **로그 파일을 저장할 때 DB를 사용하면 안되는 이유**
> * 로그는 하루에도 수천만 건씩 쌓이는 엄청난 양의 텍스트 데이터이다.
> * MySQL과 같은 관계형 DB는 이런 방대한 텍스트에서 특정 단어를 검색하는 게 매우 오래 걸린다. 따라서 ES와 같은 검색 엔진을 사용하는 것이 더 적절하다.

> **로그 파일을 저장할 때 Elasticsearch가 적절한 이유**
> * 관계형 DB의 경우 특정단어를 찾기 위해 첫 페이지부터 끝까지 넘기면서 해당 단어를 찾는데 ES의 경우 Index를 활용하여 데이터를 아주 빠르게 찾는다.
> * 이 개념을 Inverted Index(역인덱스)라고 부른다.

#### Logback + Logstash + Elasticsearch 아키텍처 구성

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/17b74604-1d34-46cc-8946-fdaf2fd74e47.png)

* ElasticSearch는 로그 한 줄을 각각의 Document로 만든다.
* 터미널에서 보는 로그 한 줄이 Logstash를 거치면 JSON 덩어리 하나로 변환되는데 이걸 ElasticSearch에서는 도큐먼트(Document)라고 부른다.
* 해당 도큐먼트를 모아서 날짜별 인덱스(Index)에 묶어서 보관한다.
* `logstash.conf` 파일에 있는 `index => "logs-%{+YYYY.MM.dd}"` 구문에 의해 해당 형식이 인덱스가 된다.
