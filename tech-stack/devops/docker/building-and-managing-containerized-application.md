# Docker - Building and Managing Containerized Application

## 📚 [Docker Compose](https://docs.docker.com/compose/)

* Container 애플리케이션을 정의하고 실행하는 도구
* 한 번에 여러 개의 컨테이너를 동시에 실행 -> 각 컨테이너별로 별도의 명령어 실행이 가능하다.
* 다른 컨테이너와의 접속을 쉽게 구성할 수 있다.
> 복잡한 설정을 쉽게 관리하기 위한 도구<br>
> Docker 생성, 설정 관련된 작업을 작성해놓는 Script 파일 사용 대신 Docker Compose 파일을 사용

```yml
service:
  servicename:
    image: #optional
    command: #optional
    environment: #optional
    volumes: #optional
  servicename2: # second service
volumes: #optional
networks: #optional
```

## 📚 Docker Compose 작성을 위한 명령어

| 명령어 | 설명 |
|:-----:|:-----:|
|image|Docker Image 지정|
|build|Dockerfile을 사용해 Docker Image 빌드|
|command/entrypoint|컨테이너 안에서 작동하는 명령어|
|ports/expose|통신을 위한 port 설정|
|depends_on|서비스 간 의존관계 정의|
|environment/env_file|컨테이너 환경 설정|
|container_name/labels|컨테이너 정보 설정|
|volumes/volumes_from|컨테이너 데이터 관리|
|up|컨테이너 생성 시작|
|ps|컨테이너 목록 표시|
|logs|컨네이너 로그 출력|
|run|컨테이너 실행|
|stop|컨테이너 정지|
|port|공개 포트 번호|
|rm|컨테이너 삭제|
|config|구성 확인|
|dowr|컨테이너/리소스 삭제|
