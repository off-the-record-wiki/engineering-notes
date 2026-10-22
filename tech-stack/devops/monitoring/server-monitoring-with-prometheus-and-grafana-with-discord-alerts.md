# Monitoring - Server Monitoring with Prometheus and Grafana with Discord Alerts

#### [PromQL 프로메테우스 쿼리 언어](https://github.com/influxdata/promql)

* PromQL이란, Prometheus Query Language의 약자로 프로메테우스에서 데이터를 꺼낼 때 쓰는 전용 언어이다.
* PromQL은 직관적인 것이 특징으로 무엇을(Metric), 어떤 조건으로(Label) 찾을지 적어주면 된다.
```text
http_server_requests_seconds_count{status="400"}
```
* Metric : http_server_requests_seconds_count(찾고 싶은 데이터의 한 종류)
* Label : status=”400”(중괄호 안에 적는 조건문으로 필터링을 한다)

#### ELK & Grafana & Prometheus 연결

```yml
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:9.1.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - xpack.security.http.ssl.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"

  logstash:
    image: docker.elastic.co/logstash/logstash:9.1.0
    container_name: logstash
    volumes:
      - ./logs:/usr/share/logstash/logs
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    ports:
      - "5044:5044"
    depends_on:
      - elasticsearch

  kibana:
    image: docker.elastic.co/kibana/kibana:9.1.0
    container_name: kibana
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch

  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    depends_on:
      - prometheus
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7af35f31-bcd9-467f-9925-f6160d340d27.png)
