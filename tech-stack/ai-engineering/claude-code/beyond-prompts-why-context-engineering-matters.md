# Claude Code - Beyond Prompts: Why Context Engineering Matters

### Beyond Prompts: Why Context Engineering Matters

**1. The 4 Core Layers of Context**

> **❗프롬프트가 아니라 구조가 생산성을 만든다는 점을 인지**

* Role Context : 자신의 역할을 이해하면 책임과 기대치를 정의할 수 있다.
* Memory Context : 무엇을 기억하고 무엇을 잊을지 정하는 것이 인지 기능을 최적화한다.
* Task Context : 할 일을 명확히 해야 올바른 일을 하고 있는지 확신할 수 있다.
* Information Context : 최신 사실을 파악하고 있어야 잘못된 정보를 막을 수 있다.

**2. Role Context - "Who am I supposed to be?"**

> **❗역할을 지정하지 않으면 모델은 기본값으로 동작한다.**

* 기본값은 도움이 되고, 일반적이고, 공손한 어시스턴트이다.
* 이것이 사용자가 원하는 결과로 직결되는 경우가 거의 없다.
* 🔴Kubernetes RBAC에 대해서 설명해줘.
* 🟢당신은 프로덕션 RBAC 설계를 검토하는 스태프 SRE입니다. 보안 경계와 흔한 실패 패턴에 초점을 맞춰 Kubernetes RBAC를 설명하세요.
* 결론 : 같은 주제라도 출력 품질이 완전히 달라진다.

**3. Task Context - "What job am I actually doing?"**

> **❗실제로 어떤 일을 하고 있는가?**

* 모델은 작업을 명확히 고정하지 않으면 이것저것 조금씩 다 건드리며 얼버무린다.
* 🔴해당 코드를 리뷰해줘.
* 🟢정확성, 프로덕션 준비도, 숨은 확장성 위험을 기준으로 리뷰하고, 스타일과 포맷팅은 무시하세요.
* 결론 : 작업을 지정한다는 것은 모델에게 무엇을 최적화할지 알려주는 일이다.

**4. Information Context - "What facts are true right now?"**

> **❗지금 이 순간 사실인 것은 무엇인가?**

* 정보를 주지 않으면 모델은 기본값을 지어낸다. 빠진 부분을 틀리게 채우고 모범 사례(Best Practices)를 그대로 따르고 있다고 가정한다.
* 컨텍스트 엔지니어링은 현실을 주입하는 것이며 다음과 같이 적용한다.
  * This runs on EKS 1.28 (EKS 1.28에서 동작함)
  * IRSA is enabled (IRSA가 활성화되어 있음)
  * We cannot change IAM roles this quarter (이번 분기에는 IAM 역할을 변경할 수 없음)
  * Latency > cost (비용보다 지연 시간이 우선)

**5. Memory Context - "What should persist vs be forgotten?"**

> **❗무엇을 유지하고 무엇을 잊어야 하는가?**

* 🔴나쁜 컨텍스트 : 오래된 가정, 반쯤 진행하다 만 아이디어, 이전에 잘못 갔던 방향

**6. Why Better Context = Better Commands**

* 🔴나쁜 예시 : 이 시스템을 최적화해줘
* 🟢좋은 예시
  * Context : 읽기 위주의 워크로드. p95 지연 시간 목표 : 200ms 미만. 쓰기는 배치로만 수행. AWS는 강하고 Kubernetes 내부 구조는 약함.
  * Task : 명확한 트레이드오프와 함께 아키텍처 최적화 방안 2가지를 제안할 것

### Reference

> * [Claude Code cheatsheet](https://support.claude.com/en/articles/14553413-claude-code-cheatsheet)
