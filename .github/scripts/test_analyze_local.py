#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Local manual test runner for ai_issue_analyze.py

Usage:
  export GITHUB_TOKEN=$(gh auth token)
  export DEEPSEEK_API_KEY=...
  # Optional overrides (defaults exist in analyzer):
  # export DEEPSEEK_BASE_URL=https://api.deepseek.com/v1
  # export DEEPSEEK_MODEL=deepseek-chat
  python .github/scripts/test_analyze_local.py

Notes:
  - Run from the repository root directory.
  - This script simulates several issues (CN/EN) with free-form user text.
  - It writes temporary event payloads under .github/.ai/tmp_event_*.json
  - It executes .github/scripts/ai_issue_analyze.py per sample and prints the result path/title/labels.
"""

import json, os, subprocess, sys, pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
GITHUB_DIR = ROOT / ".github"
AI_DIR = GITHUB_DIR / ".ai"
ANALYZER = GITHUB_DIR / "scripts" / "ai_issue_analyze.py"

def ensure_env(var: str):
    v = os.environ.get(var)
    if not v:
        print(f"[ERROR] Missing env: {var}", file=sys.stderr)
        sys.exit(1)
    return v

def main():
    print("== Local AI analyze test ==")
    ensure_env("GITHUB_TOKEN")
    ensure_env("DEEPSEEK_API_KEY")

    if not ANALYZER.exists():
        print(f"[ERROR] Analyzer not found: {ANALYZER}", file=sys.stderr)
        sys.exit(1)

    # Infer repo owner/name via `gh` if possible; otherwise require env
    owner = os.environ.get("TEST_OWNER")
    name = os.environ.get("TEST_REPO")
    if not owner or not name:
        try:
            out = subprocess.check_output(["gh", "repo", "view", "--json", "nameWithOwner"], text=True)
            nwo = json.loads(out)["nameWithOwner"]
            owner, name = nwo.split("/", 1)
        except Exception:
            print("[WARN] Could not infer repo from gh. Set TEST_OWNER and TEST_REPO.")
            owner = ensure_env("TEST_OWNER")
            name = ensure_env("TEST_REPO")

    samples = [
        # CN · bug
        (8001, "Doris 连接后崩溃", "升级后连接 Doris 时服务崩溃，疑似内存溢出，请检查重连逻辑与池化配置。"),
        # CN · feature
        (8002, "需要导出 CSV", "希望查询结果支持一键导出 CSV，支持分隔符与编码选择。"),
        # CN · enhancement
        (8003, "慢查询日志优化", "慢查询统计过于粗糙，建议支持分桶统计与阈值告警。"),
        # CN · question
        (8004, "如何配置 Flyway v2?", "不知道 v2 版本迁移脚本应该放哪里，回滚脚本怎么命名？"),
        # EN · bug
        (8005, "NullPointerException during startup", "Service throws NPE during startup after bumping to Java 17.0.10. Please advise."),
        # EN · feature
        (8006, "Add metrics export", "Please add Prometheus metrics for query latency and error counts."),
    ]

    AI_DIR.mkdir(parents=True, exist_ok=True)

    for num, title, body in samples:
        tmp_event = AI_DIR / f"tmp_event_{num}.json"
        payload = {
            "issue": {
                "number": num,
                "title": title,
                "body": body,
                "labels": [{"name": "ai:triage"}]
            },
            "repository": {
                "name": name,
                "owner": {"login": owner}
            }
        }
        tmp_event.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")

        env = os.environ.copy()
        env["GITHUB_EVENT_PATH"] = str(tmp_event)

        print(f"\n--- Running analyze for sample #{num}: {title} ---")
        proc = subprocess.run([sys.executable, str(ANALYZER)], env=env)
        if proc.returncode != 0:
            print(f"[FAIL] analyze failed for #{num}")
            sys.exit(proc.returncode)

        out_json = GITHUB_DIR / ".ai" / f"issue_{num}.json"
        if not out_json.exists():
            print(f"[FAIL] result json not found: {out_json}")
            sys.exit(1)

        data = json.loads(out_json.read_text(encoding="utf-8"))
        print(f"[OK] -> {out_json}")
        print(f"     template_file: {data.get('template_file')}")
        print(f"     title        : {data.get('title')}")
        print(f"     labels       : {data.get('labels')}")
        preview = ' '.join(((data.get('markdown_body') or '')[:160]).splitlines())
        print(f"     body preview : {preview} ...")

    print("\n== All samples analyzed successfully ==")

if __name__ == "__main__":
    main()

