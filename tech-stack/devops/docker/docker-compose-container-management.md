# Docker ‐ Docker Compose Container Management

## 📚 Docker Compose를 관리하는 이유

* Docker Compose란, 여러 개의 Docker 컨테이너들을 하나의 서비스로 정의하고 구성해 하나의 묶음으로 관리할 수 있게 도와주는 툴이다.
* Docker Compose를 사용하는 이유
  * 여러 개의 컨테이너를 관리하는 데 용이하다.
  * 복잡한 명령어로 실행시키던 걸 간소화 시킬 수 있다.
* 아래 명령어 하나만 입력하면 간단히 실행이 된다.

```shell
$ docker compose up
```

## 📚 Docker Compose CLI

```yml
services:
  websever:
    container_name: webserver
    image: nginx
    ports: 
      - 80:80
```

```shell
$ docker compose up # 포그라운드에서 실행한다.

$ docker compose up -d # 백그라운드에서 실행한다.
```

```shell
$ docker compose ps # compose.yml에 정의된 컨테이너 중에서 실행 중인 컨테이너만 보여준다.

$ docker compose ps -a # compose.yml에 정의된 모든 컨테이너를 보여준다.
```

```shell
$ docker compose logs # compose.yml에 정의된 모든 컨테이너의 로그를 모아서 출력한다.
```

```shell
$ docker compose up -d # 컨테이너 실행하기 전에 이미지 재빌드하는 과정을 포그라운드에서 실행한다.

$ docker compose up -d --build # 컨테이너 실행하기 전에 이미지 재빌드하는 과정을 백그라운드에서 실행한다.
```

```shell
$ docker compose pull # compose.yml에서 정의된 이미지를 다운받거나 업데이트 한다.
```

```shell
$ docker compose down # 전체 컨테이너를 종료한다.
```

## 📚 컨테이너로 실행시킨 Spring Boot가 MySQL에 연결이 안되는 이유 분석하기

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/15231eb1-85b9-475c-8314-3a4c7b51ba46.png)

* 호스트 컴퓨터 내부 각각의 컨테이너는 자신만의 네트워크 망과 IP주소를 가지고 있다.
* 호스트 컴퓨터 입장에서 localhost는 호스트 컴퓨터 자신을 가리키나 Spring Boot 컨테이너 입장에서 localhost는 Spring Boot 컨테이너를 가리킨다.

```yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb # 이 부분이 문제!
    username: root
    password: pwd1234
    driver-class-name: com.mysql.cj.jdbc.Driver
```

* 여기서 yml에 명시된 localhost는 호스트 컴퓨터 내부의 Spring Boot 컨테이너를 가리킨다.
* 하지만 MySQL 컨테이너는 별도로 구축이 되어 있기 때문에 MySQL 서비스의 이름을 명시해야 한다.
* 따라서 아래와 같이 수정을 해야 한다.

```yml
services:
  my-server:
    build: .
    ports:
      - 8080:8080
    depends_on:
      my-db:
        condition: service_healthy
  my-db: # 이 서비스명이 컨테이너의 주소를 뜻한다. 해당 컨테이너의 IP 주소와 같은 역할을 한다.
    image: mysql
    environment:
      MYSQL_ROOT_PASSWORD: pwd1234
      MYSQL_DATABASE: mydb
    volumes:
      - ./mysql_data:/var/lib/mysql
    ports:
      - 3306:3306
    healthcheck:
      test: [ "CMD", "mysqladmin", "ping" ]
      interval: 5s
      retries: 10
```

```yml
spring:
  datasource:
    url: jdbc:mysql://mydb:3306/mydb
    username: root
    password: pwd1234
    driver-class-name: com.mysql.cj.jdbc.Driver
```
