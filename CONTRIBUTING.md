# 기여 가이드 (CONTRIBUTING.md)

이 문서는 프로젝트의 원활한 협업을 위한 코딩 컨벤션, 브랜치 전략 및 기타 규칙을 정의함. 모든 팀원은 아래 가이드를 숙지하고 준수해야 함.

---

## 목차

* **[Git 브랜치 전략 및 커밋 규칙](#git-브랜치-전략-및-커밋-규칙)**
    * [브랜치 전략](#브랜치-전략)
    * [병합(Push) 규칙](#병합push-규칙)
    * [커밋 메시지 규칙](#커밋-메시지-규칙)
* **[프로젝트 및 패키지 구조](#프로젝트-및-패키지-구조)**
    * [프로젝트 구조](#프로젝트-구조)
    * [패키지 구조](#패키지-구조)
* **[코딩 컨벤션](#코딩-컨벤션)**
    * [클래스 명명 규칙](#클래스-명명-규칙)
    * [변수 명명 규칙](#변수-명명-규칙)
    * [의존성 규칙](#의존성-규칙)
    * [Bean 등록 및 주입](#bean-등록-및-주입)
    * [Entity와 DTO 규칙](#entity와-dto-규칙)
* **[테스트 코드 작성 규칙](#테스트-코드-작성-규칙)**
    * [예시 Template](#예시-template)
* **[API 설계 및 예외 처리](#api-설계-및-예외-처리)**
    * [API 설계 원칙](#api-설계-원칙)
    * [API 문서화](#api-문서화)
    * [예외 처리](#예외-처리)
* **[데이터베이스 스키마 규칙](#데이터베이스-스키마-규칙)**
    * [테이블 명명 규칙](#테이블-명명-규칙)
    * [컬럼 명명 규칙](#컬럼-명명-규칙)

---

### Git 브랜치 전략 및 커밋 규칙

#### 브랜치 전략

- **`main`**: 최종 배포를 위한 브랜치.
- **`develop`**: 개발의 중심이 되는 브랜치. 기능 개발이 완료되면 `develop`으로 병합.
- **`개인이름`**: 기능 개발을 위한 개인 브랜치. (예: `kwonhyeongtaeg`)

#### 병합(Push) 규칙

- `develop` 브랜치로의 병합은 기능 단위로 완결된 상태에서만 가능.
- 모든 `develop` 브랜치로의 병합은 PR 요청하여 관리자의 코드 리뷰를 거친 후, 최종 확인 후 병합.
- `develop` 브랜치로의 병합 전 백엔드 코드 작성 시, 관련 테스트 코드를 반드시 작성하고 모든 테스트를 통과해야 함.
- 공통 라이브러리 및 리소스(JS, CSS 등) 추가/변경 시, 구동에 문제가 없음을 확인하고 전체 팀원에게 내용을 공유한 후 `develop` 브랜치에 반영.

#### 커밋 메시지 규칙

커밋 메시지는 해당 커밋의 내용을 명확하게 파악할 수 있도록 작성.

- **예시**:
    - `UserController - 로그인 매핑 추가 및 구현.`
    - `UserController - 로그인 매핑 추가 및 구현. / UserService - 사용자 로직 인터페이스 생성`

---

### 프로젝트 및 패키지 구조

#### 프로젝트 구조

```
project-root/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/
│   │   │       └── goodee/
│   │   │           └── startup/
│   │   │               (자세한 내용은 '패키지 구조' 참조)
│   │   └── resources/
│   │       ├── static/
│   │       ├── templates/
│   │       └── application.properties
│   ├── test/
├── README.md
├── CONTRIBUTING.md
└── build.gradle
```

#### 패키지 구조

패키지는 도메인 중심의 구조를 따름. 각 도메인은 하위에 `controller`, `service`, `repository`, `entity`, `dto`, `exception` 등의 계층별 패키지를 포함.

```
src.main.java.org.goodee.startup/
├── user/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── exception/
├── post/
├── common/
└── config/
```

---

### 코딩 컨벤션

#### 클래스 명명 규칙

클래스의 역할이 명확히 드러나도록 접미사(Suffix)를 사용.

- **Controller**: `UserController`
- **Service**: `UserService`
- **Repository**: `UserRepository`

#### 변수 명명 규칙

- 변수명은 카멜 케이스(camelCase) 표기법을 따름.
- 변수의 의미를 명확하게 표현할 수 있도록 작성.
- 배열의 경우 `array`, 리스트의 경우 `List`를 접미사로 사용. (예: `userList`)
- 변수명은 숫자로 시작하거나 특수문자를 포함하지 않음.

#### 의존성 규칙

- 계층 간 의존성은 `Controller` -> `Service` -> `Repository` 방향으로 단방향을 유지하여 순환 참조를 방지.
- 예를들어 `Service` 계층은 웹(Web) 계층에 대한 의존성을 갖지 않음. `HttpServletRequest`, `HttpSession` 등 웹 관련 객체의 직접적인 사용을 금지하여 계층 간 결합도를
  낮춤.

#### Bean 등록 및 주입

- `@Component`, `@Service`, `@Repository` 등 스테레오타입 애노테이션을 활용한 컴포넌트 스캔을 기본으로 사용.
- 의존성 주입은 반드시 생성자 주입(Constructor Injection) 방식을 사용.
- 설정(Configuration) 클래스에서 `@Bean`을 명시적으로 등록해야 하는 경우, 필요시 목적에 따라 프로파일(`@Profile`)을 분리하여 관리.

```java
@Configuration
@Profile("dev")
public class DevDataSourceConfig {
    @Bean
    public DataSource dataSource() {
        // 개발 환경용 데이터 소스 설정
    }
}
```

#### Entity와 DTO 규칙

- 역할 분리: Controller <-> Service 계층 간 데이터 교환에는 DTO(Data Transfer Object)를 사용하고, Service <-> Repository 계층 및 DB 연동에는
  Entity를 사용.

- 변환 로직 위치: Entity와 DTO 간의 변환 로직은 DTO 클래스 내에 위치.

    - toEntity(): DTO를 Entity로 변환

    - toDTO(): Entity를 DTO로 변환

- 변환 방식: 변환 메서드는 정적 팩토리 메소드를 활용하여 구현.

- 데이터 변경 로직: 데이터 변경 로직(update)은 Service 계층이 아닌 Entity 자체에 위임. Service는 해당 Entity의 메소드를 호출하는 역할만 수행. (Tell, Don't Ask 원칙)

---

### 테스트 코드 작성 규칙

테스트 코드는 Given-When-Then 패턴을 따라 작성하여 테스트의 의도를 명확하게 드러냄.

- Given: 테스트에 필요한 환경과 데이터를 설정하는 단계.

- When: 실제 테스트할 코드를 실행하는 단계.

- Then: 테스트 결과를 검증하고 단언(Assertion)하는 단계.

#### 예시 Template

```java
@Test
    void 테스트() {
        // given: 이러한 데이터가 주어졌을 때
    
        // when: 이 메소드를 실행하면

        // then: 이러한 결과가 나와야 한다
        
    }
```

---

### API 설계 및 예외 처리

#### API 설계 원칙

- URI: 리소스를 표현하며, 리소스 이름은 복수형 명사를 사용. (예: /users, /products)

- 식별자: 특정 리소스를 식별할 때는 경로 변수(@PathVariable)를 사용. (예: /users/{userId})

#### API 문서화

- 모든 API는 Swagger UI에서 명세를 쉽게 확인할 수 있도록, Swagger 문법에 따라 코드에 문서 주석을 작성함.

#### 예외 처리

- 예외 처리는 @RestControllerAdvice와 @ExceptionHandler를 활용한 중앙 집중 방식으로 처리하여 코드 중복을 최소화하고 일관성을 유지.

---

### 데이터베이스 스키마 규칙

#### 테이블 명명 규칙

- 테이블명은 소문자와 스네이크 케이스(snake_case)를 사용하며, `tbl_` 접두사를 붙임.
    - 예시: `tbl_member`, `tbl_product`

#### 컬럼 명명 규칙

- **PK (Primary Key)**: `테이블명_id` 형식으로 작성.
    - 예시: `member_id`, `product_id`
- **Boolean 타입**: 컬럼의 의미를 명확하게 파악할 수 있도록 질문 형식으로 작성.
    - 예시: `is_deleted`, `is_active`

---

##### 위 규칙을 준수하여 프로젝트의 일관성과 가독성을 유지해주세요.

##### 문의 사항 또는 수정요청은 아래의 연락처로 말씀해주세요.

- tel) 010-2063-1003
- kakao) kht8552














