#!/usr/bin/env bash
#
# id-generator Alpha 환경 정리 스크립트
#
# 사용법:
#   ./infrastructure/scripts/teardown.sh              # App만 제거 (Helm)
#   ./infrastructure/scripts/teardown.sh --all         # 전체 제거 (App + Valkey + Namespace)
#   ./infrastructure/scripts/teardown.sh --infra-only  # Valkey만 제거
#
set -euo pipefail

KUBE_CONTEXT="nks_ccp-dev"
NAMESPACE="ramos-id-generator-test"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

REMOVE_APP=true
REMOVE_INFRA=false
REMOVE_NS=false

for arg in "$@"; do
  case $arg in
    --all)
      REMOVE_INFRA=true
      REMOVE_NS=true
      ;;
    --infra-only)
      REMOVE_APP=false
      REMOVE_INFRA=true
      ;;
  esac
done

kubectl_cmd() {
  kubectl --context "$KUBE_CONTEXT" "$@"
}

echo "========================================"
echo " id-generator Teardown"
echo "========================================"
echo " App    : ${REMOVE_APP}"
echo " Infra  : ${REMOVE_INFRA}"
echo " NS     : ${REMOVE_NS}"
echo "========================================"

# k6 Job 정리
echo "[0] k6 Job 정리..."
kubectl_cmd delete jobs -n "$NAMESPACE" -l app.kubernetes.io/part-of=k6-load-test --ignore-not-found

if [[ "$REMOVE_APP" == "true" ]]; then
  echo "[1] App 제거 (Helm)..."
  helm uninstall id-generator --kube-context "$KUBE_CONTEXT" -n "$NAMESPACE" 2>/dev/null || echo "  - Helm release 없음, skip"
fi

if [[ "$REMOVE_INFRA" == "true" ]]; then
  echo "[2] Valkey 제거..."
  kubectl_cmd delete -f "$PROJECT_ROOT/infrastructure/valkey/" -n "$NAMESPACE" --ignore-not-found
  echo "  - PVC 정리..."
  kubectl_cmd delete pvc -n "$NAMESPACE" -l app.kubernetes.io/part-of=valkey-sentinel --ignore-not-found
fi

if [[ "$REMOVE_NS" == "true" ]]; then
  echo "[3] Namespace 제거..."
  kubectl_cmd delete namespace "$NAMESPACE" --ignore-not-found
fi

echo ""
echo " Teardown 완료"
echo "========================================"
