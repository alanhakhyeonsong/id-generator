#!/usr/bin/env bash
#
# id-generator Alpha 환경 배포 스크립트
#
# 사용법:
#   ./infrastructure/scripts/deploy.sh                    # 전체 배포 (infra + app)
#   ./infrastructure/scripts/deploy.sh --app-only         # App만 배포 (Helm)
#   ./infrastructure/scripts/deploy.sh --infra-only       # Infra만 배포 (Valkey)
#   ./infrastructure/scripts/deploy.sh --app-only v1.0.0  # 특정 태그로 App 배포
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

# .env 로드
if [[ -f "$ENV_FILE" ]]; then
  set -a
  source "$ENV_FILE"
  set +a
fi

KUBE_CONTEXT="nks_ccp-dev"
NAMESPACE="ramos-id-generator-test"
DB_PASSWORD='Ijinc123!@#$'
VALKEY_PASSWORD="testpass"

# 파라미터 파싱
DEPLOY_INFRA=true
DEPLOY_APP=true
IMAGE_TAG="latest"

for arg in "$@"; do
  case $arg in
    --app-only)
      DEPLOY_INFRA=false
      ;;
    --infra-only)
      DEPLOY_APP=false
      ;;
    *)
      IMAGE_TAG="$arg"
      ;;
  esac
done

echo "========================================"
echo " id-generator Alpha 배포"
echo "========================================"
echo " Context   : ${KUBE_CONTEXT}"
echo " Namespace : ${NAMESPACE}"
echo " Image Tag : ${IMAGE_TAG}"
echo " Infra     : ${DEPLOY_INFRA}"
echo " App       : ${DEPLOY_APP}"
echo "========================================"

kubectl_cmd() {
  kubectl --context "$KUBE_CONTEXT" "$@"
}

# Step 0: Namespace 생성
echo ""
echo "[0] Namespace 확인..."
kubectl_cmd apply -f "$PROJECT_ROOT/infrastructure/namespace.yaml"

# harbor-pull secret 복사 (없는 경우에만)
if ! kubectl_cmd get secret harbor-pull -n "$NAMESPACE" &>/dev/null; then
  echo "[0] harbor-pull secret 복사..."
  kubectl_cmd get secret harbor-pull -n ccp-test -o yaml \
    | sed "s/namespace: ccp-test/namespace: $NAMESPACE/" \
    | sed '/resourceVersion/d;/uid/d;/creationTimestamp/d' \
    | kubectl_cmd apply -f -
fi

# Step 1: Infra (Valkey Sentinel)
if [[ "$DEPLOY_INFRA" == "true" ]]; then
  echo ""
  echo "[1] Valkey Sentinel 배포..."
  kubectl_cmd apply -f "$PROJECT_ROOT/infrastructure/valkey/secret.yaml"
  kubectl_cmd apply -f "$PROJECT_ROOT/infrastructure/valkey/configmap.yaml"

  echo "  - Master 배포..."
  kubectl_cmd apply -f "$PROJECT_ROOT/infrastructure/valkey/master-statefulset.yaml"
  kubectl_cmd apply -f "$PROJECT_ROOT/infrastructure/valkey/master-service.yaml"
  echo "  - Master Ready 대기..."
  kubectl_cmd rollout status statefulset/valkey-master -n "$NAMESPACE" --timeout=120s

  echo "  - Replica 배포..."
  kubectl_cmd apply -f "$PROJECT_ROOT/infrastructure/valkey/replica-statefulset.yaml"
  kubectl_cmd apply -f "$PROJECT_ROOT/infrastructure/valkey/replica-service.yaml"
  echo "  - Replica Ready 대기..."
  kubectl_cmd rollout status statefulset/valkey-replica -n "$NAMESPACE" --timeout=120s

  echo "  - Sentinel 배포..."
  kubectl_cmd apply -f "$PROJECT_ROOT/infrastructure/valkey/sentinel-deployment.yaml"
  kubectl_cmd apply -f "$PROJECT_ROOT/infrastructure/valkey/sentinel-service.yaml"
  echo "  - Sentinel Ready 대기..."
  kubectl_cmd rollout status deployment/valkey-sentinel -n "$NAMESPACE" --timeout=120s

  echo "[1] Valkey Sentinel 배포 완료"
fi

# Step 2: App (Helm)
if [[ "$DEPLOY_APP" == "true" ]]; then
  echo ""
  echo "[2] id-generator App 배포 (Helm)..."
  helm upgrade --install id-generator "$PROJECT_ROOT/infrastructure/app" \
    --kube-context "$KUBE_CONTEXT" \
    -n "$NAMESPACE" \
    --set "global.image.tag=${IMAGE_TAG}" \
    --set "appSecret.DB_PASSWORD=${DB_PASSWORD}" \
    --set "appSecret.VALKEY_PASSWORD=${VALKEY_PASSWORD}" \
    --wait \
    --timeout 180s

  echo "[2] App 배포 완료"
fi

# Step 3: 상태 확인
echo ""
echo "========================================"
echo " 배포 상태"
echo "========================================"
kubectl_cmd get pods -n "$NAMESPACE" -o wide
echo ""
kubectl_cmd get svc -n "$NAMESPACE"
echo ""
kubectl_cmd get ingress -n "$NAMESPACE"
if [[ "$DEPLOY_APP" == "true" ]]; then
  echo ""
  kubectl_cmd get hpa -n "$NAMESPACE"
fi
echo ""
echo " 접근: http://ramos-id-test.cone-chain.net/actuator/health"
echo "========================================"
