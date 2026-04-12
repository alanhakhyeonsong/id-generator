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
| `adapter/out/persistence/repository/` | Spring Data JPA 리포지토리 | `{Name}Repository` |
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
| `common-jpa` | JPA 공통 (BaseEntity 등) | `system-core`만 의존 |
| `common-valkey` | Redisson/Valkey 설정 | 외부 의존 없음 |
| `common-lock` | 분산락 (annotation, aspect) | `common-valkey`만 의존 |
| `id-generation` | 서비스 모듈 (실행 가능) | 모든 core 모듈 의존 가능 |

## 코딩 컨벤션

### Kotlin 스타일

- JPA 엔티티: `protected constructor`, `protected set`, companion `create()` 팩토리
- InPort: `fun interface` 선호, 메서드명은 `execute`
- Service: 생성자 주입, `@Service` 어노테이션
- Controller: InPort에만 의존 (Service 직접 의존 금지)
- KDoc: 모든 public interface와 핵심 클래스에 `@author` 포함

### 테스트 규칙

| 레이어 | 대상 | 테스트 방식 |
|--------|------|-------------|
| `application/service/` | UseCase | OutPort Mockk, Kotest BehaviorSpec |
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

## 로컬 환경

- **DB**: PostgreSQL 16 — `jdbc:postgresql://localhost:5432/idgen` (testuser/testpass)
- **Cache**: Valkey 7 Sentinel — master(6379), replica(6380, 6381), sentinel(26379, 26380, 26381)
- **Password**: `testpass`
