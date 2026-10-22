# Test - Load Testing Fundamentals

#### 부하 테스트란?

* 시스템이 어느 정도의 부하(트래픽, 요청)을 견딜 수 있는지 테스트하는 것을 말한다.

#### 처리량(Throughput), 지연 시간(Latency)

* `처리량(Throught)`이란 부하 테스트에서 서비스가 1초당 처리될 수 있는 트래픽 양을 말한다.
* 단위는 TPS(Transaction Per Seconds, 1초당 처리한 트랜잭션의 수)를 많이 활용한다.
* `지연 시간(Latency)`이란 요청에 대한 응답 시간을 의미한다.
* 만약 하나의 API로 요청을 보냈을 때 응답받기까지의 시간이 2.5초라면 Latency를 2.5초라고 할 수 있다.

#### 부하 테스트 실습

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/07cf3819-3436-470d-bd94-17ee34b53a2e.png)

* 부하 테스트 툴(k6)은 테스트하고자 하는 시스템과 반드시 독립적으로 분리해서 구성해야 한다. 왜냐하면 부하 테스트 툴 자체도 트래픽을 만들어내면서 컴퓨팅 리소스(CPU, 메모리 등)를 사용하기 때문이다.
