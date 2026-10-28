# Retrospective - Datadog Summit Seoul 2026

> **❗2026년 10월 27일 코엑스에서 열린 [Datadog Summit Seoul](https://events.datadoghq.com/ko/summits/datadog-summit-seoul/)에 다녀왔다. 행사 전체를 관통한 키워드는 "AI 시대의 옵저버빌리티"였다.**

## 개요

* **행사** : Datadog Summit Seoul 2026. 옵저버빌리티와 보안 기술의 업데이트와 인사이트를 나누는 Datadog 커뮤니티 컨퍼런스.
* **일시** : 2026년 10월 27일(화) 09:00 – 17:30
* **장소** : 코엑스 북관(서울 강남구 영동대로 513)

| 시간 | 형식 | 들은 세션 | 발표 |
|---|---|---|---|
| 10:00 – 11:30 | Keynote | [기조연설](https://events.datadoghq.com/ko/summits/datadog-summit-seoul/agenda/keynote/) | Datadog, KRAFTON, CJ Olive Young |
| 12:45 – 13:20 | Breakout Session | [에이전트와 LLM을 위한 실무 중심의 AI 기반 옵저버빌리티](https://events.datadoghq.com/ko/summits/datadog-summit-seoul/agenda/llm-ai-obs/) | Shriram Subramanian(Datadog) |
| 13:35 – 16:05 | Workshop | [개발자를 위한 Datadog 핵심 활용법](https://events.datadoghq.com/ko/summits/datadog-summit-seoul/agenda/core-skills/) | 핸즈온 실습 |

## 1. 기조연설

#### AI 시대를 위한 옵저버빌리티와 보안 (Datadog)

* **발표** : Yrieix Garnier(VP, Product), Konstantinos Voloudakis(Director, Product)
* "팀이 복잡한 시스템을 이해하고 운영할 수 있도록 돕는다"는 Datadog의 비전이 차세대 AI 기반 애플리케이션까지 어떻게 이어지는지 보여 주는 제품 업데이트.
* **확장된 에이전트 기능** : 사람이 하던 조사·대응 일부를 에이전트가 맡는 방향.
* **보안·컴플라이언스** : 글로벌 기업을 위한 새로운 보안 및 컴플라이언스 기능.
* **AI 애플리케이션의 데이터 신뢰성** : AI가 내놓는 결과를 믿을 수 있게 하는 것도 관측 대상이 된다.

#### Observability for AI: 다시, 표준화 (KRAFTON)

* **발표** : 정민규(Head of Technical Management Dept.)
* 게임과 서비스가 커지고 기술 환경이 복잡해질수록 **데이터를 더 많이 수집하는 것만으로는** 문제를 더 빨리 찾고 해결할 수 없다.
* 그래서 Datadog을 중심으로 흩어져 있던 Observability 환경과 운영 기준을 먼저 **표준화**하고, 그 위에 AI 기반 RCA(Root Cause Analysis)와 자동화를 얹었다.
* 목표는 AI가 실제 장애 분석과 대응에 참여하는 것. 제목의 "다시, 표준화"가 말하듯 AI 이전에 기본기가 먼저라는 메시지.

#### 글로벌 올리브영의 성장과 신뢰성을 뒷받침하는 AI Observability (CJ Olive Young)

* **발표** : 김환(CTO)
* 글로벌 확장 과정에서 국가별로 다른 요구사항을 반영하면서도 안정적인 고객 경험을 지켜야 하는 과제.
* AI-DLC(AI-Driven Development Lifecycle)로 개발 속도를 높이고, Datadog의 관측 데이터와 **Bits AI**로 고객 영향을 파악하고 장애 원인을 좁혀 간 경험.

#### 정리

* 세 발표 모두 AI는 **관측 데이터 위에서** 동작한다는 전제를 깔고 있었다. 태그·지표·운영 기준이 제각각이면 AI가 RCA를 해도 근거가 흔들린다.
* 벤더 발표가 "무엇이 가능해졌는가"를 보여 줬다면, 고객사 발표는 "그것을 쓰려면 무엇을 먼저 갖춰야 했는가"를 보여 줬다. 둘을 이어서 들으니 도입 순서가 그려졌다.

## 2. 에이전트와 LLM을 위한 실무 중심의 AI 기반 옵저버빌리티

* **발표** : Shriram Subramanian(Group Product Manager, AI Observability, Datadog)
* **문제 제기** : "AI 에이전트를 개발하라"는 요구는 받지만 무엇을 만들지, 어떻게 잘 구현할지, 제대로 동작하는지 어떻게 확인할지 지침이 없다. 데이터 사이언티스트가 아닌 소프트웨어 엔지니어는 이 과제에 어떻게 접근해야 할까?

#### 기존 애플리케이션 개발과 무엇이 다른가

| 구분 | 기존 애플리케이션 | AI 에이전트 |
|---|---|---|
| 개발 대상 | 코드 | 코드 + 데이터 + 프롬프트 + 평가 |
| 프로세스 | 개발 → 테스트 → 배포로 이어지는 일방향 | 실험하고 평가해서 다시 고치는 반복 |
| 테스트 | 정해진 입력에 정해진 출력을 단언 | 출력이 매번 달라 기존 방식만으로는 부족 |
| "잘 동작한다"의 기준 | 시스템이 안정적으로 동작하는가 | 그에 더해 의도한 작업을 **정확하고 일관되게** 수행하는가 |

#### Datadog의 접근

* Datadog이 에이전트를 직접 출시하고 내부에서 쓰면서 얻은 경험을 바탕으로 설명.
* **LLM Observability** : 에이전트와 LLM 호출이 실제로 어떻게 실행되는지 관측한다.
* **실험(Experiments)과 평가 기반 개발** : 프롬프트·모델·데이터를 바꿔 가며 실험하고, 평가 결과로 성능을 측정해 개선한다.

#### 정리

* 위키의 [Monitoring - Service Metric(feat. DataDog)](../../tech-stack/devops/monitoring/service-metric-feat-datadog.md)에서 정리한 에러율·지연 시간·처리량은 모두 "시스템이 안정적으로 동작하는가"를 보는 지표다. 에이전트는 여기에 "**답이 맞는가**"라는 축이 하나 더 붙는다. 200 OK로 빠르게 응답해도 틀린 답일 수 있다.
* 평가 기준을 먼저 정하고 그 기준으로 개선을 반복한다는 점에서, 테스트를 먼저 쓰는 TDD와 닮은 감각이었다.

## 3. 개발자를 위한 Datadog 핵심 활용법 (Workshop)

* **형식** : 2시간 30분 핸즈온 워크숍(GitHub 계정 필요)
* **문제 의식** : 코드, CI/CD 파이프라인 데이터, 프로덕션 텔레메트리가 서로 다른 도구에 흩어져 있다. 도구를 오가는 컨텍스트 전환이 배포 속도를 늦추고, 문제가 고객에게 닿기 전에 발견하기 어렵게 만든다.
* **시나리오** : 이커머스 애플리케이션에 새 할인 기능을 출시하는 개발자 역할.

#### 실습 흐름

| 단계 | 한 일 |
|---|---|
| 1. 릴리스 전 점검 | 코드 커버리지 검증, 플래키 테스트 탐지·수정, 성능 저하를 잡는 **APM 지연 시간 모니터** 설정 |
| 2. 알림 조사 | **Software Catalog**, **APM 트레이스**, **Continuous Profiler**로 원인을 추적해 해결 |
| 3. 프로덕션 오류 대응 | AI 코딩 어시스턴트 **Bits Code**로 APM 트레이스에서 근본 원인 분석 → 수정 생성 → 풀 리퀘스트 생성 |

* 마지막 단계까지 마치면 인시던트 발생부터 해결까지 추적할 수 있는 기록이 남는다.

#### 정리

* 알림 조사 단계는 서비스(Software Catalog) → 요청(APM 트레이스) → 코드(Continuous Profiler)로 범위를 좁혀 가는 흐름이었다. 메트릭에서 이상을 본 뒤 어디를 봐야 할지 헤매던 부분이 이 순서로 정리됐다.
* 지연 시간 모니터를 **릴리스 전에** 걸어 둔다는 점이 인상적이었다. P95/P99는 장애가 난 뒤 보는 지표가 아니라 릴리스 기준이 될 수 있다.
* 커버리지·플래키 테스트 같은 릴리스 품질도 운영 텔레메트리와 같은 곳에서 본다. 옵저버빌리티의 범위가 프로덕션 이전까지 넓어진다.
* Bits Code가 트레이스에서 수정 PR까지 잇는 흐름은 오전 기조연설의 AI RCA·자동화 이야기가 개발자 손에 닿은 모습이었다. AI가 만든 수정도 결국 사람이 리뷰하므로, 트레이스라는 근거가 PR과 함께 남는다는 점이 중요해 보인다.

## 돌아보며

#### 얻은 것

* AI 옵저버빌리티는 별개의 영역이 아니라 메트릭·트레이스·로그라는 기존 옵저버빌리티 위에 쌓인다. 표준화된 데이터가 먼저다.
* 에이전트의 품질은 **시스템 안정성**과 **작업의 정확성·일관성** 두 축으로 본다. 뒤의 축은 평가로 측정한다.
* 테스트 → 모니터 → 트레이스 → 프로파일 → 수정 PR까지 소프트웨어 딜리버리 라이프사이클 전체를 한 플랫폼에서 잇는 그림을 직접 손으로 따라가 봤다.

#### 아쉬운 점

* 워크숍(13:35 – 16:05)을 듣느라 같은 시간대의 브레이크아웃 세션(MCP 기반 자율형 SRE AI 시스템, AI 에이전트 자기 강화 루프, 금융 규제 환경의 Observability 등)은 놓쳤다.

#### 적용해 볼 것

* `service`, `env`, `version` 태그와 모니터 기준을 팀 단위로 표준화하기.
* [Monitoring - Service Metric(feat. DataDog)](../../tech-stack/devops/monitoring/service-metric-feat-datadog.md)의 P95/P99 기준으로 주요 API에 APM 지연 시간 모니터 걸기.
* [Monitoring - JVM Metric(feat. DataDog)](../../tech-stack/devops/monitoring/jvm-metric-feat-datadog.md)에서 GC·Heap 이상이 보이면 Continuous Profiler로 코드 수준까지 내려가 보기.
* CI 테스트 결과에서 플래키 테스트를 찾아 정리하기.
* LLM 기능을 붙일 때는 평가 기준(정확성·일관성)부터 정하고 LLM Observability로 추적하기.
* 놓친 브레이크아웃 세션은 발표 자료가 공개되면 따로 챙겨 보기.
