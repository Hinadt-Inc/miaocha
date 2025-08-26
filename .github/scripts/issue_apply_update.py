#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Step B: Apply updates to the original GitHub Issue.
- Reads the JSON produced by Step A (path from $JSON_PATH).
- No fallbacks. All validations are strict; errors surface explicitly.
- Updates title/body; adds model-proposed labels (must already exist);
  removes 'ai:triage'; adds 'ai:processed' (must already exist).
- Emits a concise Step Summary.
"""

import os, sys, json
import urllib.request, urllib.error

def fail(msg: str, details: str = "") -> None:
    print(f"::error::{msg}")
    if details:
        print(f"::error::{details[:2000]}")
    sys.exit(1)

def read_event() -> dict:
    p = os.environ.get("GITHUB_EVENT_PATH")
    if not p or not os.path.exists(p):
        fail("GITHUB_EVENT_PATH is missing.")
    with open(p, "r", encoding="utf-8") as f:
        return json.load(f)

def http(url: str, method="GET", headers=None, data=None, timeout=60):
    req = urllib.request.Request(url, method=method)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, data=data, timeout=timeout) as r:
            return r.getcode(), r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()
    except Exception as e:
        return -1, str(e).encode("utf-8")

def gh_rest(path: str, method="GET", body=None):
    token = os.environ.get("GITHUB_TOKEN")
    if not token:
        fail("GITHUB_TOKEN is missing.")
    url = f"https://api.github.com{path}"
    headers = {"Authorization": f"Bearer {token}", "Accept": "application/vnd.github+json"}
    data = None if body is None else json.dumps(body).encode("utf-8")
    code, raw = http(url, method, headers, data)
    txt = raw.decode("utf-8", "ignore")
    if code < 200 or code >= 300:
        fail(f"GitHub API error: {method} {path} -> {code}", txt)
    return json.loads(txt) if txt else {}

def list_repo_labels(owner: str, repo: str) -> set[str]:
    labels, page = [], 1
    while True:
        arr = gh_rest(f"/repos/{owner}/{repo}/labels?per_page=100&page={page}")
        if not arr:
            break
        labels.extend([x.get("name") for x in arr if x.get("name")])
        if len(arr) < 100:
            break
        page += 1
    return set(labels)

def add_step_summary(md: str) -> None:
    p = os.environ.get("GITHUB_STEP_SUMMARY")
    if not p:
        return
    with open(p, "a", encoding="utf-8") as f:
        f.write(md + "\n")

def main() -> None:
    print("::notice::Apply: start")
    evt = read_event()
    issue = evt.get("issue") or {}
    repo = evt.get("repository") or {}
    owner = (repo.get("owner") or {}).get("login")
    name = repo.get("name")
    number = issue.get("number")
    if not (owner and name and number):
        fail("owner/repo/number missing in event.")

    json_path = os.environ.get("JSON_PATH")
    if not json_path or not os.path.exists(json_path):
        fail("JSON_PATH is missing or file not found.")

    with open(json_path, "r", encoding="utf-8") as f:
        obj = json.load(f)

    # Strict keys
    for k in ("title", "markdown_body", "labels"):
        if k not in obj:
            fail(f"Missing key in result JSON: {k}")

    new_title = obj["title"]
    new_body = obj["markdown_body"]
    want_labels = obj["labels"]

    if not isinstance(new_title, str) or not new_title.strip():
        fail("Invalid title in result JSON.")
    if not isinstance(new_body, str) or not new_body.strip():
        fail("Invalid markdown_body in result JSON.")
    if not isinstance(want_labels, list) or not all(isinstance(x, str) for x in want_labels):
        fail("Invalid labels in result JSON.")

    repo_labels = list_repo_labels(owner, name)
    unknown = set(want_labels) - repo_labels
    if unknown:
        fail("Result labels contain unknown values.", ", ".join(sorted(unknown)))

    # Update title & body
    print("::group::Update title/body")
    gh_rest(f"/repos/{owner}/{name}/issues/{number}", "PATCH", {
        "title": new_title,
        "body": new_body
    })
    print("::endgroup::")

    # Add labels
    print("::group::Add labels")
    if want_labels:
        gh_rest(f"/repos/{owner}/{name}/issues/{number}/labels", "POST", {"labels": want_labels})
    print("::endgroup::")

    # Remove ai:triage (if present); add ai:processed (must exist)
    print("::group::Finalize flags")
    if "ai:triage" in [l.get("name") for l in (issue.get("labels") or [])]:
        try:
            gh_rest(f"/repos/{owner}/{name}/issues/{number}/labels/ai:triage", "DELETE")
        except Exception:
            pass
    if "ai:processed" not in repo_labels:
        fail("Label 'ai:processed' does not exist in repo.")
    gh_rest(f"/repos/{owner}/{name}/issues/{number}/labels", "POST", {"labels": ["ai:processed"]})
    print("::endgroup::")

    add_step_summary(
        "### Apply Result\n"
        f"- Issue: #{number}\n"
        f"- Title updated to: **{new_title}**\n"
        f"- Labels added: {', '.join(want_labels) or '(none)'}"
    )
    print("::notice::Apply: success")

if __name__ == "__main__":
    main()

