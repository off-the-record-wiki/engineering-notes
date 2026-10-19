# Docker - Logging and Monitoring

## 📚 [Docker Container Logging](https://docs.docker.com/reference/cli/docker/container/logs/)

* 도커 실행 시 또는 실행 이후 발생되는 로그 메시지 출력

```shell
$ docker logs <Container ID or Name>
```

* SpringBoot에서는 Logging을 위해 로그백 구현체를 사용하는 경우가 있다.
* 위의 경우에 XML에서 Docker 관련 설정 내용을 작성해주면 역시 적용이 된다.
> [참고](https://rebugs.tistory.com/761) 

## 📚 [Docker Container Monitoring - Prometheus](https://docs.docker.com/engine/daemon/prometheus/)

>❗이번에 테크 블로그 플랫폼 사이드 프로젝트를 진행하면서 모니터링을 고도화 주제로 선정했다. 그에 따라서 Prometheus를 사용해서 컨테이너를 모니터링하는 방법에 대해서 공부해보려고 한다.
* Prometheus
  * 컨테이너 환경의 애플리케이션을 모니터링하기 위한 오픈소스 시스템
  * 여러 컨테이너 상태를 개별적으로 추적 가능
  * 컨테이너 상태에 대한 Metrics 수집 -> 외부 서비스와의 연계

```shell
$ docker container run -e DOCKER_HOST=$hostIP -d --rm -p 9090:9090 diamol/prometheus:2.13.1
```
