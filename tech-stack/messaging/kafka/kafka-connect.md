# Apache Kafka - Kafka Connect

#### Kafka Connect

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/2499f705-2f73-40ee-93a9-a1a635a5dc9d.png)

* 카프카 커넥트는 카프카 오픈소스에 포함된 툴 중 하나로 데이터 파이프라인 생성 시 반복 작업을 줄이고 효율적인 전송을 이루기 위한 애플리케이션이다.
* 커넥트는 특정 작업 형태를 템플릿으로 만들어놓은 커넥터를 실행함으로써 반복 작업을 줄일 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/1fe30c22-5768-498c-aa47-019c8b0ebe8e.png)

* 사용자가 커넥트에 커넥터 생성 명령을 내리면 커넥트는 내부에 커넥터와 태스크를 생성한다.
* 커넥터는 내부에서 태스크들을 관리한다. 태스크는 커넥터에 종속된 개념으로 실질적인 데이터 처리를 한다.
* 그렇기 때문에 데이터 처리를 정상적으로 하는지 확인하기 위해서는 각 태스크 상태를 확인해야 한다.

#### 커넥터(Connector)

* 커넥터는 프로듀서 역할을 하는 소스 커넥터와 컨슈머 역할을 하는 싱크 커넥터가 있다.
* MySQL, S3, MongoDB 등과 같은 저장소가 대표적인 싱크 애플리케이션, 소스 애플리케이션이 될 수 있다.
* 즉, MySQL에서 카프카로 데이터를 보낼 때 그리고 카프카에서 데이터를 MySQL로 저장할 때 JDBC 커넥터를 사용해 파이프라인을 만들 수 있다.

> [오픈소스 커넥터 리스트](https://github.com/aiven/aiven-docs/blob/main/docs/products/kafka/kafka-connect/concepts/list-of-connector-plugins.md)

#### 컨버터(Converter), 트랜스폼(Transform)

* 사용자가 커넥터를 사용해 파이프라인을 생성할 때, 컨버터와 트랜스폼 기능을 옵션으로 추가할 수 있다.
* 커넥터를 운영할 때 반드시 필요한 설정은 아니지만 데이터 처리를 더욱 풍부하게 도와주는 역할을 한다.
  * Converter : 데이터 처리 전 스키마를 변경하도록 하는 역할
  * Transform : 데이터 처리 시 각 메시지 단위로 데이터를 간단하게 변환하기 위한 용도로 사용

#### 커넥트 배포 및 운영

* **단일 모드 커넥트(standalone mode kafka connect)**
* **분산 모드 커넥트(distributed mode kafka connect)**

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/dd3439a6-36d2-483a-be30-c5a2fe4b120d.png)

* 단일 모드 커넥트는 1개 프로세스만 실행된다는 점이 특징인데, 단일 프로세스로 실행되기 때문에 고가용성 구성이 되지 않아 단일 장애점(SPOF)이 될 수 있다. 그러므로 단일 모드 커넥트 파이프라인은 주로 개발환경이나 중요도가 낮은 파이프라인을 운영할 때 사용한다.
* 분산 모드 커넥트는 2대 이상의 서버에서 클러스터 형태로 운영함으로써 단일 모드 커넥트 대비 안전하게 운영할 수 있다는 장점이 있다. 2개 이상의 커넥트가 클러스터로 묶이면 1개의 커넥트가 이슈 발생으로 중단되더라도 남은 1개의 커넥트가 파이프라인을 지속적으로 처리할 수 있다. 
* 분산 모드 커넥트는 데이터 처리량 변화도 유연하게 대응할 수 있다. 커넥트가 실행되는 서버 개수를 늘림으로써 무중단으로 스케일 아웃하여 처리량을 늘릴 수 있다.
* 이런 장점이 있기 때문에 상용 환경에서는 커넥트를 운영한다면 분산 모드 커넥트를 2대 이상으로 구성하고 설정하는 것이 좋다.

#### 커넥트 REST API 인터페이스

* REST API를 사용하면 현재 실행 중인 커넥트의 플러그인 종류, 태스크 상태, 커넥터 상태 등을 조회할 수 있다.
* 커넥트는 8083 포트로 호출할 수 있으며 HTTP 메서드 기반 API를 제공한다.

| 요청 메서드 | 호출 경로 | 설명 |
| --- | --- | --- |
| `GET` | `/` | 실행 중인 커넥트 정보 확인 |
| `GET` | `/connectors` | 실행 중인 커넥터 이름 확인 |
| `POST` | `/connectors` | 새로운 커넥터 생성 요청 |
| `GET` | `/connectors/{커넥터 이름}` | 실행 중인 커넥터 정보 확인 |
| `GET` | `/connectors/{커넥터 이름}/config` | 실행 중인 커넥터의 설정값 확인 |
| `PUT` | `/connectors/{커넥터 이름}/config` | 실행 중인 커넥터 설정값 변경 요청 |
| `GET` | `/connectors/{커넥터 이름}/status` | 실행 중인 커넥터 상태 확인 |
| `POST` | `/connectors/{커넥터 이름}/restart` | 실행 중인 커넥터 재시작 요청 |

#### 단일 모드 커넥트 설정

