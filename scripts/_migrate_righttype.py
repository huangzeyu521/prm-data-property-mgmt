# -*- coding: utf-8 -*-
"""权益类型术语统一迁移:长式 → 短式(持有权/使用权/经营权)。
系统数据值/代码/UI/种子/测试全量迁移;法律·官方原文(KB指导书/D协议范本/工作指引/docs)保留长式。
用法: python _migrate_righttype.py [--apply]   (默认 dry-run 只报告)
"""
import os
import sys

ROOT = r"D:\MyProject\PRM"
APPLY = "--apply" in sys.argv

# 长 → 短(顺序无冲突;两种持有权变体都收敛为"持有权")
REPL = [
    ("数据资源持有权", "持有权"),
    ("数据持有权", "持有权"),
    ("数据加工使用权", "使用权"),
    ("数据产品经营权", "经营权"),
]

# 保留长式的法律/官方原文(相对 ROOT 的路径片段,大小写敏感,用 / 分隔)
EXCLUDE_SUBSTR = [
    "/prm-backend/dpr-confirm-service/src/main/resources/aitool/kb/",
    "/prm-backend/dpr-confirm-service/src/main/resources/aitool/templates/",
    "/prm-frontend/src/assets/workGuide.js",
    "/docs/",
    "/README.md",
    "/output/",            # 产品设计文档另行处理
    "/node_modules/",
    "/.git/",
    "/target/",
    "/dist/",
    "/build/",             # Gradle 构建产物副本(自动再生)
    "/scripts/_migrate_righttype.py",
]

INCLUDE_EXT = {".java", ".vue", ".js", ".ts", ".sql", ".py", ".json", ".spec.js"}


def excluded(path):
    p = path.replace("\\", "/")
    return any(s in p for s in EXCLUDE_SUBSTR)


def wanted(path):
    _, ext = os.path.splitext(path)
    return ext in INCLUDE_EXT


def main():
    total_files = 0
    total_hits = 0
    report = []
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if d not in
                       ("node_modules", ".git", "target", "dist", "build")]
        for fn in filenames:
            fp = os.path.join(dirpath, fn)
            if excluded(fp) or not wanted(fp):
                continue
            if fn.endswith(".bak") or ".jun13" in fn:
                continue
            try:
                with open(fp, "r", encoding="utf-8") as f:
                    text = f.read()
            except (UnicodeDecodeError, PermissionError):
                continue
            hits = sum(text.count(a) for a, _ in REPL)
            if hits == 0:
                continue
            new = text
            for a, b in REPL:
                new = new.replace(a, b)
            total_files += 1
            total_hits += hits
            report.append((os.path.relpath(fp, ROOT), hits))
            if APPLY:
                with open(fp, "w", encoding="utf-8", newline="") as f:
                    f.write(new)
    report.sort(key=lambda x: -x[1])
    for rel, hits in report:
        print("%4d  %s" % (hits, rel))
    print("\n%s files=%d  total_hits=%d" %
          ("[APPLIED]" if APPLY else "[DRY-RUN]", total_files, total_hits))


if __name__ == "__main__":
    main()
