# Apache Kafka - Producer & Consumer Serialization/DeSerialization

#### Producer와 Consumer의 Custom 객체 직렬화 및 역직렬화 적용

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/99d3b460-6e3a-4271-93f3-10f50a480538.png)

* 비즈니스적으로 다양한 유형의 클래스를 데이터 인터페이스로 설정하고 해당 객체를 전송하고 읽을 수 있어야 한다.
* Kafka는 `Serializer/Deserializer`는 `String`, `Integer`, `Long` 등의 Primitive 기반의 Object들에 대해서만 기본적으로 제공된다.
* 비즈니스적으로 사용되는 데이터를 객체 형태로 전달하기 위해서 직접 `Serializer/Deserializer`를 구현해야 한다.
