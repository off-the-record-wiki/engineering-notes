# TDD - Building a Testing DSL

#### 잘 짜여진 테스트 코드의 개요와 세부 내용

* 특정 범위의 코드를 읽을 때, 코드가 수행하려는 작업에 대한 전반적인 이해와 함께 각 세부 실행 내용을 파악할 수 있다.
* 어떤 경우에는 전체적인 개요만으로도 코드를 읽는 목적에 충분할 수 있다.
* 전체 코드의 개요를 먼저 파악한 후 세부 내용을 이해하는 것이 반대의 순서보다 더 수월하다.
* 테스트 메서드가 한 눈에 들어오면 테스트 개요를 쉽게 이해할 수 있다.
* 테스트 메서드 코드의 추상화 수준을 높이고 구체적인 코드를 분리하면 테스트 코드 가독성을 높일 수 있다.

#### [테스트 픽스처(Test Fixture)](https://jojoldu.tistory.com/611)

* xUnit에서는 테스트 대상 시스템(System Under Test, 이하 SUT) 를 실행하기 위해 해줘야 하는 모든 것을 테스트 픽스처라고 한다.
* 자주 사용하게 되는 테스트 픽스처가 있다면 보통 다음과 같이 2가지로 처리한다.
  * 클래스 내부에 private 팩토리 메소드를 만들어서 사용한다.
  * 클래스 외부에 static 팩토리 메소드를 만들어서 사용한다.

```typescript
describe('Order2', () => {

    it('주문취소1', () => {
        const amount = 1000;
        const description = "배민주문";
        const sut = createOrder(amount,  description);

        const cancelOrder: Order = sut.cancel(LocalDateTime.of(2021,10,31,0,0,0));

        expect(cancelOrder.status).toBe(OrderStatus.CANCEL);
        expect(cancelOrder.amount).toBe(-amount);
        expect(cancelOrder.description).toBe(description);
    });

    it('주문취소2', () => {
        const amount = 1000;
        const sut = createOrder(amount);
        expect(sut.cancel(LocalDateTime.of(2021,10,31,0,0,0)).amount).toBe(-amount);
    });
});

function createOrder(amount: number = 1000, description: string = "배민주문") {
    return Order.create(amount, LocalDateTime.of(2021, 10, 30, 10, 0, 0), description);
}
```

* 만약 여러 곳에서 사용될 수 있는 픽스처라고 한다면 아예 별도의 팩토리 클래스로 추출해서 사용하는 것도 좋다.

```typescript
describe('Order2', () => {

    it('주문취소1', () => {
        const amount = 1000;
        const description = "배민주문";
        const sut = TestOrderFactory.create(amount,  description);

        const cancelOrder: Order = sut.cancel(LocalDateTime.of(2021,10,31,0,0,0));

        expect(cancelOrder.status).toBe(OrderStatus.CANCEL);
        expect(cancelOrder.amount).toBe(-amount);
        expect(cancelOrder.description).toBe(description);
    });

    it('주문취소2', () => {
        const amount = 1000;
        const sut = TestOrderFactory.create(amount);
        expect(sut.cancel(LocalDateTime.of(2021,10,31,0,0,0)).amount).toBe(-amount);
    });
});
```

* 둘 중 정답이 있는 것은 아니다. 둘 중 어느 방법을 사용해도 다음의 장점을 얻을 수 있다.
  * 전체 테스트 코드의 양이 줄어들고, 재사용성이 좋다.
  * 각각의 테스트 메서드 가독성이 향상되고 맥락 파악이 쉽다.
  * 각각의 테스트 픽스쳐가 모두 1회성으로 끝나는 지역변수를 사용하기에 테스트간 결합도가 낮아지는 효과를 볼 수 있다.

#### 테스트 언어

* [내부 DSL](https://ko.wikipedia.org/wiki/%EB%8F%84%EB%A9%94%EC%9D%B8_%ED%8A%B9%ED%99%94_%EC%96%B8%EC%96%B4)이란, 내부 도메인 특화 언어라고도 하는 내장형 도메인 특화 언어(eDSL)라고 불리며 프로그래밍 언어로 라이브러리로 구현되는 DSL이다.
* 테스트는 요구사항의 표현이기 때문에 쉽게 읽고 의도를 이해할 수 있어야 하는 것이 중요하다.
* 테스트를 위한 DSL을 만들어 사용하면 구체적인 내용은 함축하고 의미를 효과적으로 드러내서 테스트 코드가 짧아지고 테스트 전체 내용을 이해하기 쉽다.
