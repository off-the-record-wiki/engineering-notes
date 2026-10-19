# Test - Chaos Monkey

#### Chaos Monkey란?

* 카오스 엔지니어링 툴이란, 프로덕션 환경, 분산 시스템 환경에서 불확실성을 파악하고 해결 방안을 모색하는데 사용하는 툴이다.
* 운영 환경의 불확실성 예시는 다음과 같은 것들이 있을 수 있다.
  * 네트워크 지연
  * 서버 장애
  * 디스크 오작동
  * 메모리 누수
* **[Chaos Monkey Spring Boot](https://codecentric.github.io/chaos-monkey-spring-boot/latest/)**
  * 스프링 부트 애플리케이션에 Chaos Monkey를 손쉽게 적용해 볼 수 있는 툴
  * 즉, 스프링 부트 애플리케이션을 망가트릴 수 있는 툴이다.
* 공격 대상(Watcher) 정리 - `@RestController`, `@Controller`, `@Service`, `@Repository`, `@Component` ...
* 공격 유형(Assaults) 정리 - 응답 지연(Latency Assault), 예외 발생(Exception Assault), 애플리케이션 종료(AppKiller Assault), 메모리 누수(Memory Assault)

#### [Chaos Monkey](https://netflix.github.io/chaosmonkey/)

```xml
<!-- Source: https://mvnrepository.com/artifact/de.codecentric/chaos-monkey-spring-boot -->
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>chaos-monkey-spring-boot</artifactId>
    <version>4.0.0</version>
    <scope>compile</scope>   <!-- 테스트/로컬 환경에서만 사용 권장 -->
</dependency>
```

```properties
# 프로필 활성화 (application.yml 또는 application-chaos-monkey.yml)
spring.profiles.active=chaos-monkey
```
```properties
# Chaos Monkey 엔드포인트 설정, Chaos Monkey 기능 활성화
management.endpoint.chaosmonkey.enabled=true

# Actuator에서 노출할 엔드포인트
# health : 헬스 체크
# info : 애플리케이션 정보
# chaosmonkey : Chaos Monkey 제어(장애 주입, 통계 조회)
management.endpoints.web.exposure.include=health,info,chaosmonkey
```

#### Chaos Monkey에서 응답 지연 이슈를 재현하는 방법

* Repository Watcher 활성화
```properties
# Repository Watcher 활성화한다(DB 계층 모니터링)
# 다른 Watcher들 : controller, service, restTemplate, webClient 등 추가할 수 있다.
chaos.monkey.watcher.repository=true
```

* Chaos Monkey 활성화
```bash
# Chaos Monkey 활성화
http POST localhost:8080/actuator/chaosmonkey/enable

# 또는 curl 사용
curl -X POST http://localhost:8080/actuator/chaosmonkey/enable
```

* Chaos Monkey 활성화 확인
```bash
# Chaos Monkey 활성화 상태 확인
http localhost:8080/actuator/chaosmonkey/status

# 응답 예: {"chaosMonkeyEnabled": true}
```

* Chaos Money Watcher 확인
```bash
# 현재 활성화된 Watcher 확인
http localhost:8080/actuator/chaosmonkey/watchers

# 응답 예:
# {
#   "chaosMonkeyProperties": {
#     "watchers": {
#       "repository": true,
#       "controller": false,
#       "service": true
#     }
#   }
# }
```

* Chaos Monkey 지연 공격 설정
```bash
# 지연 공격 설정 (2~5초 지연)
http POST localhost:8080/actuator/chaosmonkey/assaults \
  level=3 \
  latencyRangeStart=2000 \
  latencyRangeEnd=5000 \
  latencyActive=true

# curl 버전
curl -X POST http://localhost:8080/actuator/chaosmonkey/assaults \
  -H "Content-Type: application/json" \
  -d '{
    "level": 3,
    "latencyRangeStart": 2000,
    "latencyRangeEnd": 5000,
    "latencyActive": true
  }'
```

#### Chaos Monkey에서 에러를 발생시키는 방법

```bash
# Chaos Monkey 활성화 (아직 안 했다면)
curl -X POST http://localhost:8080/actuator/chaosmonkey/enable

# 예외 공격 활성화
curl -X POST http://localhost:8080/actuator/chaosmonkey/assaults \
  -H "Content-Type: application/json" \
  -d '{
    "level": 5,
    "exceptionsActive": true
  }'
```
```bash
curl -X POST http://localhost:8080/actuator/chaosmonkey/assaults \
  -H "Content-Type: application/json" \
  -d '{
    "level": 5,
    "exceptionsActive": true,
    "exception": {
      "type": "java.lang.RuntimeException",
      "arguments": [
        {
          "className": "java.lang.String",
          "value": "Chaos Monkey - RuntimeException 강제 발생"
        }
      ]
    }
  }'
```

* `level` : 공격 강도(1 ~ 10, 낮을수록 자주 발생한다.)
* Repository/Service Watcher가 활성화된 메서드 호출 시에만 예외 발생한다.
* 테스트 후 반드시 비활성화해야한다.

> * **[Chaos Monkey SpringBoot Customizer Watcher](https://github.com/codecentric/chaos-monkey-spring-boot/blob/main/chaos-monkey-docs/src/main/asciidoc/watcher.adoc)**
> * **[마이크로서비스를 위한 Chaos 엔지니어링](https://news.hada.io/topic?id=21063)**
