# ID Generator

Kotlin 기반 Random ID Generator + Redisson 분산락 테스트 프로젝트.

## Tech Stack

| 항목 | 기술 |
|------|------|
| Language | Kotlin 2.1.21 |
| JDK | Java 21 |
| Framework | Spring Boot 3.5.4 |
| Database | PostgreSQL 16, Spring Data JPA, QueryDSL 7 |
| Cache | Valkey 7 (Sentinel), Redisson 3.52 |
| Migration | Flyway 11.4 |
| Testing | Kotest 5.8 (BehaviorSpec), Mockk 1.14 |
| Build | Gradle 8.14 (Kotlin DSL), Version Catalog |

## Architecture

헥사고날 아키텍처 (Ports & Adapters) + 멀티모듈 Gradle 구조.

```mermaid
graph TD
    A[id-generation<br/>서비스 모듈] --> B[common-lock<br/>분산락]
    A --> C[common-jpa<br/>JPA 공통]
    A --> D[common-valkey<br/>Valkey 공통]
    A --> E[system-core<br/>도메인 모델]
    B --> D
    C --> E
```

### 모듈 구조

```
id-generator/
├── build-logic/          # Gradle 컨벤션 플러그인
├── system-core/          # 순수 도메인 (Spring 의존 X)
├── core/
│   ├── common-jpa/       # JPA 공통
│   ├── common-valkey/    # Redisson Sentinel 설정
│   └── common-lock/      # 분산락 (annotation + AOP)
├── id-generation/        # 서비스 모듈 (헥사고날)
├── docker-compose.yml    # PostgreSQL + Valkey Sentinel
└── gradle/libs.versions.toml
```

### 헥사고날 레이어 (id-generation)

```
adapter/in/rest/          → Controller (InPort 호출)
adapter/out/persistence/  → JPA Adapter (OutPort 구현)
adapter/out/cache/        → Valkey Adapter (OutPort 구현)
application/port/in/      → InPort (UseCase 인터페이스)
application/port/out/     → OutPort (저장소 인터페이스)
application/service/      → UseCase (비즈니스 로직)
domain/model/             → 도메인 모델
```

## ID 생성 알고리즘

선형 합동 생성기(LCG)를 사용하여 100,000개 범위 내에서 중복 없이 랜덤 순서로 ID를 생성한다.

```
nextSeq = (capacity + (currentSeq - 1 + coprimeIncrement)) % capacity + 1
```

- **Base33 인코딩**: I, O, S, L을 제외한 33개 문자로 4자리 ID 생성
- **서로소 증분**: capacity와 서로소인 증분값으로 전체 범위 순회 보장
- **분산락**: Redisson 기반으로 멀티 인스턴스 환경에서 시퀀스 충돌 방지

## API Endpoints

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/api/v1/id-generation/batch` | Base33 랜덤 ID 100,000건 배치 삽입 |
| `POST` | `/api/v1/id-generation/types/{type}` | 새 ID 타입 등록 |
| `POST` | `/api/v1/id-generation/{type}` | ID 생성 (e.g. `AG-A1B2`) |

## Getting Started

### 1. 로컬 인프라 실행

```bash
docker compose up -d
```

- PostgreSQL: `localhost:5432` (idgen / testuser / testpass)
- Valkey Sentinel: `localhost:26379,26380,26381` (password: testpass)

### 2. 빌드 & 실행

```bash
# 빌드
./gradlew clean build

# 실행
./gradlew :id-generation:bootRun

# 테스트
./gradlew test
```

### 3. ID 생성 테스트

```bash
# 1) 배치 ID 삽입
curl -X POST http://localhost:8080/api/v1/id-generation/batch

# 2) 타입 등록
curl -X POST http://localhost:8080/api/v1/id-generation/types/AG

# 3) ID 생성
curl -X POST http://localhost:8080/api/v1/id-generation/AG
# → "AG-A1B2"
```
