# Redis - Redis Server

#### master/slave

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4fec828e-b5db-4285-92bd-56b43eab6324.png)

* Redis DB 기본 구성
* 하나의 마스터 서버가 쓰기 작업(Write)을 처리하고, 여러 슬레이브 서버가 마스터의 데이터를 복제하여 읽기 작업(Read)을 처리한다.

#### cluster

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/2c478431-cb73-46d0-a37c-4c5e641859fc.png)

* 추가적인 구성과 설정을 필요로 한다.
* Redis 클러스터는 데이터를 여러 노드에 분산 저장 즉, 샤딩을 지원함으로써 고가용성과 확장성을 제공한다.
* 클러스터는 자동으로 데이터 샤딩을 수행하고, 노드 간의 복제를 통해 장애 복구를 지원한다.
