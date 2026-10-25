# (Clean Architecture 9) - LSP: 리스코프 치환 원칙

#### LSP: 리스코프 치환 원칙

![](https://github.com/user-attachments/assets/0221e987-bd8c-4967-8513-70108d7caf38)

* Billing 애플리케이션의 행위가 License 하위 타입 중 무엇을 사용하는지에 전혀 의존하지 않기 때문이다.
* 이들 하위 타입은 모두 License 타입을 치환할 수 있다.

#### 정사각형/직사각형 문제

![](https://github.com/user-attachments/assets/a9d611b1-8dc1-48cb-b6a2-afb9cfed7045)

* 수학에서는 정사각형이 직사각형의 한 종류이지만, 코드에서 `Square`는 "가로와 세로를 따로 바꿀 수 있다"는 `Rectangle`의 약속을 지킬 수 없다.
* 개념상 IS-A 관계처럼 보여도 상속하면 안 된다. 판단 기준은 개념 분류가 아니라 "하위 타입으로 바꿔도 동작이 같은가"이다.
