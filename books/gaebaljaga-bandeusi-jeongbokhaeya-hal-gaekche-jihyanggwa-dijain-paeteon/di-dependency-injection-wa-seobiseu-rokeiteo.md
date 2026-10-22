# 개발자가 반드시 정복해야 할 객체 지향과 디자인 패턴 - DI(Dependency Injection)와 서비스 로케이터

#### 요구사항 - 어플리케이션 영역과 메인 영역

***

1. 파일의 확장자를 이용해서 비디오 파일의 포맷을 변환한다.
2. 변환 요청을 등록하면 순차적으로 변환 작업을 수행한다. 이 때, 변환 요청 정보는 파일 또는 DB를 이용해서 보관할 수 있어야 한다.
3. 비디오 형식의 변환 처리는 오픈소스인 ffmpeg를 이용하거나 구매 예정인 변환 솔루션을 사용할 수 있어야 한다.
4. 명령 행에서 변환할 원본 파일과 변환 결과로 생성될 파일을 입력한다.

***

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ac806aed-92f7-48c7-b00f-724d87dbf0fa.png)

#### DI(Dependency Injection)을 이용한 의존 객체 사용

* 만약 사용할 객체를 직접 생성할 경우, 아래 코드처럼 콘크리트 클래스에 대한 의존이 발생하게 된다.

```java
public class Worker {
    public void run() {
        JobQueue jobQueue = new FileJobQueue();  // DIP 위반
    }
}
```
* 콘크리트 클래스를 직접 사용해서 객체를 생성하게 되면 의존 역전 원칙을 위반하게 되어 결과적으로 확장 폐쇄 원칙을 위반하게 된다.
* 이런 단점을 보완하기 위한 방법이 DI이다. DI는 필요한 객체를 직접 생성하거나 찾지 않고 외부에서 넣어주는 방식이다. DI 자체 구현은 매우 간단한데, 사용할 객체를 전달받을 수 있는 방법을 제공하면 DI를 적용하기 위한 준비가 끝나게 된다.

```java
public class Worker {
    private JobQueue jobQueue;
    private Transcoder transcoder;

    // 외부에서 사용할 객체를 전달받을 수 있는 방법을 제공한다.
    public Worker(JobQueue jobQueue, Transcoder transcoder) {
        this.jobQueue = jobQueue;
        this.transcoder = transcoder;
    }
}
```

#### DI 적용 방법 1 - 생성자 방식(권장)

* 생성자 방식은 생성자를 통해서 의존 객체를 전달받는 방식이다.
* 생성자 방식은 객체를 생성하는 시점에 필요한 모든 의존 객체를 준비할 수 있기 때문이다.
* 생성자 방식은 생성자를 통해서 필요한 의존 객체를 전달받기 때문에, 객체를 생성하는 시점에서 의존 객체가 정상인지 확인할 수 있다.

```java
public class JobCLI {
    private JobQueue jobqueue;

    public JobCLI(JobQueue jobQueue) {
        this.jobQueue = jobQueue;
    }
}
```

#### DI 적용 방법 2 - 설정 메서드 방식

* 설정 메서드 방식은 메서드를 이용해서 의존 객체를 전달받는 방식이다.
* 설정 메서드 방식은 객체를 생성한 뒤에 의존 객체를 주입하게 된다. 이 경우 의존 객체를 생성하지 못한 상태에서 객체를 사용할 수 있게 되므로, 객체의 메서드를 호출하는 과정에서 NPE가 발생하게 된다.

```java
public class Worker {
    private JobQueue jobQueue;
    private Transcoder transcoder;

    public void setJobQueue(JobQueue jobQueue) {
        this.jobQueue = jobQueue;
    }

    public void setTranscoder(Transcoder transcoder) {
        this.transcoder = transcoder;
    }
}
```
