---
name: rollback
description: Alpha 환경(nks_ccp-dev)에서 id-generator 앱을 이전 버전으로 롤백한다
user_invocable: true
---

# id-generator Alpha 롤백

Alpha 환경에서 id-generator 앱을 이전 버전으로 롤백한다.

## 사용법

`/rollback` — 직전 리비전으로 롤백
`/rollback <revision>` — 특정 리비전 번호로 롤백

## 절차

### 1. 현재 상태 확인

```bash
# Helm 릴리스 히스토리
helm history id-generator --kube-context nks_ccp-dev -n ramos-id-generator-test

# 현재 실행 중인 Pod 이미지 확인
kubectl --context nks_ccp-dev get pods -n ramos-id-generator-test -l app.kubernetes.io/name=id-generator -o jsonpath='{.items[0].spec.containers[0].image}'
```

### 2. 롤백 실행

특정 리비전 번호가 주어진 경우:
```bash
helm rollback id-generator <revision> --kube-context nks_ccp-dev -n ramos-id-generator-test --wait --timeout 120s
```

직전 리비전으로 롤백 (리비전 미지정):
```bash
# 히스토리에서 직전 리비전 번호 확인 후 실행
helm rollback id-generator <previous-revision> --kube-context nks_ccp-dev -n ramos-id-generator-test --wait --timeout 120s
```

### 3. 긴급 롤백 (Helm 없이)

Helm이 응답하지 않는 긴급 상황:
```bash
kubectl --context nks_ccp-dev rollout undo deployment/id-generator-deploy -n ramos-id-generator-test
```

### 4. 검증

롤백 후 반드시 확인:

```bash
# Pod 상태 (새 Pod이 Running인지)
kubectl --context nks_ccp-dev get pods -n ramos-id-generator-test -l app.kubernetes.io/name=id-generator

# Rollout 상태
kubectl --context nks_ccp-dev rollout status deployment/id-generator-deploy -n ramos-id-generator-test

# Health check
curl -s http://ramos-id-test.cone-chain.net/actuator/health

# ID 생성 테스트
curl -s -X POST http://ramos-id-test.cone-chain.net/api/v1/id-generation/BACKUP
```

## 환경 정보

- **클러스터**: nks_ccp-dev
- **네임스페이스**: ramos-id-generator-test
- **Helm Release**: id-generator

## 정리

`./infrastructure/scripts/teardown.sh` — App만 제거
`./infrastructure/scripts/teardown.sh --all` — 전체 제거 (App + Valkey + Namespace)