* 단일 모드 커넥트를 실행하기 위해서는 단일 모드 커넥트를 참조하는 설정 파일인 `connect-standalone.properties` 파일을 수정해야 한다.
```properties
bootstrap.servers=my-kafka:9092
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=false
value.converter.schemas.enable=false
offset.storage.file.filename=/tmp/connect.offsets
offset.flush.interval.ms=10000
plugin.path=/usr/local/share/java,/usr/local/share/kafka/plugins
```
* 단일 모드 커넥트를 실행 시에 파라미터로 커넥트 설정파일과 커넥터 설정파일을 차례로 넣어 실행하면 된다.
```properties
name=local-file-source
connector.class=FileStreamSource
tasks.max=1
file=/tmp/test.txt
topic=connect-test
```

#### 분산 모드 커넥트

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/9bf69cbb-f186-4a9e-af92-5d827657507c.png)

* 분산 모드 커넥트는 단일 모드 커넥트와 다르게 2개 이상의 프로세스가 1개의 그룹으로 묶여서 운영된다.
* 이를 통해 1개의 커넥트 프로세스에 이슈가 발생하여 종료되더라도 살아있는 나머지 1개 커넥트 프로세스가 커넥터를 이어받아서 파이프라인을 지속적으로 실행할 수 있다는 특징이다. 이제 분산 모드 커넥트를 묶어서 운영하기 위해 어떤 설정을 해야 하는지 분산 모드 설정 파일인 `connect-distributed.properties`를 살펴보면 된다.

```properties
bootstrap.servers=my-kafka:9092
group.id=connect-cluster
key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
key.converter.schemas.enable=false
value.converter.schemas.enable=false
offset.storage.topic=connect-offsets
offset.storage.replication.factor=1
config.storage.topic=connect-configs
config.storage.replication.factor=1
status.storage.topic=connect-status
status.storage.replication.factor=1
offset.flush.interval.ms=10000
plugin.path=/usr/local/share/java,/usr/local/share/kafka/plugins
```

#### 커스텀 소스 커넥터

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/9d6b701b-f8ed-41ec-b8ae-5848f9d38288.png)

* 소스 커넥터는 소스 애플리케이션 또는 소스 파일로부터 데이터를 가져와 토픽으로 넣는 역할을 한다. 오픈소스 소스 커넥터를 사용해도 되지만 라이선스 문제나 로직이 원하는 요구사항에 부합하지 않아 직접 개발해야 하는 경우도 있는데 이 떄는 카프카 커넥트 라이브러리에서 제공하는 `SourceConnector`와 `SourceTask` 클래스를 사용해 직접 소스 커넥터를 구현하면 된다. 직접 구현한 소스 커넥터를 빌드하여 jar 파일로 만들고 커넥트를 실행 시 플러그인으로 추가하여 사용할 수도 있다.

```gradle
dependencies {
    compileOnly 'org.apache.kafka:connect-api:2.5.0'
}
```

* 소스 커넥터를 만들 때는 connect-api 라이브러리를 추가해야 한다. connect-api 라이브러리에는 커넥터를 개발하기 위한 클래스들이 포함되어 있다.
* `SourceConnector` : 태스크를 실행하기 전 커넥터 설정파일을 초기화하고 어떤 태스크 클래스를 사용할 것인지 정의하는 데에 사용한다. 그렇기 때문에 `SourceConnector`에는 실질적인 데이터를 다루는 부분이 들어가지 않는다. `SourceTask`가 실제로 데이터를 다루는 클래스다.

> docs : SourceConnectors implement the connector interface to pull data from another system and send it to Kafka.

* `SourceTask` : 소스 애플리케이션 또는 소스 파일로부터 데이터를 가져와서 토픽으로 데이터를 서빙하는 역할을 수행한다. `SourceTask`만의 특징은 토픽에서 사용하는 오프셋이 아닌 자체적으로 사용하는 오프셋을 사용한다. `SourceTask`에서 사용하는 오프셋은 소스 애플리케이션 또는 소스 파일을 어디까지 읽었는지를 저장하는 역할을 한다. 이 오프셋을 통해 데이터를 중복해서 토픽으로 보내는 것을 방지할 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e2a4f2ad-5e58-40c0-9182-f376bdbef02b.png)

#### 커넥터 옵션값 설정시 중요도(Importance) 지정 기준

* 커넥터를 개발할 때 옵션값의 중요도를 `Importance` ENUM 클래스로 지정할 수 있다.
* `Importance` ENUM 클래스는 HIGH, MEDIUM, LOW 3가지 종류로 나뉘어 있다.
* 결론부터 말하자면 옵션의 `Importance`를 HIGH, MEDIUM, LOW로 정하는 명확한 기준은 없다. 단지 사용자로 하여금 이 옵션이 중요하다는 것을 명시적으로 표시하기 위한 문서로 활용할 뿐이다. 그러므로 커넥터에서 반드시 사용자가 입력한 설정이 필요한 값은 HIGH, 사용자의 입력값이 없더라도 상관없고 기본값이 있는 옵션을 MEDIUM, 사용자의 입력값이 없어도 되는 옵션을 LOW 정도로 구분해 지정하면 된다.

#### 커스텀 싱크 커넥터

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/5b7dce51-1d5a-434e-9c56-59bdfcdffec6.png)

* 싱크 커넥터는 토픽의 데이터를 타깃 애플리케잉션 또는 타깃 파일로 저장하는 역할을 한다.
* 카프카 커넥트 라이브러리에서 제공하는 `SinkConnector`와 `SinkTask` 클래스를 사용하면 직접 싱크 커넥터를 구현할 수 있다. 직접 구현한 싱크 커넥트는 빌드하여 jar로 만들고 커넥트의 플러그인으로 추가해 사용할 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/37ad83ec-29c2-4776-8fab-4044aaa36481.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/87135707-7112-441e-a20f-1d4afe8193b0.png)
