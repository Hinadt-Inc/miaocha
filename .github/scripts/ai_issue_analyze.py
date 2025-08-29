#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Step A: Analyze issue via LLM and produce STRICT JSON.
- No fallbacks. Missing env or invalid response => explicit failure.
- Reads ACTUAL issue templates (.github/ISSUE_TEMPLATE/*.yml) and ACTUAL repo labels.
- Instructs model to return ONLY raw JSON (no fences, no extra text).
- Validates: JSON schema, template_file from provided list, title non-empty,
  markdown_body non-empty, and labels subset of repo labels with at least one of {bug|feature|enhancement|question}.
- On parse/validation failure, retries up to 3 times, each time appending prior
  assistant content + explicit error reason to the conversation.
- Outputs a JSON file path via $GITHUB_OUTPUT for the next step to consume.
"""

import os, sys, json, glob
import urllib.request, urllib.error

RETRY_MAX = 3

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

def list_repo_labels(owner: str, repo: str) -> list[str]:
    labels, page = [], 1
    while True:
        arr = gh_rest(f"/repos/{owner}/{repo}/labels?per_page=100&page={page}")
        if not arr:
            break
        labels.extend([x.get("name") for x in arr if x.get("name")])
        if len(arr) < 100:
            break
        page += 1
    return sorted(set(labels))

def read_templates() -> dict[str, str]:
    """Read raw YAML texts of issue forms (excluding config.yml)."""
    out = {}
    for f in sorted(glob.glob(".github/ISSUE_TEMPLATE/*.yml")):
        base = os.path.basename(f).lower()
        if base == "config.yml":
            continue
        with open(f, "r", encoding="utf-8") as fp:
            out[os.path.basename(f)] = fp.read()
    if not out:
        fail("No issue templates found in .github/ISSUE_TEMPLATE.")
    return out

def deepseek_chat(messages: list[dict]) -> str:
    """Call DeepSeek Chat Completions. Uses sensible defaults for base/model."""
    # Defaults for convenience
    base = os.environ.get("DEEPSEEK_BASE_URL") or "https://api.deepseek.com/v1"
    model = os.environ.get("DEEPSEEK_MODEL") or "deepseek-chat"
    key = os.environ.get("DEEPSEEK_API_KEY")
    if not key:
        fail("DEEPSEEK_API_KEY is missing.")

    url = f"{base.rstrip('/')}/chat/completions"
    headers = {"Authorization": f"Bearer {key}", "Content-Type": "application/json"}
    payload = {"model": model, "messages": messages}
    code, raw = http(url, "POST", headers, json.dumps(payload).encode("utf-8"))
    if code < 200 or code >= 300:
        fail("DeepSeek API call failed.", raw.decode("utf-8", "ignore"))
    data = json.loads(raw.decode("utf-8", "ignore"))
    return data["choices"][0]["message"]["content"]

def strict_load_json(s: str) -> dict | None:
    """Strictly parse a JSON object; no trimming/heuristics."""
    try:
        obj = json.loads(s)
        return obj if isinstance(obj, dict) else None
    except Exception:
        return None

def validate_result(obj: dict, allowed_templates: set[str], repo_labels: set[str]) -> None:
    """Raise on any validation error (no defaults)."""
    schema = ["template_file", "title", "labels", "markdown_body"]
    for k in schema:
        if k not in obj:
            raise ValueError(f"Missing key: {k}")

    template_file = obj["template_file"]
    title = obj["title"]
    labels = obj["labels"]
    body = obj["markdown_body"]

    if not isinstance(template_file, str) or template_file not in allowed_templates:
        raise ValueError("template_file must be one of: " + ", ".join(sorted(allowed_templates)))
    if not isinstance(title, str) or not title.strip():
        raise ValueError("title must be a non-empty string")
    if not isinstance(body, str) or not body.strip():
        raise ValueError("markdown_body must be a non-empty string")
    if not isinstance(labels, list) or not all(isinstance(x, str) for x in labels):
        raise ValueError("labels must be an array of strings")

    # Enforce: all labels must exist in repo, and include exactly one core type label
    labels_set = set(labels)
    if not labels_set.issubset(repo_labels):
        unknown = labels_set - repo_labels
        raise ValueError("labels contain unknown values: " + ", ".join(sorted(unknown)))

    type_candidates = {"bug", "feature", "enhancement", "question"}
    exists_type = list(labels_set & type_candidates)
    if len(exists_type) != 1:
        raise ValueError("labels must include exactly ONE of {bug|feature|enhancement|question}")

    if "ai:triage" in labels_set:
        raise ValueError("labels must NOT include 'ai:triage'")

def write_output(name: str, value: str) -> None:
    out = os.environ.get("GITHUB_OUTPUT")
    if not out:
        return
    with open(out, "a", encoding="utf-8") as f:
        f.write(f"{name}={value}\n")

def add_step_summary(md: str) -> None:
    p = os.environ.get("GITHUB_STEP_SUMMARY")
    if not p:
        return
    with open(p, "a", encoding="utf-8") as f:
        f.write(md + "\n")

def main() -> None:
    print("::notice::Analyze: start")
    evt = read_event()
    issue = evt.get("issue") or {}
    repo = evt.get("repository") or {}
    owner = (repo.get("owner") or {}).get("login")
    name = repo.get("name")
    number = issue.get("number")
    if not (owner and name and number):
        fail("owner/repo/number missing in event.")

    raw_title = issue.get("title") or ""
    raw_body = issue.get("body") or ""

    templates = read_templates()
    template_names = set(templates.keys())
    repo_labels = set(list_repo_labels(owner, name))

    # System & User prompts: MUST return ONLY raw JSON (no extra text)
    system_msg = {
        "role": "system",
        "content": (
            # === ROLE ===
            "You are the AI Issue Triage assistant for THIS repository.\n"
            "Your job is to transform a user's free-form GitHub issue into a structured issue that matches one of the repository's actual Issue Form templates.\n"
            "You should optimize the user's expression without changing the semantics, using a polite tone. Always write in English, even if the input is in another language.\n"

            # === INPUTS YOU WILL RECEIVE ===
            "- raw_issue: the original user-provided title and body, sometimes input may include image links, code blocks, etc. You need to ensure that this objective data is not lost and tampered with.\n"
            "- issue_form_templates: a DICTIONARY of the repository’s ACTUAL Issue Form YAML files (filename => raw YAML string)\n"
            "- repo_labels: the ACTUAL labels that exist in this repository (array of strings)\n"

            # === WHAT YOU MUST DO ===
            "1) Decide exactly ONE template_file from the provided `issue_form_templates` (choose by filename).\n"
            "   Use the YAML contents to understand the form's intention and major fields (e.g., bug report vs feature request).\n"
            "2) Craft a concise, specific, high-signal `title` that captures the user's intent (< 80 chars preferred).\n"
            "3) Produce a FULL `markdown_body` that mirrors the chosen template’s structure:\n"
            "   - Derive section headings primarily from the template’s form items (e.g., an item's `label` becomes an H2 heading `## <label>`).\n"
            "   - Organize the user's information under those headings. Keep code/logs in fenced code blocks.\n"
            "   - Do NOT invent facts. If the user didn't provide data for a section, summarize what is known or leave that section with TODO bullets.\n"
            "   - The body must be READY-TO-USE as Markdown in GitHub (no placeholders like <fill here>).\n"
            "4) Choose `labels` ONLY from `repo_labels`:\n"
            "   - Include EXACTLY ONE of these type labels: {bug, feature, enhancement, question}.\n"
            "   - NEVER include 'ai:triage'.\n"
            "   - You MAY add up to 3 area:* labels and optionally one severity:* and one priority:* IF AND ONLY IF they exist in repo_labels and are clearly relevant.\n"

            # === STRICT MARKDOWN/HTML PRESERVATION POLICY ===\n"
            "Strict Markdown/HTML Preservation Policy\n"
            "\n"
            "Do not alter, normalize, or move any non-textual blocks. Preserve them byte-for-byte, exactly as given in the input:\n"
            "- Images:\n"
            "  - Keep Markdown images `![alt](url \"title\")` EXACTLY unchanged.\n"
            "  - Keep HTML `<img .../>` EXACTLY unchanged (attribute order, spacing, quotes, case).\n"
            "  - NEVER convert images to plain links. NEVER change alt/title/src/width/height. NEVER inline data URIs.\n"
            "- Code:\n"
            "  - Keep fenced code blocks (triple backticks + language) EXACTLY unchanged, including indentation and blank lines.\n"
            "  - Keep inline code `...` EXACTLY unchanged.\n"
            "- HTML blocks:\n"
            "  - Keep any raw HTML EXACTLY unchanged (e.g., `<details>`, `<summary>`, `<table>`, `<thead>`, `<tbody>`, `<tr>`, `<th>`, `<td>`, comments).\n"
            "- Markdown structures:\n"
            "  - Keep tables, blockquotes, lists (including checkboxes), footnotes, link reference definitions, anchors, and issue/PR references EXACTLY unchanged.\n"
            "- Unknown or uncertain content:\n"
            "  - If unsure whether a block is textual or structural, copy it verbatim without any modification.\n"
            "\n"
            "What you MAY rewrite:\n"
            "- Only the natural-language prose outside preserved blocks (titles, headings, paragraphs, step lists), for clarity and conciseness.\n"
            "- Keep original meaning and the original language unless instructed otherwise.\n"
            "- Do NOT change existing URLs or link texts; do NOT introduce or remove images.\n"
            "\n"
            "Output requirements:\n"
            "- Produce valid GitHub-flavored Markdown.\n"
            "- Keep all preserved blocks byte-for-byte identical, in the same order and position relative to surrounding text.\n"
            "- Do NOT re-wrap lines or adjust whitespace inside preserved blocks.\n"
            "- Do NOT convert images to links; do NOT emit naked image URLs where an image block existed.\n"
            "- If any preservation rule would be violated, return the input unchanged.\n"
            "\n"
            "Final self-check before answering:\n"
            "- All original Markdown images are still present and identical.\n"
            "- All original HTML `<img>` tags are still present and identical.\n"
            "- All fenced code blocks and inline code are identical.\n"
            "- No image became a plain link. No preserved block was re-ordered or reformatted.\n"
            "- If uncertain, output the original input unmodified.\n"

            # === DECISION RUBRIC ===
            "- Choose template_file by intent:\n"
            "  * bug -> incorrect behavior, crashes, errors, exceptions, repro steps, 'bug/crash/error/exception' words.\n"
            "  * feature -> a new capability that doesn't exist yet.\n"
            "  * enhancement -> improve an existing capability/performance/refactor.\n"
            "  * question -> asking for help/clarification/How-to.\n"

            # === OUTPUT CONTRACT ===
            "Return ONLY a single JSON object with EXACTLY these keys and types:\n"
            "{\n"
            "  \"template_file\": \"<one of the provided YAML filenames>\",\n"
            "  \"title\": \"string\",\n"
            "  \"labels\": [\"string\", ...],\n"
            "  \"markdown_body\": \"string\"\n"
            "}\n"
            "The title must use one of these prefixes based on intent: [Bug], [Feature], [Enhancement], or [Q&A].\n"
            "Return ONLY raw JSON (UTF-8, no BOM), with no surrounding code fences, headers, or commentary.\n"
            "No extra keys, no trailing commas. The top-level value must be a single JSON object.\n"
        )
    }
    user_payload = {
        "raw_issue": {"title": raw_title, "body": raw_body},
        "repo_labels": sorted(repo_labels),
        "issue_form_templates": templates
    }
    messages = [system_msg, {"role": "user", "content": json.dumps(user_payload, ensure_ascii=False)}]

    last_assistant = ""
    for attempt in range(1, RETRY_MAX + 1):
        print(f"::notice::Analyze: attempt {attempt}/{RETRY_MAX}")
        assistant_text = deepseek_chat(messages)
        last_assistant = assistant_text
        obj = strict_load_json(assistant_text)
        if obj is None:
            # Append error and retry
            messages.append({"role": "assistant", "content": assistant_text})
            messages.append({"role": "user", "content":
                "ERROR: Your previous reply was not valid JSON. "
                "Return ONLY raw JSON with the exact schema and keys. No code fences. No extra text."})
            continue
        try:
            validate_result(obj, template_names, repo_labels)
        except Exception as e:
            # Feed back the exact error and retry
            messages.append({"role": "assistant", "content": assistant_text})
            messages.append({"role": "user", "content":
                f"VALIDATION ERROR: {str(e)}. Fix and return ONLY raw JSON. No code fences. No extra text."})
            continue

        # Valid result -> save and exit
        os.makedirs(".github/.ai", exist_ok=True)
        out_path = f".github/.ai/issue_{number}.json"
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(obj, f, ensure_ascii=False, indent=2)
        write_output("json_path", out_path)

        add_step_summary(
            "### Analyze Result\n"
            f"- Template file: `{obj['template_file']}`\n"
            f"- New title: **{obj['title']}**\n"
            f"- Labels: {', '.join(obj['labels']) or '(none)'}\n\n"
            "<details><summary>Preview body</summary>\n\n"
            f"{obj['markdown_body']}\n\n"
            "</details>"
        )
        print("::notice::Analyze: success")
        return

    # If here, all attempts failed -> error out
    fail("LLM analysis failed after retries.", last_assistant)

if __name__ == "__main__":
    main()
