# (Effective Java Item 68) Java ‐ 일반적으로 통용되는 명명 규칙을 따르라

* 결론부터 말하자면 특별한 이유가 없다면 반드시 따르는 것이 좋다.
* 그렇지 않으면 유지보수가 어려워지고 의미를 오해할 수 있어 오류 발생 가능성이 높아진다.

#### 패키지와 모듈

* 요소를 온점(.)으로 구분하여 계층화한다.
* 요소들은 소문자 알파벳과 (드물게)숫자로 이뤄진다
* 조직의 도메인 이름을 역순으로 한다.
* 각 요소는 일반적으로 8글자 이하의 짧은 단어로 짓는다.

#### 클래스와 인터페이스

* 하나 이상의 단어로 이루어지며, Pascal Case로 작성한다.
* 널리 통용되는 약어가 아니라면 줄여쓰지 않는다.
  * 만약 널리 통용되는 약어라면 첫 글자만 대문자로 표기한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/0b2859f5-baad-4137-b3c5-227783855086.png)

#### 메서드와 필드

* 하나 이상의 단어로 이루어지며, Camel Case로 작성한다.
* 상수 필드는 예외로, 모두 대문자로 작성하며 단어 사이는 언더바(_)로 구분한다

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/db00fdca-24a6-4cc9-bba3-31f7e034fda3.png)

#### 타입 매개 변수

* `T(Type)` : 임의의 타입
* `E(Element)` : 컬렉션 원소의 타입
* `K(Key)` : 맵의 키
* `V(Value)` : 맵의 값
* `X(eXception)` : 예외
* `R(Return)` : 메서드의 반환 타입

#### 인스턴스화 가능한 클래스, Enum

* 보통 단수 명사, 명사구를 사용한다.

#### 인스턴스화가 불가능한 클래스

* 보통 복수 명사를 사용한다.

#### 어노테이션

* 규칙이 따로 없어 명사, 형용사, 동사, 전치사가 두루 쓰인다.

#### 인터페이스

* 보통 단수 명사, 명사구를 사용한다.
* `~able`, `~ible`로 끝나는 형용사를 사용하기도 한다.

#### 메서드

* 동사나 동사구를 사용한다.
* `boolean`을 반환한다면 `is~`,`has~`로 시작하고, 명사, 명사구, 형용사로 끝난다.
* `boolean`을 반환하지 않거나 인스턴스의 속성을 반환한다면 명사, 명사구, 혹은 `get~`으로 시작하는 동사구를 사용한다.

#### 특별한 메서드

* 타입을 바꿔서 다른 객체의 객체를 반환하는 메서드는 `to~`
* 객체의 내용을 다른 뷰로 보여주는 메서드는 `as~`
* 객체의 값을 기본 타입으로 반환하는 메서드는 `~Value`

> [Seven Virtues of a Good Object](https://www.yegor256.com/2014/11/20/seven-virtues-of-good-object.html)
