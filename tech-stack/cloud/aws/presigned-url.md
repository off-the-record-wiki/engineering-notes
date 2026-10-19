# AWS - PreSigned URL

## 📚 PreSigned URL(미리 서명된 URL)

* S3에 직접 파일을 업로드 하기 위해서는 해당 S3에 대한 접근 권한을 인증해야 한다.
* 접근 권한에 대한 인증을 마치면 S3에 업로드 할 수 있는 URL을 발급해 주는데, 이 URL을 PreSigned URL이라고 한다.
* 발급 받은 PreSigned URL을 이용하면 브라우저에서 AWS S3 버킷에 바로 파일을 업로드 할 수 있다.

### PreSigned URL 사용 배경

* 현재 회사 진행 프로젝트의 요구사항으로 마크다운 텍스트와 여러 미디어를 같이 저장할 수 있는 기능을 개발해야 했다.
* 이 때, 기획 측 요구사항 제약 조건이 별도로 명시가 되어 있지 않았다. 어떤 미디어가 들어올지 모르기 때문에 섣불리 MultipartFile 인터페이스를 사용할 수도 없었다.
* 서버를 거치지 않으면서 대용량의 파일에 대해서도 안정적으로도 업로드가 가능하며 S3에 접근 가능한 사람만이 접근할 수 있다는 점에서 PreSigned URL을 도입하기로 결정했다.

### PreSigned URL 아키텍처

![스크린샷 2025-07-23 11 48 59](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/92bcfbb0-1a97-4bdc-aac6-45b84d74ab11.png)

* 아키텍처에는 AWS 리소스 위주로 구성이 되어있는데 부가적인 설명을 달아보자면 S3에 업로드 하기 위해 서버 측에 POST 요청을 보내 PreSigned URL을 발급받아야 하는 과정이 선행되어야 한다.
