#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
BACKEND_DIR="$ROOT_DIR/backend"
OUTPUT_DIR="$ROOT_DIR/build"
FRONTEND_DIST_DIR="$FRONTEND_DIR/dist/frontend/browser"
CONFIG_SOURCE="$ROOT_DIR/config/application.properties"

function ensure_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[錯誤] 找不到指令：$1" >&2
    exit 1
  fi
}

echo "==> 檢查必要工具"
ensure_command npm
ensure_command java

if [ ! -x "$BACKEND_DIR/mvnw" ]; then
  echo "[錯誤] 後端 Maven Wrapper (mvnw) 不存在或無法執行" >&2
  exit 1
fi

echo "==> 建置 Angular 前端"
pushd "$FRONTEND_DIR" >/dev/null
npm install
npm run build
popd >/dev/null

if [ ! -d "$FRONTEND_DIST_DIR" ]; then
  echo "[錯誤] 找不到 Angular build 產出於 $FRONTEND_DIST_DIR" >&2
  exit 1
fi

echo "==> 打包 Spring Boot 後端"
pushd "$BACKEND_DIR" >/dev/null
./mvnw clean package -DskipTests
popd >/dev/null

JAR_SOURCE=$(find "$BACKEND_DIR/target" -maxdepth 1 -type f -name "*.jar" ! -name "*original*.jar" | head -n 1)
if [ -z "$JAR_SOURCE" ]; then
  echo "[錯誤] 無法在 backend/target 找到 jar 檔" >&2
  exit 1
fi

rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"
cp "$JAR_SOURCE" "$OUTPUT_DIR/git-uploader.jar"

echo "==> 複製外部設定檔"
if [ -f "$CONFIG_SOURCE" ]; then
  mkdir -p "$OUTPUT_DIR/config"
  cp "$CONFIG_SOURCE" "$OUTPUT_DIR/config/application.properties"
else
  echo "[警告] 未找到預設設定檔 $CONFIG_SOURCE" >&2
fi

echo "\n建置完成！"
echo "可執行以下指令啟動服務："
echo "  cd \"$OUTPUT_DIR\" && java -jar git-uploader.jar"
