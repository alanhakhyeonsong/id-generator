# 분산 락과 Failover 안전성 분석

## 목적 (Goal)

Valkey Sentinel 환경에서 Redisson 분산 락의 Failover 취약점을 분석하고,
Segment 블록 할당 패턴을 통한 대응 전략의 설계 근거와 실측 결과를 기록한다.

---

## 분산 락의 기본 원리

### Redis/Valkey 분산 락 동작

```mermaid
sequenceDiagram
    participant A as Client A
    participant M as Valkey Master
    participant B as Client B

    A->>M: SET lock_key UUID_A NX PX 5000
    M-->>A: OK (락 획득)

    B->>M: SET lock_key UUID_B NX PX 5000
    M-->>B: nil (락 점유 중)

    Note over A: 비즈니스 로직 실행

    A->>M: Lua Script (UUID_A 확인 후 DEL)
    M-->>A: 1 (락 해제)

    B->>M: SET lock_key UUID_B NX PX 5000
    M-->>B: OK (락 획득)
```

**핵심 보장:**
- `NX` (Not eXists): 키가 없을 때만 SET → 상호 배제 보장
- `PX` (만료 시간): 클라이언트 비정상 종료 시 데드락 방지
- `UUID`: 본인이 잡은 락만 해제 가능 → 타인의 락 해제 방지

---

## RedLock vs Sentinel 기반 락

### RedLock 알고리즘

Martin Kleppmann과 Salvatore Sanfilippo(Redis 창시자)의 논쟁으로 유명한 알고리즘.
**N개의 독립 Master**에서 과반수 합의로 락을 획득한다.

```mermaid
flowchart LR
    Client[Client]
    Client -->|SET NX| M1[Master 1]
    Client -->|SET NX| M2[Master 2]
    Client -->|SET NX| M3[Master 3]
    Client -->|SET NX| M4[Master 4]
    Client -->|SET NX| M5[Master 5]

    M1 -->|OK| Client
    M2 -->|OK| Client
    M3 -->|OK| Client
    M4 -->|fail| Client
    M5 -->|fail| Client

    style M1 fill:#dfd,stroke:#3a3
    style M2 fill:#dfd,stroke:#3a3
    style M3 fill:#dfd,stroke:#3a3
    style M4 fill:#fdd,stroke:#a33
    style M5 fill:#fdd,stroke:#a33
```

**RedLock 과반수 합의 프로세스:**

1. 현재 시간 기록 (T1)
2. N개 Master에 순차적으로 SET NX PX 요청
3. 과반수(N/2+1) 이상에서 성공 && 소요시간 < TTL일 때 락 획득
4. 유효 락 시간 = TTL - 소요시간(T2-T1)
5. 과반수 미달 시 모든 Master에서 DEL → 락 해제

### Sentinel 기반 락

**단일 Master에서 SET NX를 수행.** Sentinel은 Master 발견과 Failover만 담당하며,
**락 합의에는 관여하지 않는다.**

```mermaid
flowchart LR
    Client[Client]
    S1[Sentinel 1] -.->|Master 위치 제공| Client
    S2[Sentinel 2] -.->|모니터링| M
    S3[Sentinel 3] -.->|모니터링| M

    Client -->|SET NX| M[Master]
    M -->|비동기 복제| R1[Replica 1]
    M -->|비동기 복제| R2[Replica 2]

    style M fill:#dfd,stroke:#3a3
    style R1 fill:#ddf,stroke:#33a
    style R2 fill:#ddf,stroke:#33a
```

### 비교

| 기준 | RedLock | Sentinel 기반 |
|------|---------|---------------|
| 락 대상 | N개 독립 Master | 단일 Master |
| 합의 방식 | 클라이언트가 과반수 합의 | 없음 (단일 Master SET) |
| Failover 안전성 | Master 독립이라 영향 없음 | **비동기 복제로 락 유실 가능** |
| 인프라 비용 | Master 5개 (최소 3개) | Master 1 + Replica 2 + Sentinel 3 |
| 운영 복잡도 | 높음 (독립 인스턴스 관리) | 낮음 |
| 성능 | 느림 (N개 왕복) | 빠름 (1회 왕복) |
| 시계 의존성 | 높음 (TTL 정확성 필요) | 낮음 |

---

## Sentinel 기반 락의 Failover 취약점

### 문제 시나리오: 비동기 복제 + Failover = 락 유실

