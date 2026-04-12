# ID Generator 프로젝트 가이드

## 프로젝트 개요

기존 Java 기반 Random ID Generator 및 Redisson 분산락 모듈을 Kotlin으로 변환하고,
헥사고날 아키텍처 + 멀티모듈 구조로 개선/테스트하는 프로젝트.

## 아키텍처 규칙

### 헥사고날 아키텍처 (Ports & Adapters)

이 프로젝트는 헥사고날 아키텍처를 따른다. 코드 작성 시 아래 의존 방향을 반드시 준수한다.

```
Adapter(In) → Port(In) ← Service → Port(Out) → Adapter(Out)
```

- **의존 방향**: 바깥 → 안쪽. Domain/Application 레이어는 Adapter를 알지 못한다.
- **Port(In)**: UseCase 인터페이스. Controller가 호출한다.
- **Port(Out)**: 외부 시스템 인터페이스. Service가 호출하고, Adapter가 구현한다.
- **Service**: InPort를 구현하며, OutPort에만 의존한다.

### 패키지 구조 규칙

서비스 모듈(`id-generation`) 내부 패키지는 다음을 따른다.

| 패키지 | 역할 | 네이밍 규칙 |
|--------|------|-------------|
| `adapter/in/rest/controller/` | HTTP 입력 어댑터 | `{Feature}RestController` |
| `adapter/in/rest/dto/` | REST 요청/응답 DTO | `{Feature}Request`, `{Feature}Response` |
| `adapter/out/persistence/entity/` | JPA 엔티티 | `{Name}JpaEntity` |
| `adapter/out/persistence/repository/` | Spring Data JPA + QueryDSL 리포지토리 | `{Name}Repository`, `{Name}RepositoryCustom`, `{Name}RepositoryImpl` |
| `adapter/out/persistence/adapter/` | OutPort 구현체 (JPA) | `{Name}PersistenceAdapter` |
| `adapter/out/cache/` | OutPort 구현체 (Valkey) | `{Name}CacheAdapter` |
| `application/port/in/` | 입력 포트 (UseCase 인터페이스) | `{Action}InPort` |
| `application/port/out/` | 출력 포트 (외부 시스템 인터페이스) | `{Action}OutPort` |
| `application/service/` | UseCase 구현체 | `{Action}UseCase` |
| `application/dto/` | Application DTO | `{Name}Command`, `{Name}Result` |
| `application/exception/` | 비즈니스 예외 | `{Name}Exception` |
| `domain/model/` | 도메인 모델 | 비즈니스 용어 기반 |
| `domain/vo/` | Value Object | `{Name}` (inline value class 권장) |

### 모듈 구조

| 모듈 | 역할 | 의존 제약 |
|------|------|----------|
| `system-core` | 순수 도메인 모델 (열거형, 유틸리티) | Spring 의존 금지 |
| `common-jpa` | JPA 공통 (QueryDslConfig 등) | `system-core`만 의존 |
| `common-valkey` | Redisson/Valkey Sentinel 설정 | 외부 의존 없음 |
| `common-lock` | 분산락 (annotation, aspect, DistributedLockManager) | `common-valkey`만 의존 |
| `common-web` | 공통 웹 (GlobalExceptionHandler, LoggingFilter) | `system-core`만 의존 |
| `id-generation` | 서비스 모듈 (실행 가능) | 모든 core 모듈 의존 가능 |

### 분산 락 패턴 (Segment 블록 할당)

ID 채번은 **SegmentIdAllocator** 기반 블록 할당 방식을 사용한다.

- Pod별 1,000건 ID 블록을 사전 확보 (분산 락 사용)
- 블록 내에서는 AtomicInteger로 채번 (락 없음)
- 블록 소진 시에만 분산 락 획득 (1,000건당 1회)
- Valkey 장애 시: 블록 잔여분으로 서비스 지속, 할당 실패 시 retry 2회 후 503

## 코딩 컨벤션

### Kotlin 스타일

- JPA 엔티티: `protected constructor`, `protected set`, companion `create()` 팩토리
- InPort: `fun interface` 선호, 메서드명은 `execute`
- Service: 생성자 주입, `@Service` 어노테이션
- Controller: InPort에만 의존 (Service 직접 의존 금지)
- KDoc: 모든 public interface와 핵심 클래스에 `@author` 포함

### QueryDSL 규칙

- `@Query`/`@Modifying` 사용 금지 — QueryDSL로 작성
- 커스텀 쿼리: `{Name}RepositoryCustom` 인터페이스 + `{Name}RepositoryImpl` 구현
- `JpaRepository`에는 Spring Data 파생 쿼리(method name query)만 허용
- `JPAQueryFactory` 빈은 `common-jpa` 모듈의 `QueryDslConfig`에서 제공

### 캐시 어댑터 규칙

- Valkey 장애 시 DB fallback 필수 (try-catch)
- 캐시 저장 실패는 무시 (warn 로그만)
- 캐시는 성능 최적화 수단이지 필수 의존이 아님

### 테스트 규칙

| 레이어 | 대상 | 테스트 방식 |
|--------|------|-------------|
| `application/service/` | UseCase, SegmentIdAllocator | OutPort Mockk, Kotest BehaviorSpec |
| `domain/` | 도메인 모델, VO | 순수 로직 테스트, Kotest BehaviorSpec |
| `system-core/` | 유틸리티 | 순수 함수 테스트, Kotest BehaviorSpec |

위 레이어의 코드를 작성/수정할 때는 **반드시** 단위 테스트를 함께 작성해야 한다.

## 커밋 메시지 규칙

- **형식**: `{type}({브랜치명}): 제목`
- **type**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`
- **예시**: `feat(develop): 배치 ID 삽입 API 추가`

## 빌드 & 실행

```bash
# 로컬 인프라 시작
docker compose up -d

# 전체 빌드
./gradlew clean build

# 테스트만 실행
./gradlew test

# id-generation 모듈 실행
./gradlew :id-generation:bootRun
```

## Alpha 배포 (Claude Skills)

```bash
/deploy          # 전체 빌드 + 배포 (Gradle → Docker → Harbor → Helm)
/deploy --skip-build  # Helm upgrade만
/rollback        # 직전 리비전으로 롤백
/rollback <N>    # 특정 리비전으로 롤백
/k6-test         # Smoke 테스트
/k6-test load    # Load 테스트 (50 VUs, 5분) + 리포트 생성
```

## 환경 정보

### 로컬

- **DB**: PostgreSQL 16 — `jdbc:postgresql://localhost:5432/idgen` (testuser/testpass)
- **Cache**: Valkey 7 Sentinel — master(6379), replica(6380, 6381), sentinel(26379, 26380, 26381)
- **Password**: `testpass`

### Alpha (nks_ccp-dev)

- **클러스터**: nks_ccp-dev
- **네임스페이스**: ramos-id-generator-test
- **Ingress**: ramos-id-test.cone-chain.net
- **DB**: jdbc:postgresql://192.168.0.42:5432/ramos-test (admin)
- **Registry**: harbor.cone-chain.net/id-generator/id-generator
- **k6 클러스터**: nks_ccp-common (k6-load-test 네임스페이스)
