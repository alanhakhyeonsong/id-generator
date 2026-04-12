#!/usr/bin/env bash
#
# Valkey Sentinel Failover 테스트 스크립트
#
# 사용법:
#   ./infrastructure/scripts/failover-test.sh              # 기본 Failover (Master 삭제 + 검증)
#   ./infrastructure/scripts/failover-test.sh --status      # 현재 상태만 확인
#   ./infrastructure/scripts/failover-test.sh --recover     # Valkey 전체 재시작 복구
#
set -euo pipefail

CONTEXT="nks_ccp-dev"
NAMESPACE="ramos-id-generator-test"
APP_URL="http://ramos-id-test.cone-chain.net"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ============================================================
# 상태 확인
# ============================================================
check_status() {
  echo ""
  echo "========================================"
  echo " Valkey Sentinel 상태 확인"
  echo "========================================"

  echo ""
  log_info "Pod 상태:"
  kubectl --context "$CONTEXT" get pods -n "$NAMESPACE" | grep valkey

  echo ""
  log_info "Sentinel Master:"
  SENTINEL_POD=$(kubectl --context "$CONTEXT" get pods -n "$NAMESPACE" -l app.kubernetes.io/component=sentinel -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
  if [[ -z "$SENTINEL_POD" ]]; then
    SENTINEL_POD=$(kubectl --context "$CONTEXT" get pods -n "$NAMESPACE" | grep valkey-sentinel | head -1 | awk '{print $1}')
  fi

  if [[ -n "$SENTINEL_POD" ]]; then
    kubectl --context "$CONTEXT" exec -n "$NAMESPACE" "$SENTINEL_POD" -c sentinel -- \
      valkey-cli -p 26379 SENTINEL get-master-addr-by-name mymaster 2>/dev/null || echo "Sentinel 조회 실패"
  else
    log_error "Sentinel Pod을 찾을 수 없습니다."
  fi

  echo ""
  log_info "Replication 역할:"
  for pod in valkey-master-0 valkey-replica-0 valkey-replica-1; do
    ROLE=$(kubectl --context "$CONTEXT" exec -n "$NAMESPACE" "$pod" -- \
      valkey-cli -a testpass INFO replication 2>/dev/null | grep "role:" || echo "role:unknown")
    echo "  $pod: $ROLE"
  done

  echo ""
  log_info "앱 헬스체크:"
  HEALTH=$(curl -s "$APP_URL/actuator/health" 2>/dev/null || echo '{"status":"DOWN"}')
  echo "  $HEALTH"

  echo ""
  log_info "ID 생성 테스트:"
  RESULT=$(curl -s -X POST "$APP_URL/api/v1/id-generation/BACKUP" 2>/dev/null || echo '{"error":"connection failed"}')
  echo "  $RESULT"
}

# ============================================================
# Master Failover 테스트
# ============================================================
failover_test() {
  echo ""
  echo "========================================"
  echo " Valkey Master Failover 테스트"
  echo "========================================"

  # 1. 사전 상태 확인
  log_info "Phase 1: 사전 상태 확인"
  check_status

  # 2. Master Pod 식별
  SENTINEL_POD=$(kubectl --context "$CONTEXT" get pods -n "$NAMESPACE" | grep valkey-sentinel | head -1 | awk '{print $1}')
  MASTER_IP=$(kubectl --context "$CONTEXT" exec -n "$NAMESPACE" "$SENTINEL_POD" -c sentinel -- \
    valkey-cli -p 26379 SENTINEL get-master-addr-by-name mymaster 2>/dev/null | head -1)

  MASTER_POD=$(kubectl --context "$CONTEXT" get pods -n "$NAMESPACE" -o wide | grep valkey | grep "$MASTER_IP" | awk '{print $1}')

  if [[ -z "$MASTER_POD" ]]; then
    log_error "Master Pod을 식별할 수 없습니다. (IP: $MASTER_IP)"
    exit 1
  fi

  log_info "현재 Master: $MASTER_POD (IP: $MASTER_IP)"
  echo ""

  # 3. Master 삭제
  log_info "Phase 2: Master Pod 삭제"
  read -r -p "$(echo -e "${YELLOW}$MASTER_POD 를 삭제하시겠습니까? (y/N): ${NC}")" CONFIRM
  if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
    log_warn "취소되었습니다."
    exit 0
  fi

  START_TIME=$(date +%s)
  kubectl --context "$CONTEXT" delete pod "$MASTER_POD" -n "$NAMESPACE"
  log_info "Master Pod 삭제 완료: $MASTER_POD"

  # 4. Failover 대기
  log_info "Phase 3: Failover 대기 (최대 30초)..."
  for i in $(seq 1 30); do
    sleep 1
    NEW_MASTER_IP=$(kubectl --context "$CONTEXT" exec -n "$NAMESPACE" "$SENTINEL_POD" -c sentinel -- \
      valkey-cli -p 26379 SENTINEL get-master-addr-by-name mymaster 2>/dev/null | head -1 || echo "")

    if [[ -n "$NEW_MASTER_IP" && "$NEW_MASTER_IP" != "$MASTER_IP" ]]; then
      END_TIME=$(date +%s)
      ELAPSED=$((END_TIME - START_TIME))
      log_info "Failover 완료! 소요시간: ${ELAPSED}초"
      log_info "새 Master IP: $NEW_MASTER_IP"
      break
    fi

    if [[ $i -eq 30 ]]; then
      log_warn "30초 내 Failover 미완료. Sentinel 상태를 확인하세요."
    fi
  done

  # 5. 검증
  echo ""
  log_info "Phase 4: Failover 후 검증"
  sleep 3

  log_info "ID 생성 테스트 (5건):"
  SUCCESS=0
  FAIL=0
  for i in $(seq 1 5); do
    RESULT=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$APP_URL/api/v1/id-generation/BACKUP" 2>/dev/null || echo "000")
    if [[ "$RESULT" == "200" ]]; then
      SUCCESS=$((SUCCESS + 1))
      echo "  요청 $i: ${GREEN}200 OK${NC}"
    else
      FAIL=$((FAIL + 1))
      echo -e "  요청 $i: ${RED}$RESULT${NC}"
    fi
  done

  echo ""
  log_info "결과: 성공=${SUCCESS}, 실패=${FAIL}"

  # 6. 최종 상태
  echo ""
  log_info "Phase 5: 최종 상태"
  check_status
}

# ============================================================
# 복구 (Valkey 전체 재시작)
# ============================================================
recover() {
  echo ""
  echo "========================================"
  echo " Valkey 전체 복구"
  echo "========================================"

  log_warn "Valkey 전체 재시작 + 앱 Pod 재시작을 수행합니다."
  read -r -p "$(echo -e "${YELLOW}진행하시겠습니까? (y/N): ${NC}")" CONFIRM
  if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
    log_warn "취소되었습니다."
    exit 0
  fi

  log_info "Valkey 컴포넌트 재시작..."
  kubectl --context "$CONTEXT" rollout restart statefulset/valkey-master -n "$NAMESPACE"
  kubectl --context "$CONTEXT" rollout restart statefulset/valkey-replica -n "$NAMESPACE"
  kubectl --context "$CONTEXT" rollout restart deployment/valkey-sentinel -n "$NAMESPACE"

  log_info "Valkey 롤아웃 대기..."
  kubectl --context "$CONTEXT" rollout status statefulset/valkey-master -n "$NAMESPACE" --timeout=60s
  kubectl --context "$CONTEXT" rollout status statefulset/valkey-replica -n "$NAMESPACE" --timeout=60s
  kubectl --context "$CONTEXT" rollout status deployment/valkey-sentinel -n "$NAMESPACE" --timeout=60s

  log_info "캐시 무효화..."
  curl -s -X DELETE "$APP_URL/api/v1/id-generation/cache" >/dev/null 2>&1 || true

  log_info "앱 Pod 재시작..."
  kubectl --context "$CONTEXT" rollout restart deployment/id-generator-deploy -n "$NAMESPACE"
  kubectl --context "$CONTEXT" rollout status deployment/id-generator-deploy -n "$NAMESPACE" --timeout=120s

  echo ""
  log_info "복구 완료. 최종 상태:"
  check_status
}

# ============================================================
# Main
# ============================================================
case "${1:-}" in
  --status)
    check_status
    ;;
  --recover)
    recover
    ;;
  *)
    failover_test
    ;;
esac