```mermaid
sequenceDiagram
    participant A as Client A
    participant M as Master (Old)
    participant R as Replica → Master (New)
    participant B as Client B
    participant S as Sentinel

    A->>M: SET lock_key UUID_A NX PX 5000
    M-->>A: OK (락 획득)
    Note over A: 비즈니스 로직 실행 중

    Note over M: Master crash!
    Note over M,R: 비동기 복제 미완료<br/>lock_key가 Replica에 없음

    S->>R: SLAVEOF NO ONE (Master 승격)
    Note over R: 새 Master (lock_key 없음)

    B->>R: SET lock_key UUID_B NX PX 5000
    R-->>B: OK (락 획득!)

    Note over A,B: 동시에 두 클라이언트가<br/>같은 락을 보유 (안전성 위반)
```

### 취약 조건 3요소

```mermaid
flowchart TD
    V1[비동기 복제<br/>Master → Replica] --> Vuln{락 유실<br/>가능성}
    V2[Failover 타이밍<br/>복제 전 Master crash] --> Vuln
    V3[Lock Lease Time<br/>Failover보다 긴 유효 시간] --> Vuln

    Vuln -->|3요소 동시 충족| Risk[두 클라이언트가<br/>동시에 락 보유]

    style Vuln fill:#f66,stroke:#333,color:#fff
    style Risk fill:#fdd,stroke:#a33
```

| 요소 | 설명 | 발생 확률 |
|------|------|-----------|
| **비동기 복제** | Master → Replica 복제에 ms~s 지연 | 항상 존재 |
| **Failover 타이밍** | 복제 전 Master crash | 낮음 (ms 단위 윈도우) |
| **Lease Time** | Failover 후에도 Client A가 락 유효하다고 판단 | 설정 의존 |

### 실제 영향 범위

| 상황 | 영향 |
|------|------|
| SET NX 직후 Master crash | 락 유실 → 중복 획득 가능 |
| SET NX 후 충분한 시간 경과 | 복제 완료 → 새 Master에도 락 존재 |
| Lease Time 만료 후 Failover | 이미 해제된 락 → 영향 없음 |

> **결론: 취약 윈도우는 "SET NX 직후 ~ 복제 완료 전"의 ms~수백ms 구간.**
> 이 짧은 구간에 Master crash가 발생해야 하므로, 실제 발생 확률은 매우 낮다.

---

## Segment 블록 할당 패턴

### 설계 동기

분산 락의 Failover 취약점을 **완전히 제거하는 것은 불가능**하다.
대신, **락 사용 빈도를 극단적으로 줄여** 취약 윈도우에 노출될 확률을 최소화한다.

### 핵심 아이디어

```
기존: 매 요청 1건 = 분산 락 1회 (락:요청 = 1:1)
개선: 1,000건당 분산 락 1회 (락:요청 = 1:1000)
```

### 아키텍처

```mermaid
flowchart TD
    subgraph pod1["Pod 1"]
        SA1[SegmentIdAllocator]
        Seg1[IdSegment<br/>sequences: LongArray<br/>cursor: AtomicInteger]
        SA1 --> Seg1
    end

    subgraph pod2["Pod 2"]
        SA2[SegmentIdAllocator]
        Seg2[IdSegment<br/>sequences: LongArray<br/>cursor: AtomicInteger]
        SA2 --> Seg2
    end

    subgraph shared["공유 자원"]
        Valkey[(Valkey<br/>분산 락)]
        DB[(PostgreSQL<br/>used_id)]
    end

    SA1 -->|1000건당 1회| Valkey
    SA2 -->|1000건당 1회| Valkey
    SA1 -->|블록 할당 시| DB
    SA2 -->|블록 할당 시| DB

    style Seg1 fill:#dfd,stroke:#3a3
    style Seg2 fill:#dfd,stroke:#3a3
    style Valkey fill:#ffd,stroke:#aa3
```

### 동작 흐름

