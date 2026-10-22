# Spring AI - Spring AI Component

#### [1. ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html)

* ChatClient 내부에는 유저의 질문이 AI를 거쳐 최종 결과물로 나오기까지 각각의 역할을 수행하는 5가지의 핵심 컴포넌트가 유기적으로 연결되어 있다.
* AI 대화의 시작과 끝을 관리하는 컨트롤러 역할을 한다.
* 해당 객체의 `.prompt()` 메서드를 시작으로 전체 대화 과정을 제어한다.
  * Spring AI 생태계에서 AI 모델(ChatGPT, Claude 등)과 대화를 주고받는 모든 과정을 총괄하고 제어하는 최상위 핵심 인터페이스이다.
  * `ChatClient`는 이 복잡한 대화 파이프라인을 스프링에서 쉽게 관리하기 위해 만들어졌다.
  * 빌더 패턴(Fluent API) 스타일을 제공하여 필요한 옵션들을 직관적으로 조립 가능하다.

#### [2. Advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html)

* 전처리 단계
* 유저가 보낸 질문이 AI 모델로 넘어가기 직전, 중간에 개입하여 질문을 가공하거나 필요한 배경지식(Context)을 주입한다.
* 이전 대화 기록을 불러와 질문에 붙여주거나, Vector DB 관련 문서를 찾아와 참고 자료로 결합해주는 역할을 수행한다.
  * AI 모델과 주고받는 요청과 응답을 중간에서 가로채 데이터를 동적으로 수정, 확장, 차단할 수 있게 해주는 컴포넌트이다.
  * 스프링 웹 개발의 인터셉터, 필터, 정통 AOP와 동일한 역할을 수행한다고 볼 수 있다.
* Chat Memory Advisors(대화 흐름 관리)
  * `MessageChatMemoryAdvisor` : 대화 이력을 유저/AI 메시지 구조 그대로 유지하며 프롬프트에 추가
  * `PromptChatMemoryAdvisor` : 과거 대화를 단순 텍스트로 뽑아서 시스템 프롬프트에 삽입
* Question Answering Advisors(RAG 검색 증강 생성)
  * `QuestionAnswerAdvisor` : Vector DB 자동 연동 등의 패턴을 수행
  * `RetrievalAugmentationAdvisor` : 고도화된 RAG 아키텍처를 구현할 때 사용
* Reasoning & Safety Advisors(추론 및 보안) 
  * `ReReadingAdvisor(RE2)` : LLM의 추론 능력을 높이기 위해 입력된 질문을 한 번 더 읽게끔 프롬프트를 자동으로 보강
  * `SafeGuardAdvisor` : AI 모델이 해롭거나 부적절한 답변을 생성하지 않도록 사전에 방어막을 쳐준다.

> **Advisor의 핵심 동작 원리 : 스택(Stack) 구조**
![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f0402994-d1d0-4a8f-88fe-0cab0823a3eb.png)

> * 실행순서 규칙 : `getOrder()` 메서드가 반환하는 값이 작을수록(=우선순위가 높을수록) 체인의 가장 바깥쪽(=맨 위)에 위치한다.
>   * 요청(Request) 단계 : 우선순위가 높은(=Order 값이 작은) Advisor가 가장 먼저 요청을 가로채서 전처리한다.
>   * 응답(Response) 단계 : 우선순위가 높은(=Order 값이 작은) Advisor가 가장 마지막에 응답을 가로채서 후처리함

#### [3. Prompts](https://docs.spring.io/spring-ai/reference/api/prompt.html)

