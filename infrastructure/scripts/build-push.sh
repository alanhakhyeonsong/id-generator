#!/usr/bin/env bash
#
# id-generator 이미지 빌드 & Harbor 푸시 스크립트
#
# 사용법:
#   ./infrastructure/scripts/build-push.sh              # 자동 태그 (timestamp)
#   ./infrastructure/scripts/build-push.sh v1.0.0       # 커스텀 태그
#   ./infrastructure/scripts/build-push.sh --skip-build  # Gradle 빌드 생략 (이미 빌드된 경우)
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
else
  echo "ERROR: $ENV_FILE 파일이 없습니다."
  exit 1
fi

# 파라미터 파싱
SKIP_BUILD=false
CUSTOM_TAG=""

for arg in "$@"; do
  case $arg in
    --skip-build)
      SKIP_BUILD=true
      ;;
    *)
      CUSTOM_TAG="$arg"
      ;;
  esac
done

# 태그 결정
TAG="${CUSTOM_TAG:-$(date +%Y%m%d%H%M%S)}"
FULL_IMAGE="${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${TAG}"
LATEST_IMAGE="${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:latest"

echo "========================================"
echo " id-generator Build & Push"
echo "========================================"
echo " Registry : ${HARBOR_URL}/${HARBOR_PROJECT}"
echo " Image    : ${IMAGE_NAME}:${TAG}"
echo " Skip Build: ${SKIP_BUILD}"
echo "========================================"

# Step 1: Gradle 빌드
if [[ "$SKIP_BUILD" == "false" ]]; then
  echo ""
  echo "[1/4] Gradle 빌드 시작..."
  cd "$PROJECT_ROOT"
  ./gradlew :id-generation:clean :id-generation:build -x test
  echo "[1/4] Gradle 빌드 완료"
else
  echo ""
  echo "[1/4] Gradle 빌드 생략 (--skip-build)"
fi

# Step 2: JAR 파일 확인
JAR_FILE="$PROJECT_ROOT/id-generation/build/libs/app.jar"
if [[ ! -f "$JAR_FILE" ]]; then
  echo "ERROR: $JAR_FILE 파일이 없습니다. 빌드를 먼저 실행하세요."
  exit 1
fi
echo "[2/4] JAR 파일 확인: $(du -h "$JAR_FILE" | cut -f1)"

# Step 3: Docker 빌드
echo ""
echo "[3/4] Docker 이미지 빌드..."
cd "$PROJECT_ROOT"
docker build \
  --platform linux/amd64 \
  -f infrastructure/Dockerfile \
  -t "$FULL_IMAGE" \
  -t "$LATEST_IMAGE" \
  .
echo "[3/4] Docker 빌드 완료: $FULL_IMAGE"

# Step 4: Harbor 로그인 & 푸시
echo ""
echo "[4/4] Harbor 푸시..."
echo "$HARBOR_PASSWORD" | docker login "$HARBOR_URL" -u "$HARBOR_USERNAME" --password-stdin
docker push "$FULL_IMAGE"
docker push "$LATEST_IMAGE"
echo "[4/4] Harbor 푸시 완료"

echo ""
echo "========================================"
echo " 완료!"
echo " Image: $FULL_IMAGE"
echo ""
echo " Helm 배포:"
echo "   helm upgrade --install id-generator ./infrastructure/app \\"
echo "     -n ramos-id-generator-test \\"
echo "     --set global.image.tag=${TAG} \\"
echo "     --set 'appSecret.DB_PASSWORD=Ijinc123!@#\$' \\"
echo "     --set appSecret.VALKEY_PASSWORD=testpass"
echo "========================================"