```mermaid
flowchart TD
    Req[ID 생성 요청] --> Check{Segment 블록<br/>잔여 있음?}

    Check -->|Yes: 99.9%| Atomic["AtomicInteger.getAndIncrement()<br/>Pre-computed sequence 반환<br/>분산 락 없음, ~3ms"]

    Check -->|No: 0.1%| Sync["synchronized(type.intern())<br/>Pod 내 단일 스레드만 진입"]
    Sync --> Lock["Redisson tryLock<br/>ID-SEGMENT:{type}<br/>waitTime=10s, leaseTime=5s"]
    Lock --> Alloc["doAllocateSegment:<br/>1. 캐시/DB에서 entity 로드<br/>2. LCG로 1000건 시퀀스 사전 계산<br/>3. DB 상태 갱신 (currentSeq, count)<br/>4. 캐시 갱신"]
    Alloc --> NewSeg["새 IdSegment 생성<br/>sequences: LongArray(1000)"]
    NewSeg --> Atomic

    Atomic --> DBSelect["DB SELECT random_id_generator<br/>WHERE id_generation_seq = seq"]
    DBSelect --> Result["{type}-{randomNo}"]

    style Atomic fill:#dfd,stroke:#3a3
    style Lock fill:#ffd,stroke:#aa3
    style Result fill:#ddf,stroke:#33a
```

### IdSegment 구조

```kotlin
class IdSegment(
    private val sequences: LongArray,  // 사전 계산된 1000개 시퀀스
) {
    private val cursor = AtomicInteger(0)  // CAS 기반 원자적 증가

    fun next(): Long? {
        val index = cursor.getAndIncrement()
        return if (index < sequences.size) sequences[index] else null
    }
}
```

**Thread Safety:**
- `AtomicInteger.getAndIncrement()`: CAS(Compare-And-Swap) 기반, 락 없이 원자적
- 50 VUs 동시 접근에서도 경합 없이 ~3ms 응답

### Failover 내성 분석

```mermaid
flowchart TD
    Failover[Valkey Master<br/>Failover 발생] --> Impact{현재 요청이<br/>락 필요?}

    Impact -->|No: 99.9%<br/>블록 잔여 있음| Safe["정상 처리<br/>Valkey 완전 무관<br/>AtomicInteger + DB SELECT"]

    Impact -->|Yes: 0.1%<br/>블록 소진 시점| Retry["블록 할당 retry<br/>1차: 즉시 시도<br/>2차: 1초 후 재시도"]

    Retry -->|Failover 완료| Success["새 Master에서<br/>블록 할당 성공"]
    Retry -->|Failover 미완료| Err503["503 Service Unavailable<br/>LockAcquisitionFailedException"]

    style Safe fill:#dfd,stroke:#3a3
    style Success fill:#ddf,stroke:#33a
    style Err503 fill:#ffd,stroke:#aa3
```

| 시나리오 | 기존 (매 요청 락) | Segment 적용 후 |
|----------|-------------------|-----------------|
| Failover 중 영향 요청 비율 | **100%** (모든 요청이 락 필요) | **0.1%** (블록 소진 시점만) |
| 락 유실 시 영향 | 즉시 500 에러 | 블록 잔여분으로 서비스 지속 |
| 취약 윈도우 노출 빈도 | 요청 수 = 락 수 | 요청 수 / 1000 = 락 수 |

---

## 방어 코드 설계

### 계층별 방어

```mermaid
flowchart TD
    subgraph L1["계층 1: Redisson 설정"]
        R1["retryAttempts=5<br/>Failover 중 재연결 시도"]
        R2["retryInterval=2000ms<br/>재시도 간격"]
        R3["connectTimeout=5000ms<br/>빠른 실패 감지"]
    end

    subgraph L2["계층 2: Segment 블록"]
        S1["BLOCK_SIZE=1000<br/>락 빈도 99.9% 감소"]
        S2["AtomicInteger 채번<br/>Valkey 무관"]
    end

    subgraph L3["계층 3: 앱 방어 코드"]
        A1["블록 할당 retry 2회<br/>Failover 완료 대기"]
        A2["캐시 graceful degradation<br/>장애 시 DB fallback"]
        A3["LockAcquisitionFailedException<br/>503 응답 (재시도 유도)"]
    end

    L1 --> L2 --> L3
```

### 캐시 Graceful Degradation

```kotlin
// Valkey 장애 시에도 서비스 지속
override fun getOrLoad(type: String, loader: () -> UsedIdJpaEntity): UsedIdJpaEntity {
    val cached = try {
        redisTemplate.opsForValue().get(KEY_PREFIX + type)
    } catch (e: Exception) {
        log.warn(e) { "캐시 조회 실패, DB fallback" }
        null  // 장애 시 DB로 fallback
    }
    // ...
}

override fun put(type: String, entity: UsedIdJpaEntity): UsedIdJpaEntity {
    try {
        redisTemplate.opsForValue().set(...)
    } catch (e: Exception) {
        log.warn(e) { "캐시 저장 실패 (무시)" }  // 장애 시 무시
    }
    return entity
}
```

