# Monitoring - Log Level & Filter

#### 로그는 어떤 것을 기록하는 것인지?

* 무작정 남기면 쓰레기 데이터가 되면서 서버 용량만 차지한다.
* 너무 안 남기면 장애가 발생했을때 원인을 쉽게 찾을 수 있다.
* 언제, 어디서, 누가, 무엇을 하다가 에러가 발생했는지 추적 가능하게 남겨야 한다.
* 즉, 트러블 슈팅에 도움이 되는 로그를 남겨야 한다는 것이다.

**1. 요청/응답(Request/Response) 로그**
* 목적 : **사용자가 어떤 요청을 보냈고, 시스템이 어떻게 응답했는지를 기록한다.**
* 내부 서비스가 아닌, 외부에 있는 서비스에서는 특히 중요하다.(커뮤니케이션 비용 절감)

**2. 오류 및 예외(Exception/Error) 로그**
* 목적 : **시스템에서 발생한 오류와 예외 상황을 기록하여 디버깅에 활용한다.**
* 트러블 슈팅에 있어서 가장 중요한 로그이다.
* 시스템의 정상 작동 여부를 확인할 수 있다.

**3. 사용자 활동(Audit) 로그**
* 목적 : **사용자 행동 기록을 통한 서비스 이용 흐름 및 보안 감사를 추적한다.**
* 법적으로 남겨야 하는 로그들이 이에 해당한다.
* 실무에서는 로그를 직접 남기거나, 로그 양이 많아지면 해당 로그를 모니터링 하는 툴을 사용한다.

**4. 디버깅(Debug) 로그**
* 목적 : **상세한 정보를 기록하여 복잡한 문제를 디버깅한다.**
* 주로 개발 단계나 로컬 환경에서 많이 사용하는 로그이다.

#### 로그의 레벨

* 로그는 총 6개의 레벨(TRACE, DEBUG, INFO, WARN, ERROR, FATAL)이 있다.
* TRACE : 가장 상세한 정보를 남긴다. 평소에는 용량 문제로 상시로 켜두지 않고 일시적으로만 켜서 확인하고 다시 끄는 용도이다.
* DEBUG : 시스템의 작동 상태를 상세하게 기록한다. 서비스 개발 과정에서 주요하게 찍어야 하는 값들이나 버그들을 기록한다. 주로 개발 환경에서 코드를 테스트하고, 버그를 찾을 때 사용하고 운영환경에서는 보통 꺼두는 것이 일반적이다.
* INFO : 시스템이 예상대로 정상적으로 작동하고 있음을 알려준다. 운영환경에서 가장 기본적으로 켜두는 레벨로, 서버 시작, 사용자 로그인 완료, 배치 작업 완료 등고 같이 의미있는 주요 흐름을 기록한다.
* WARN : 당장 시스템이 멈추거나 에러가 발생한 것은 아니지만 잠재적인 문제가 될 수 있는 상황을 의미한다. 데이터가 쌓이면 쌓일수록 외부 API 응답이 점점 지연되는 경우처럼 추후에 문제가 일어날 것 같은 부분을 미리 표시해두는 것이다.
* ERROR : 시스템의 특정 기능에 오류가 발생하여 요청 작업을 정상적으로 처리하지 못하는 상황을 의미한다. DB 저장 중 예외 발생, 결제 API 호출 실패 등의 장애 상황이 대표적인 사례이다.
* FATAL : 애플리케이션이나 치명적 오류로 전체 시스템이 다운되어 더 이상 서비스를 제공할 수 없는 최악의 상태를 의미한다. DB 서버 중단/장애, 메모리 부족으로 서버 강제 종료 등 심각한 오류 발생이 대표적인 사례이다.

#### Appender

* Logback에서는 로그를 배송하는 목적지(출력 위치)를 Appender가 결정한다.
* `ConsoleAppender` : 지금까지 우리가 쓴 방식으로, 개발자의 모니터에 로그를 띄워준다. 휘발성이 강하다.
* `RollingFileAppender` : FileAppender만을 사용하면 1년 치 로그가 하나의 파일에 다 쌓여서 용량이 커진다. 매일 자정이 되면 어제 쓴 로그를 저장하고, 오늘 날짜가 적힌 새로운 로그를 만들어내는데 실무에서 해당 Appender를 사용한다.
* `FileAppender` : 로그를 텍스트 파일(.log) 안에 차곡차곡 기록한다. 삭제하지 않는 한 영구 보관된다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- property : 자바의 변수 같은 것 -->

    <!-- 로그를 저장할 경로 -->
    <property name="LOG_DIR" value="./logs" />
    <property name="LOG_FILE_NAME" value="waiting-api-log" />

    <!-- 로그를 출력할 곳을 정하는것이 appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} %highlight(%-5level) [%X{traceId}] %cyan(%logger{36}) : %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_DIR}/${LOG_FILE_NAME}.log</file>

        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level [%X{traceId}] %logger{36} : %msg%n</pattern>
        </encoder>

        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <!-- 로그를 최대 30일치만 남기는 옵션 -->
<!--            <fileNamePattern>${LOG_DIR}/${LOG_FILE_NAME}-%d{yyyy-MM-dd}.log</fileNamePattern>-->
<!--            <maxHistory>30</maxHistory>-->
            <fileNamePattern>${LOG_DIR}/${LOG_FILE_NAME}-%d{yyyy-MM-dd_HH-mm}.log</fileNamePattern>
            <maxHistory>3</maxHistory>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" /> 
        <appender-ref ref="FILE" />    
    </root>

</configuration>
```

#### 로그 압축(.gz)으로 서버 용량 확보

* 로그가 긴 파일의 경우, 로그 파일 하나의 크기가 2~3GB 넘는 파일도 있다.
* 서버의 하드디스크는 무한하지 않기 때문에, 서버 용량 부족으로 에러가 발생한다.
* 실무에서는 로그 파일을 그냥 보관하지 않고, 부피를 확 줄여서 압축해서 보관한다.
* 압축은 `.gz` 형식으로 압축하는 것이 일반적이다.
> `.gz` : 리눅스/유닉스 환경에서 가장 흔하게 쓰이는 gzip 압축 포맷이다.

```xml
<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
    <fileNamePattern>${LOG_DIR}/${LOG_FILE_NAME}-%d{yyyy-MM-dd_HH-mm}.log.gz</fileNamePattern>
    <maxHistory>5</maxHistory>
</rollingPolicy>
```
