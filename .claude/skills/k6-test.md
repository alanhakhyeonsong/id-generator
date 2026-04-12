---
name: k6-test
description: nks_ccp-common 클러스터에서 k6 부하테스트를 실행하고 결과 리포트를 생성한다
user_invocable: true
---

# k6 부하테스트 실행 및 리포트 생성

`nks_ccp-common` 클러스터에서 id-generator Alpha 환경 대상 k6 부하테스트를 실행하고,
결과를 `docs/load-test-report/` 경로에 Markdown 리포트로 생성한다.

## 사용법

`/k6-test` — smoke 테스트 실행
`/k6-test load` — load 테스트 실행 (50 VUs, 5분)
`/k6-test stress` — stress 테스트 실행 (최대 200 VUs, 10분)
`/k6-test <scenario>` — 지정 시나리오 실행 (smoke, load, stress, spike, soak)

## 환경 정보

- **k6 클러스터**: nks_ccp-common
- **k6 네임스페이스**: k6-load-test
- **대상 URL**: http://ramos-id-test.cone-chain.net
- **대상 API**: POST /api/v1/id-generation/BACKUP (ID 채번)
- **테스트 제외**: Batch Insert API (/api/v1/id-generation/batch)

## 절차

### 1. 사전 확인

대상 앱이 정상인지 확인한다.

```bash
# 대상 앱 헬스체크
curl -s http://ramos-id-test.cone-chain.net/actuator/health

# 대상 앱 Pod 상태
kubectl --context nks_ccp-dev get pods -n ramos-id-generator-test -l app.kubernetes.io/name=id-generator

# k6 네임스페이스 확인
kubectl --context nks_ccp-common get ns k6-load-test
```

### 2. ConfigMap 최신 반영

스크립트 변경이 있는 경우 ConfigMap을 업데이트한다.

```bash
kubectl --context nks_ccp-common apply -f infrastructure/k6/configmap.yaml
```

### 3. 기존 Job 정리

동일 이름의 Job이 남아있으면 삭제한다.

```bash
# <scenario>는 smoke, load, stress 중 하나
kubectl --context nks_ccp-common delete job k6-<scenario> -n k6-load-test --ignore-not-found
```

### 4. 테스트 실행

```bash
kubectl --context nks_ccp-common apply -f infrastructure/k6/job-<scenario>.yaml
```

### 5. 완료 대기 및 로그 수집

```bash
# 완료 대기 (타임아웃은 시나리오에 따라 조정)
# smoke: 120s, load: 600s, stress: 900s, spike: 600s, soak: 2400s
kubectl --context nks_ccp-common wait --for=condition=complete job/k6-<scenario> -n k6-load-test --timeout=<timeout>s

# 실패한 경우에도 로그 확인
kubectl --context nks_ccp-common logs job/k6-<scenario> -n k6-load-test
```

### 6. 대상 앱 상태 확인

테스트 후 앱이 정상인지 확인한다.

```bash
# Pod 상태
kubectl --context nks_ccp-dev get pods -n ramos-id-generator-test -l app.kubernetes.io/name=id-generator

# 리소스 사용량
kubectl --context nks_ccp-dev top pods -n ramos-id-generator-test -l app.kubernetes.io/name=id-generator

# HPA 상태
kubectl --context nks_ccp-dev get hpa -n ramos-id-generator-test

# 헬스체크
curl -s http://ramos-id-test.cone-chain.net/actuator/health
```

### 7. 리포트 생성

테스트 결과를 분석하여 `docs/load-test-report/` 경로에 Markdown 리포트를 생성한다.

**파일명 규칙**: `YYYYMMDD-<scenario>-test.md`

**리포트 필수 포함 항목**:

1. **목적 (Goal)** — 테스트 목적
2. **배경 (Context)** — 테스트 환경 정보 (클러스터, 네트워크 경로, 대상 URL)
3. **테스트 환경** — 앱 스펙 (replicas, resources, JVM), k6 Job 스펙
4. **테스트 설정** — k6 옵션 (VUs, duration, stages, thresholds)
5. **결과 요약 테이블** — 총 요청수, 성공/실패율, 응답시간 (avg, p50, p90, p95, p99, max), 처리량
6. **Threshold 판정** — 각 threshold의 PASS/FAIL
7. **원인 분석** — 병목 지점 식별, Mermaid 다이어그램 포함
8. **권장 후속 조치** — 우선순위별 개선 방안
9. **메타 정보** — 테스트 일시, k6 버전

### 8. Job 정리

테스트 완료 후 Job을 삭제한다.

```bash
kubectl --context nks_ccp-common delete job k6-<scenario> -n k6-load-test
```

## 시나리오별 설정

| 시나리오 | VUs | Duration | 목적 |
|----------|-----|----------|------|
| smoke | 1 | 30s | 기본 연결 및 기능 검증 |
| load | 50 | 5m | 정상 부하에서의 성능 측정 |
| stress | 10→200 | 10m | 한계 부하 탐색 |
| spike | 10→500 | 5m | 급격한 트래픽 증가 대응 |
| soak | 30 | 30m | 장시간 안정성 검증 |

## k6 스크립트 위치

- ConfigMap: `infrastructure/k6/configmap.yaml`
- Job YAML: `infrastructure/k6/job-{smoke,load,stress}.yaml`
- 리포트: `docs/load-test-report/`

## 정리

```bash
# 전체 Job 정리
kubectl --context nks_ccp-common delete jobs -n k6-load-test -l app.kubernetes.io/part-of=k6-load-test

# 네임스페이스 전체 삭제 (필요 시)
kubectl --context nks_ccp-common delete namespace k6-load-test
```