**설계 원칙: 캐시는 성능 최적화 수단이지 필수 의존이 아니다.**

---

## 실측 결과

### 성능 (50 VUs, 5분, In-Cluster)

| 지표 | 매 요청 락 (Phase 0) | Segment (Phase 2) | 개선율 |
|------|---------------------|-------------------|--------|
| 실패율 | 91.33% | **0.00%** | 해소 |
| p(95) | 5,000ms | **7.22ms** | **692배** |
| 처리량 | 8.89 req/s | **238.23 req/s** | **27배** |

### Failover 내성 (50 VUs 부하 중 Master 삭제)

| 지표 | 방어 코드 적용 전 | 방어 코드 적용 후 |
|------|-------------------|-------------------|
| 총 요청 수 | 82,481 | 79,289 |
| 실패 | 21건 (0.02%) | **0건 (0.00%)** |
| 처리량 | 274.92 req/s | 264.29 req/s |

---

## 대안 비교

### 언제 어떤 전략을 선택하나?

| 전략 | 적합한 상황 | 부적합한 상황 |
|------|-----------|-------------|
| **Sentinel + Segment (본 프로젝트)** | ID 채번처럼 블록 사전 할당이 가능한 경우 | 매 요청이 고유한 락 키를 사용하는 경우 |
| **RedLock** | 금융 거래 등 락 유실이 절대 불가한 경우 | 성능이 중요하고 인프라 비용이 제한적인 경우 |
| **DB 비관적 락** | 단순 구조, Valkey 없이 처리하고 싶은 경우 | 높은 동시성 + 빠른 응답이 필요한 경우 |
| **Fencing Token** | 락 유실 후에도 데이터 무결성을 검증해야 하는 경우 | 쓰기 대상이 fencing을 지원하지 않는 경우 |

### Fencing Token 패턴 (참고)

Martin Kleppmann이 제안한 락 안전성 보완 패턴.
락 획득 시 단조 증가하는 토큰을 발급하고, 쓰기 대상이 토큰 순서를 검증한다.

```mermaid
sequenceDiagram
    participant A as Client A
    participant Lock as Lock Service
    participant DB as Storage

    A->>Lock: acquire_lock()
    Lock-->>A: token=33

    Note over A: Master Failover → 락 유실

    participant B as Client B
    B->>Lock: acquire_lock()
    Lock-->>B: token=34

    B->>DB: write(data, token=34)
    DB-->>B: OK

    A->>DB: write(data, token=33)
    DB-->>A: REJECT (33 < 34, 이미 더 높은 토큰 존재)
```

> 본 프로젝트에서는 Segment 블록이 사실상 Fencing Token 역할을 수행한다.
> 블록 할당 시 DB의 `currentSeq`/`count`가 단조 증가하며,
> 이전 블록의 시퀀스는 새 블록과 겹치지 않으므로 데이터 무결성이 보장된다.

---

## 결론

### Sentinel 기반 분산 락의 한계와 대응

1. **한계**: 비동기 복제 + Failover 조합에서 이론적 락 유실 가능 (ms 단위 취약 윈도우)
2. **실무적 판단**: 발생 확률이 극히 낮고, ID 채번의 경우 블록 기반 설계로 영향을 무력화 가능
3. **Segment 패턴의 효과**: 락 사용 빈도를 1/1000로 줄여 취약 윈도우 노출 자체를 최소화
4. **방어 코드의 역할**: Failover 구간에서도 retry + graceful degradation으로 서비스 지속

### 설계 원칙

```
"락의 안전성을 높이는 것보다, 락이 필요한 순간을 줄이는 것이 더 효과적이다."
```

- RedLock으로 완벽한 락을 만드는 대신, **Segment로 락 의존을 최소화**
- 캐시 장애 시 **DB fallback으로 서비스 지속** (캐시는 성능 수단, 필수 의존 아님)
- 남은 극소수 락 실패는 **retry + 503으로 클라이언트 재시도 유도**

---

## 참고 자료

- [Martin Kleppmann — How to do distributed locking (2016)](https://martin.kleppmann.com/2016/02/08/how-to-do-distributed-locking.html)
- [Salvatore Sanfilippo — Is Redlock safe? (2016)](http://antirez.com/news/101)
- [Redis Distributed Locks (RedLock)](https://redis.io/docs/manual/patterns/distributed-locks/)
- [Redisson Lock Documentation](https://github.com/redisson/redisson/wiki/8.-Distributed-locks-and-synchronizers)
