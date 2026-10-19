# AWS - Cognito

## 📚 Cognito

* Amazon Cognito 사용자 풀은 웹 및 모바일 앱 인증과 권한 부여를 위한 사용자 디렉터리이다.
* 앱의 관점에서 보면 Amazon Cognito 사용자 풀은 OpenID Connect(OIDC) 제공업체이다.
* 사용자 풀(User Pool)은 보안, 아이덴티티 페더레이션, 앱 통합, 사용자 경험 사용자 지정을 위한 기능 계층을 추가한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/8a0ba36c-4d55-4739-8a48-0c8f65c9ab7b.png)

### Cognito 사용 배경

* 회사에서 전반적인 인증을 Cognito 자격 증명을 사용해 관리하고 있었다.
* 복잡한 인증 백엔드를 직접 구축하고 관리하는 대신, Cognito가 제공하는 강력하고 안전한 기능을 활용하여 개발자는 핵심 비즈니스 로직에 더 집중할 수 있다는 장점을 느꼈다.

### Cognito 주요 기능 및 구성 요소

**사용자 풀 (User Pools)**
* 사용자 풀은 애플리케이션의 사용자 디렉터리를 제공합니다. 사용자의 회원가입과 로그인을 처리하며, 다음과 같은 기능을 포함한다.
  * 로그인 및 회원가입
  * 소셜 및 연동 로그인
  * 보안 기능
  * 사용자 지정 UI
**자격 증명 풀 (Identity Pools)**
* 자격 증명 풀은 사용자에게 AWS 서비스에 액세스할 수 있는 임시 자격 증명을 부여하는 역할을 한다.
* 이를 통해 인증된 사용자 또는 익명의 사용자에게도 특정 AWS 리소스(예: Amazon S3, DynamoDB)에 대한 접근 권한을 안전하게 제어할 수 있다.
  * AWS 자격 증명 부여
  * 세분화된 접근 제어
  * 인증되지 않은 사용자 지원
