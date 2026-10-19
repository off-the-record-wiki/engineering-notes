# AWS - AWS CDK(Cloud Development Kit)

#### AWS CDK

* AWS 클라우드 개발 키트(AWS CDK)는 코드에서 클라우드 인프라를 정의하고 AWS CloudFormation을 통해 프로비저닝하기 위한 오픈 소스 소프트웨어 개발 프레임워크이다.
* AWS CDK를 정리하기 앞서 먼저 실무에서 경험하며 알게 되었던 키워드인 IaC에 대해서 먼저 알아보자.

#### IaC - Infrastructure as Code

* IaC 즉, **코드로써 인프라를 정의한다**는 의미이다.
* 작은 소규모 프로젝트의 경우 활용되는 AWS 리소스가 적다. 그렇기 때문에 빠르게 시작한다면 콘솔에서 딸깍 버튼을 통해서 하는 것이 더 빠를 수 있으나 규모가 커지는 상황에서는 콘솔에서 작업하는 것이 트래킹이 어렵고 복잡해지게 되면 까다로울 수 있다.
* IaC는 인프라 관리 코드 기반으로 자동화하는 방식으로 이렇게 코드 형태로 정의하고 관리하여 수동 설정의 번거로움과 오류를 줄이고, 일관성, 효율성, 확장성을 높일 수 있다.
* 실제로 회사에서는 Terraform과 AWS CDK를 혼용해서 사용 중이다.

#### AWS CDK

* AWS CDK를 다뤄보면서 알게 되었던 3가지 개념이 있다.

|구분|설명|
|:--------:|:--------:|
|Construct|* CDK에서 재사용 가능한 컴포넌트를 말한다.<br> * 하나 이상의 AWS CloudFormation Resources 및 설정을 캡슐화한다.<br> * 추상화 단계에 따라 Level 1~3으로 분류한다.|
|Stack|* Stack은 CDK 배포 단위이다.<br> * Stack 범위 내에서 직/간접적으로 정의된 모든 AWS 리소스는 하나의 묶음으로 프로비저닝이 된다.<br> * Stack은 AWS CloudFormation 스택을 통해 구현되므로 동일한 제한사항이 있다.|
|App|* 하나 이상의 스택을 위한 컨테이너로, 각 스택의 Scope을 정의한다.<br> * 단일 App 내의 Stack은 서로의 자원과 속성을 Logical ID를 통해 참조한다.<br> * 또한 CDK는 디펜던시 제어를 통해 종속성을 유추해 정해진 순서로 배포하는 것도 가능하다.|

#### CDK 개발언어는 어떤 것을 선택해야할까?

* AWS CDK의 장점 중 하나는 바로 개발자 친화적이라는 것이다.
* Python, TypeScript 등 친숙한 프로그래밍 언어를 통해 작성할 수 있어 진입장벽이 낮다.
* 여러 언어를 지원하지만 실제로 권장되는 언어는 TypeScript라고 한다.

> * TypeScript는 자바스크립트를 기반으로 정적 타입 문법을 추가한 프로그래밍 언어입니다.
> * 마이크로소프트에서 개발, 유지하고 있으며 자바스크립트에 비해 코드의 안정성을 높이고, 유지보수를 용이하게 해줍니다.
> * TypeScript는 자바스크립로 컴파일되고 자바스크립트의 모든 기능을 포함하고 있습니다.

* TypeScript를 권장하는 이유는 CDK가 지원하는 언어 중 가장 빨리 업데이트가 되고 Toolkit의 일부 기능은 TypeScript에서만 지원되기도 해서이다.
* 그러나 AWS에 종속된다는 점, 그리고 업데이트가 발생하면 그에 따른 버전 관리를 해줘야 한다는 점이 단점으로 지목되고 있어 회사에서는 하이브리드 방식(Terraform + AWS CDK)을 도입하고 있다.

#### 회사에서 CDK를 다루면서 참조했던 레퍼런스들 정리

* [AWS CDK 버전 업데이트 시 고려사항](https://devocean.sk.com/search/techBoardDetail.do?ID=167262&boardType=&query=CDK&searchData=&page=&subIndex=&idList=&searchText=&techType=&searchDataSub=&searchDataMain=&comment=)
* [AWS CDK가 뭔데?](https://devocean.sk.com/search/techBoardDetail.do?ID=163277&boardType=&query=CDK&searchData=&page=&subIndex=&idList=&searchText=&techType=&searchDataSub=&searchDataMain=&comment=)
* [AWS CDK 개발자 가이드](https://docs.aws.amazon.com/ko_kr/cdk/v2/guide/home.html)
