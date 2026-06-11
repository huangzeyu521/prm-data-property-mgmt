#!/usr/bin/env bash
# 用 .ai.env(本地,不入库)的大模型配置重建三服务容器,使全部 AI 触点真实调用 qwen。
# 用法: bash scripts/restart-with-ai.sh        # 读 .ai.env
#       bash scripts/restart-with-ai.sh stub   # 临时切回本地规则桩(忽略 .ai.env)
set -e
# git-bash 会把 /app 等参数转成 Windows 路径,关闭转换
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."

ENV_ARGS=()
if [ "$1" = "stub" ]; then
  echo "[AI] provider=stub(本地规则桩)"
else
  if [ ! -f .ai.env ]; then
    echo "缺少 .ai.env —— 请: cp .ai.env.example .ai.env 并填入 DASHSCOPE_API_KEY" >&2
    exit 1
  fi
  ENV_ARGS=(--env-file .ai.env)
  echo "[AI] 使用 .ai.env(provider/model/key)重建容器"
fi

declare -A SVC=( [prm-ledger]="dpr-ledger-service 9101" \
                 [prm-confirm]="dpr-confirm-service 9102" \
                 [prm-authorize]="dpr-authorize-service 9103" )

for name in prm-ledger prm-confirm prm-authorize; do
  read -r module port <<< "${SVC[$name]}"
  docker rm -f "$name" >/dev/null 2>&1 || true
  docker run -d --name "$name" -p "$port:$port" "${ENV_ARGS[@]}" \
    -v "D:/MyProject/PRM/prm-backend/$module/target:/app" -w /app \
    eclipse-temurin:17-jre java -jar "$module-0.1.0-SNAPSHOT.jar" >/dev/null
  echo "started $name ($port)"
done

echo -n "等待健康检查"
for port in 9101 9102 9103; do
  for i in $(seq 1 40); do
    [ "$(curl -s --max-time 3 http://localhost:$port/actuator/health | head -c 15)" = '{"status":"UP"' ] && break
    sleep 3
  done
  echo -n " $port:UP"
done
echo
echo "完成。验证真调: curl -s -X POST 'http://localhost:9103/api/dpr/auth/batch-list/ai-intent?text=测试' (结论无'(规则桩)'字样即真调)"
