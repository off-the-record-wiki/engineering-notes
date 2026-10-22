# (Effective Java Item 16) Java ‐ public 클래스에서는 public 필드가 아닌 접근자 메서드를 사용하라

#### public 클래스에서는 public 필드가 아닌 접근자 메서드를 사용하라

* public 클래스의 가변 필드를 직접 노출하지 않도록 하면서 필드를 모두 private으로 바꾸고 Getter를 추가한다.

```java
// Bad
class Point {
    public double x;
    public double y;
}
```

```java
// Good
class Point {
    private double x;
    private double y;
    
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public double getX() {return x;}
    public double getY() {return y;}
}
```

```java
// Good
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter //접근
@NoArgsConstructor
@Entity
public class Product extends Timestamped{

    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String image;

    @Column(nullable = false)
    private String link;

    @Column(nullable = false)
    private int lprice;

    @Column(nullable = false)
    private int myprice;

    public Product(ProductRequestDto requestDto) {
        this.title = requestDto.getTitle();
        this.link = requestDto.getLink();
        this.lprice = requestDto.getLprice();
        this.image = requestDto.getImage();
        this.myprice = 0;
    } 

    public void updateByItemDto (ItemDto itemDto) {
        this.lprice = itemDto.getLprice();
    }

    public void update(ProductMypriceRequestDto requestDto) {
        this.myprice = requestDto.getMyprice();
    }
}
```
