# (Effective Java Item 2) Java - 생성자에 매개변수가 많다면 빌더를 고려하라

```java
// 요약 : 점층적 생성자 패턴은 확장하기 어렵다.
public class NutritionFacts {
	private final int servingSize;  // 필수
	private final int servings;     // 필수
	private final int calories;     // 선택
	private final int fat;          // 선택
	private final int sodium;       // 선택
	private final int carbohydrate; // 선택

	public NutritionFacts(int servingSize, int servings) {
		this(servingSize, servings, 0);
	}

	public NutritionFacts(int servingSize, int servings, int calories) {
		this(servingSize, servings, calories, 0);
	}

	public NutritionFacts(int servingSize, int servings, int calories, int fat) {
		this(servingSize, servings, calories, fat, 0);
	}

	public NutritionFacts(int servingSize, int servings, int calories, int fat, int sodium) {
		this(servingSize, servings, calories, fat, 0);
	}

		public NutritionFacts(int servingSize, int servings, int calories, int fat, int sodium, int carbohydrate) {
		this.servingSize = servingSize;
		this.servings = servings;
		this.calories = calories;
		this.fat = fat;
		this.sodium = sodium;
		this.carbohydrate = carbohydrate;
	}
}
```
* 점층적 생성자 패턴도 쓸 수는 있지만, 매개변수 개수가 많아지면 클라이언트 코드를 작성하거나 읽기가 어렵다.
* 코드를 읽을 때 각 값의 의미가 무엇인지 헷갈릴 것이고 매개변수가 몇 개인지도 주의해서 세어 보아야 할 것이다.
* 타입이 같은 매개변수가 연달아 늘어서 있으면 찾기 어려운 버그로 이어질 수 있다.

#### 자바 빈즈(Java Beans)

* 매개변수가 없는 생성자로 객체를 먼저 만든 후, `setter` 메서드들을 호출해 원하는 매개변수의 값을 설정하는 방식이다.

```java
public class NutritionFacts {
    private int servingSize = -1;  // Required; no default value
    private int servings = -1;     // Required; no default value
    private int calories = 0;      // Optional; default value
    private int fat = 0;           // Optional; default value
    private int sodium = 0;        // Optional; default value
    private int carbohydrate = 0;  // Optional; default value

    public NutritionFacts() { }

    // Setters
    public void setServingSize(int servingSize) { this.servingSize = servingSize; }
    public void setServings(int servings) { this.servings = servings; }
    public void setCalories(int calories) { this.calories = calories; }
    public void setFat(int fat) { this.fat = fat; }
    public void setSodium(int sodium) { this.sodium = sodium; }
    public void setCarbohydrate(int carbohydrate) { this.carbohydrate = carbohydrate; }
}
```
* 자바빈즈 패턴에서는 객체 하나를 만들려면 메서드를 여러 개 호출해야 하고, 객체가 완전히 생성되기 전까지는 일관성(consistency)이 무너진 상태에 놓이게 된다.
* 일관성이 깨진 객체가 만들어지면 버그를 심은 코드와 그 버그 때문에 런타임에 문제를 겪는 코드가 물리적으로 멀리 떨어져 있어 디버깅도 만만치 않다.
* 이런 일관성 측면에서의 단점을 완화하고자 생성이 끝난 객체를 수동으로 얼리고 얼리기 전에는 사용할 수 없도록 하기도 한다. 하지만 이 방법은 다루기 어려워서 실전에서 거의 사용되지 않는다.
* 이 방법을 쓴다고 하더라도 객체 사용 전에 프로그래머가 `freeze` 메서드를 확실히 호출했는가를 컴파일러가 보증할 방법이 없어서 런타임 오류에 취약하다.

#### 빌더 패턴(Builder Pattern)

* 필요한 객체를 직접 만드는 대신 필수 매개변수만으로 생성자를 호출해 빌더 객체를 얻을 수 있다.

```java
public class NutritionFacts {
    private final int servingSize;
    private final int servings;
    private final int calories;
    private final int fat;
    private final int sodium;
    private final int carbohydrate;

    public static class Builder {
        // Required parameters
        private final int servingSize;
        private final int servings;

        // Optional parameters - initialized to default values
        private int calories = 0;
        private int fat = 0;
        private int sodium = 0;
        private int carbohydrate = 0;

        public Builder(int servingSize, int servings) {
            this.servingSize = servingSize;
            this.servings = servings;
        }

        public Builder calories(int val) {
            calories = val;
            return this;
        }

        public Builder fat(int val) {
            fat = val;
            return this;
        }

        public Builder sodium(int val) {
            sodium = val;
            return this;
        }

        public Builder carbohydrate(int val) {
            carbohydrate = val;
            return this;
        }

        public NutritionFacts build() {
            return new NutritionFacts(this);
        }
    }

    private NutritionFacts(Builder builder) {
        servingSize = builder.servingSize;
        servings = builder.servings;
        calories = builder.calories;
        fat = builder.fat;
        sodium = builder.sodium;
        carbohydrate = builder.carbohydrate;
    }
}
```
* 객체를 만들려면, 그에 앞서 빌더부터 만들어야 한다.
* 빌더 생성 비용이 크지는 않지만 성능에 민감한 상황에서는 문제가 될 수 있다.
* 생성자나 정적 팩토리가 처리해야 할 매개변수가 많다면 빌더 패턴을 선택하는 것이 더 나을 수 있다.
* 그러나 이 빌더 패턴에 장점만 있는 것은 아니다. 객체를 만들려면, 그에 앞서 빌더부터 만들어야 한다.
* 빌더 생성 비용이 크진 않지만, 성능에 민감한 상황이 될 수 있다.
* 또한 점층적 생성자 패턴보다는 코드가 장황해서 매개변수가 4개 이상은 되어야 값어치를 한다. 하지만 API는 시간이 지날수록 매개변수가 많아지는 경향이 있음을 명심하자.