* 최종 프롬프트 생성
* Advisors가 수집한 배경지식(Context)과 유저가 처음 입력한 내용을 결합하여, AI 모델이 완벽하게 이해할 수 있는 최종 프롬프트(명령어)를 완성하는 단계이다.
* [Prompt](https://github.com/spring-projects/spring-ai/blob/main/spring-ai-model/src/main/java/org/springframework/ai/chat/prompt/Prompt.java) 객체의 내부는 `List<Message>`와 `ChatOptions`이 있다.
  * `ChatOptions` : 대화의 맥락을 구성하는 여러 종류의 메시지 묶음
  * `List<Message>` : 온도, 사용할 모델명과 같은 AI 실행 옵션

> **[MessageType](https://docs.spring.io/spring-ai/docs/current/api/org/springframework/ai/chat/messages/MessageType.html)**
> 
> * `SYSTEM` : AI의 성격, 직업, 행동 지침, 제약 사항 등을 세뇌시키는 메시지
> * `USER` : 실제 서비스를 이용하는 유저가 입력한 질문이나 지시 내용
> * `ASSISTANT` : AI가 이전에 대답했던 응답 내용을 챗봇이 기억하고 이어가기 위해(Chat Memory) 덧붙이는 메시지
> * `TOOL` : AI가 외부 API나 함수(예: 날씨 조회, 사내 DB 조회)를 호출하고 돌아온 결과값을 담는 특수 메시지

> **[PromptTemplate](https://docs.spring.io/spring-ai/reference/api/prompt.html#_prompttemplate)**
> 
> * 실제로 질문이 고정되어 있지 않기에 유저의 입력값에 따라 질문 내용이 매번 바뀌어야 하므로 `PromptTemplate`을 필수적으로 사용한다.
> * 비즈니스 로직(자바 코드)과 프롬프트 문구(텍스트)를 깔끔하게 분리할 수 있어 유지보수가 매우 편해진다.

> **[ChatOptions](https://docs.spring.io/spring-ai/reference/api/chatmodel.html#_chat_options)**
>
> * `ChatOptions`란, 온도(Temperature), 사용할 모델명(GPT-4)와 같은 AI 실행 옵션을 말한다.
> * `Temperature(온도)`
>   * 0.0 ~ 0.3(낮은 온도) : 매우 결정적이고 일관된 응답, 사실 기반 답변, 데이터 분류, 코드 생성에 적합하다.
>   * 0.4 ~ 0.7(중간 온도) : 균형 잡힌 응답, 일반적인 대화나 정보 제공용 챗봇에 가장 많이 쓰는 표준 값이다.
>   * 0.8 ~ 1.0(높은 온도) : 창의적이고 다양한 응답, 엉뚱하고 기발한 생각이 필요한 스토리텔링, 소설 쓰기, 아이디어 브레인스토밍에 적합하다.
> * `Output Length(최대 길이 제한)`
>   * 5 ~ 25 : 한두 단어, 짧은 구절, 혹은 "긍정/부정" 같은 분류 라벨이다.
>   * 50 ~ 500 : 하나의 깔끔한 문단이나 짧은 설명을 원할 때 적합하다.
>   * 1000+ : 장문의 글, 소설 스토리, 복잡한 로직의 아키텍처 설명이다.
> * `Sampling Controls(샘플링 제어)`
>   * AI가 다음에 올 단어(토큰) 후보군을 추려내는 고도의 필터링 기법이다.
>   * Top-K(상위 K개 필터링) : 다음 토큰을 선택할 때, 확률이 가장 높은 상위 K개의 토큰으로만 후보군을 제한한다. 값이 클수록 다양한 단어가 섞여 나와 다양성이 증가하고, 작을수록 늘 쓰던 단어만 써서 결정적인 답변이 나온다.
>   * Top-P (누적 확률 필터링 / Nucleus Sampling) : 후보 단어들을 확률 높은 순으로 줄 세운 뒤, 누적 확률이 P(예: 90%)를 초과하기 전까지만 동적으로 단어 후보군을 선택한다. 상위 90%의 안전한 단어들 안에서만 고르게 하므로 문맥이 꼬이지 않으면서도 자연스러운 변화를 주며, 일반적으로 0.8 ~ 0.95 사이의 값을 사용한다.

> **[Spring AI 프롬프트 엔지니어링 9가지 패턴](https://www.promptingguide.ai/techniques/zeroshot)**
>
> * **Zero-Shot Prompting** : 사전 정보나 예시 없이 AI에게 곧바로 질문을 던지는 방식
> * **Few-Shot Prompting** : AI에게 1개 또는 여러 개의 정답 예시를 미리 보여주고 패턴을 학습시킨 뒤 질문을 던지는 방식
> * **Role & System Prompting** : 시스템 프롬프트를 사용해 AI에게 '페르소나(직업, 성격, 톤앤매너)'를 부여하는 방식
> * **Step-Back Prompting** : AI가 섣불리 대답하기 전에, 상황을 먼저 객관적으로 분석하고 성찰하도록 유도하는 방식
> * **Chain-of-Thought, CoT** : 단순히 답만 뱉는 게 아니라, 문제를 해결하는 과정을 단계별로 풀어서 설명하도록 지시하는 방식
> * **Self-Consistency Prompting** : CoT(생각의 사슬) 프롬프트를 여러 번 반복해서 호출한 뒤, 가장 많이 나온(일관된) 답변을 최종 정답으로 채택하는 방식
> * **Tree-of-Thoughts, ToT** : 하나의 문제에 대해 여러 가지 해결책을 먼저 제안하게 하고, 그 중 가장 좋은 것을 스스로 선택해 평가하게 만드는 방식
> * **Automatic Prompt Engineering** : 내가 쓴 부실한 프롬프트를 AI에게 던져서 "네가 더 완벽한 프롬프트로 다듬어봐"라고 시키는 방식
> * **Code Prompting** : 사용자의 입력을 기반으로 코드를 생성하거나 분석하도록 요청하는 방식

#### [4. Models](https://docs.spring.io/spring-ai/reference/api/index.html)

* AI 모델 실행
* 완성된 최종 프롬프트를 실제 외부 AI API(OpenAI, 구글 등)에 던져 답변을 받아온다.
* 이 단계에서 설정(일반 응답 vs 스트리밍)에 따라 데이터를 한 번에 통째로 받을지, 글자 단위로 실시간 스트리밍으로 받을지 응답 규격이 결정된다.
  * OpenAI(ChatGPT), Google(Gemini), Anthropic(Claude) 등 다양한 외부 AI 서비스와 통신하기 위한 자바 표준 인터페이스
  * Spring AI Models는 자바의 다형성(Polymorphism)을 극대화하여, 개발자가 단일한 스프링 인터페이스(Model)만 보고 코딩하도록 설계되어있다.
  * application.yaml과 같은 설정 파일에서 API 키와 의존성 라이브러리만 교체하여 사용 가능하다.

> * **ChatModel** : 텍스트 대화 엔진, 텍스트(Prompt)를 입력받아 텍스트(ChatResponse)를 반환하는 가장 기본적이고 핵심적인 모델
> * **EmbeddingModel** : Vector 변환 엔진, RAG(검색 증강 생성) 시스템의 핵심. 줄글 텍스트를 입력받아 Vector DB에 저장할 수 있도록 숫자 배열(Vector)로 변환해주는 모델
> * **ImageModel** : 이미지 생성 엔진, 텍스트 프롬프트를 입력받아 이미지를 생성
> * **AudioModel** : 음성 처리 엔진, 음성을 텍스트로 변환(STT)하거나, 텍스트를 음성으로 변환(TTS)하는 모델

#### [5. Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)

* 후처리 및 객체 변환
* AI 모델이 뱉어낸 줄글 형태의 텍스트 답변을 받아, 백엔드 로직에서 즉시 사용할 수 있도록 정해진 자바 객체(DTO)나 JSON 규격으로 자동 파싱해주는 최종 변환 단계이다.
  * AI 모델이 뱉어내는 예측 불가능한 문자열(String) 형태의 답변을, 백엔드 서버가 즉시 사용할 수 있는 정형화된 데이터 규격(JSON, 자바 객체, 리스트 등)으로 강제하고 변환해주는 프레임워크 지원 기능이다.
  * Structured Output 기능을 사용하면, 백엔드 로직은 AI의 응답을 `String`이 아닌 처음부터 자바 DTO나 `List<String>` 같은 깔끔한 객체 형태로 바로 건네받을 수 있다.

> * **BeanOutputConverter\<T>** : AI의 답변을 개발자가 직접 만든 커스텀 자바 클래스(DTO 또는 Record)로 변환한다.
> * **ListOutputConverter** : 답변을 단순한 `List<String>` 형태로 반환한다.
> * **MapOutputConverter** : 답변을 `Map<String, Object>` 형태의 키-값 쌍으로 반환한다.


```yml
spring:
  application:
    name: spring-ai
  ai:
#    anthropic:
#      api-key: "TEST-API-KEY"
#      chat:
#        model: claude-opus-4-8
#        max-tokens: 4096
    ollama:
      init:
        pull-model-strategy: when_missing
      chat:
        model: hf.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF # https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF
```
```java
@RestController
public class ChatController {

    private final ChatClient client;

    @Autowired
    public ChatController(ChatClient.Builder builder) {
        this.client = builder.build();
    }

    // 1) 단순하게 받기: call().content() 로 응답 본문 텍스트만 String 으로 반환받는다. 가장 기본 형태.
    @GetMapping("/chat/simple")
    public String simple(@RequestParam String prompt) {
        return this.client.prompt()
                .user(prompt)
                .call()
                .content();
    }

    // 2) ChatResponse 객체로 받기: call().chatResponse() 로 본문 + 메타데이터를 통째로 받는다.
    //    본문 텍스트 → getResult().getOutput().getText(), 토큰 사용량 → getMetadata().getUsage()
    @GetMapping("/chat/response")
    public ChatResponse response(@RequestParam String prompt) {
        return this.client.prompt()
                .user(prompt)
                .call()
                .chatResponse();
    }

    // 3) 자바 객체로 받기: call().entity(ActorFilms.class) 로 응답을 DTO(record)에 매핑한다.
    //    Spring AI 가 JSON 출력 포맷 지침을 프롬프트에 자동으로 덧붙여 그 결과를 객체로 변환한다.
    @GetMapping("/chat/entity")
    public ActorFilms entity(@RequestParam String actor) {
        return this.client.prompt()
                .user(u -> u.text("{actor}이(가) 출연한 대표 영화 5편을 알려줘.")
                            .param("actor", actor))
                .call()
                .entity(ActorFilms.class);
    }

    // 4) Flux 로 받기: stream().content() 로 토큰을 생성되는 즉시 흘려보낸다(SSE).
    //    produces = text/event-stream 이라 응답이 data: 청크로 실시간 스트리밍된다. (curl -N 로 확인)
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String prompt) {
        return this.client.prompt()
                .user(prompt)
                .stream()
                .content();
    }
}
```
