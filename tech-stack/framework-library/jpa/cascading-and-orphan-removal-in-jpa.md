# JPA - Cascading and Orphan Removal in JPA

## 📚 영속성전이: CASCADE

* 특정 엔티티를 영속 상태로 만들 때 연관된 엔티티도 함께 영속 상태로 만들고 싶으면 영속성 전이 기능을 사용하면 된다.
* JPA에서 엔티티를 저장할 때는 모두 영속 상태여야 한다.
* 부모를 영속화할 때 연관된 자식들도 함께 영속화하는 코드는 부모 측에 작성해주면 된다.

```java
@Entity
@Getter
@Setter
public class Parent {

	@Id
	@GeneratedValue
	private Long id;

	// 부모를 영속화 할 때 자식 엔티티들도 같이 영속화해라.
        // 부모 엔티티 측에서 영속화
	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
	private List<Child> children = new ArrayList<>();

	public void addChild(Child child) {
		this.children.add(child);
		child.setParent(this);
	}
}
```
```java
@SpringBootTest
@Transactional
class ParentTest {

	@PersistenceContext
	EntityManager em;

	@Test
	@Commit
	void test() {
		Parent parent = new Parent();

		Child child1 = new Child();
		Child child2 = new Child();

		parent.addChild(child1);
		parent.addChild(child2);

		em.persist(parent);
		em.flush();
		em.clear();
	}
}
```

## 📚 고아 객체

* JPA는 부모 엔티티와 연관관계가 끊어진 자식 엔티티를 자동으로 삭제하는 기능을 제공하는데 이것을 고아 객체 제거라고 한다.
* 고아 객체 제거는 참조가 제거된 엔티티는 다른 곳에서 참조하지 않는 고아 객체로 보고 삭제하는 기능이다.
* 따라서 다른 곳에서 참조를 하는 엔티티라면 사용이 불가능하며 참조하는 곳이 하나일 때만 사용해야 한다.
* 이런 이유로 `orphanRemoval = true`는 `@OneToMany`, `@OneToOne` 경우에만 사용이 가능하다.

```java
@Entity
@Getter
@Setter
public class Parent {

	@Id
	@GeneratedValue
	private Long id;

	// 고아 객체 제거 기능 활성화
	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Child> children = new ArrayList<>();

	public void addChild(Child child) {
		this.children.add(child);
		child.setParent(this);
	}
}
```
