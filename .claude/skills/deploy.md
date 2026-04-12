---
name: deploy
description: Alpha 환경(nks_ccp-dev)에 id-generator 앱을 빌드/배포한다
user_invocable: true
---

# id-generator Alpha 배포

Alpha 환경(`nks_ccp-dev` 클러스터, `ramos-id-generator-test` 네임스페이스)에 id-generator를 빌드하고 배포한다.

## 사용법

`/deploy` — 전체 빌드 + 배포 (Gradle → Docker → Harbor → Helm upgrade)
`/deploy --skip-build` — 이미지 재빌드 없이 Helm upgrade만 수행
`/deploy <tag>` — 특정 태그로 배포

## 절차

### 1. Gradle 빌드 (--skip-build가 아닌 경우)

```bash
./gradlew :id-generation:clean :id-generation:build -x test
```

### 2. Docker 이미지 빌드 & Harbor 푸시

```bash
TAG=$(date +%Y%m%d%H%M%S)
docker build --platform linux/amd64 -f infrastructure/Dockerfile \
  -t harbor.cone-chain.net/id-generator/id-generator:$TAG \
  -t harbor.cone-chain.net/id-generator/id-generator:latest .

# Harbor 로그인 (.env에서 인증정보 로드)
source infrastructure/scripts/.env
echo "$HARBOR_PASSWORD" | docker login "$HARBOR_URL" -u "$HARBOR_USERNAME" --password-stdin
docker push harbor.cone-chain.net/id-generator/id-generator:$TAG
docker push harbor.cone-chain.net/id-generator/id-generator:latest
```

### 3. Helm 배포

```bash
helm upgrade --install id-generator ./infrastructure/app \
  --kube-context nks_ccp-dev \
  -n ramos-id-generator-test \
  --set global.image.tag=$TAG \
  --set 'appSecret.DB_PASSWORD=Ijinc123!@#$' \
  --set appSecret.VALKEY_PASSWORD=testpass \
  --wait \
  --timeout 180s
```

### 4. 검증

배포 후 반드시 확인:

```bash
# Pod 상태
kubectl --context nks_ccp-dev get pods -n ramos-id-generator-test -l app.kubernetes.io/name=id-generator

# Health check
curl -s http://ramos-id-test.cone-chain.net/actuator/health

# ID 생성 테스트
curl -s -X POST http://ramos-id-test.cone-chain.net/api/v1/id-generation/BACKUP
```

## 환경 정보

- **클러스터**: nks_ccp-dev
- **네임스페이스**: ramos-id-generator-test
- **Registry**: harbor.cone-chain.net/id-generator/id-generator
- **Ingress**: ramos-id-test.cone-chain.net
- **DB**: jdbc:postgresql://192.168.0.42:5432/ramos-test (admin)
- **Valkey**: K8s 내부 Sentinel (testpass)

## 또는 스크립트 사용

빌드/푸시만: `./infrastructure/scripts/build-push.sh`
전체 배포: `./infrastructure/scripts/deploy.sh`
