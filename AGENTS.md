# Language Usage
1. By default, communicate with the user in Chinese. When actually writing code, whether in comments or in the code itself, all logs must be in English.
2. If the user communicates with you in English, respond in English accordingly.
3. When submitting a git commit, use English.
4. When creating GitHub issues, pull requests, or performing any operations on GitHub, use English.
5. When writing, refactoring, or modifying existing code or files, if there are Chinese comments or Chinese logs in the code or anything chinese, convert them to English.

# Coding Rules
1. Always ensure elegance and reusability in your code.
2. Do not add redundant logs; logs should only be placed where necessary. Avoid adding meaningless logs as personal reminders.
3. When handling exceptions, think about whether the exception can be perceived and handled by the user. Exceptions that can be perceived and handled by the user are meaningful and should have specific error codes defined.
4. **Do not commit or push code without my explicit approval.**

## Logging Rules
- Language: All logs must be in English.
- Framework: Prefer `@Slf4j` (Lombok) with parameterized logging; avoid string concatenation.
- Level semantics:
  - ERROR: User-visible failures or critical bugs. Include concise context, avoid sensitive data.
  - WARN: Unexpected but recoverable conditions, potential misconfiguration. Use sparingly.
  - INFO: Important state changes or key business events only (e.g., an alert sent). Do NOT use for tracking, heartbeats, or periodic noise.
  - DEBUG: Routine operation details, periodic/scheduled task starts and finishes, developer diagnostics.
  - TRACE: Extremely verbose details for deep debugging only.
- Noise control:
  - Do not abuse INFO+ levels; never turn logs into tracking/telemetry. Use metrics/tracing for observability.
  - Avoid flooding within loops or schedulers; add guards, sampling, or rate-limiting when needed.
  - Prefer structured and actionable messages (ids, counts), but no secrets/PII.
  - Log stack traces only when necessary; otherwise log concise messages with correlation context.

## Database (Flyway) Rules
- Use Flyway for all schema changes. Current major is `v2` (2.x.y), so place migrations under `src/main/resources/db/migration/v2`.
- Every schema change must include:
    - A forward migration SQL file `V<version>__<Title>.sql` in `v2`.
    - A matching undo SQL file `UNDO_V<version>_SAFE__<Title>.sql` under `db/migration/undo`.
- Keep naming consistent with existing files (use concise, descriptive English titles).
- Forward migrations must be idempotent where possible and safe for production; undo scripts should clearly state data loss risks if any.
- When adding columns/tables used by code, update corresponding entity classes and MyBatis mappers in the same change.


# Testing Rules
## Unit Testing Rules
1. When writing unit tests, clearly understand the user’s intent, identify the specific functionality to be tested, and write tests based on actual business logic rather than high-level abstraction.
2. Do not perform absurd mocking (e.g., mocking a calculator). Only mock components that interact with external systems, such as database queries or third-party services.
3. Maintain critical thinking when writing unit tests. Consider what the correct result should be based on the actual code and business scenario, rather than making the test pass without verifying correctness.
4. If there are many test cases, group them appropriately.
5. Since the `miaocha-ui` module is time-consuming to compile, it is recommended to add the `-Pskip-ui` parameter when running unit tests.

## Integration Testing Rules
1. When writing integration tests, use the **testcontainers** framework to simulate a real external environment. Mocking is prohibited unless absolutely necessary.
2. Integration tests should cover the entire functional logic, clearly listing all branch cases. Plan test cases first, review them, and remove any unnecessary ones.
3. Keep integration test validations rigorous — validate from the outside while also ensuring the internal implementation fully meets the expected behavior.
4. If there are too many test cases in one class, group them appropriately.

# Additional Rules
1. In addition to the above rules, you must also follow the rules in the `.cursor/rules` folder that apply to Cursor.
2. If the above rules conflict with other files’ rules, follow the rules in this document.
 


# GitHub PR & ISSUE Rules

1. Always use **English** for PR and ISSUE.
2. Follow the templates:
    - ISSUE → `.github/ISSUE_TEMPLATE`
    - PR → `.github/PULL_REQUEST_TEMPLATE`
3. Use **precise and concise** wording; avoid redundancy.

## PR–Issue Mapping Policy

- One PR ↔ One Issue: Every Pull Request MUST correspond to exactly one Issue. Do not bundle unrelated changes under a single PR.
- Ensure Issue exists first: Before opening a PR, verify there is an existing Issue describing the change. If none exists, create a new Issue using the appropriate template from `.github/ISSUE_TEMPLATE` (bug/feature/enhancement/question). Fill it thoroughly, including the Environment table where applicable.
- Title format: PR title MUST follow the repository template format `[ISSUE #xx] Short description`, where `#xx` is the real Issue number.
- PR description: Use the PR template strictly. Do not modify the structure or omit required sections. Explain What, How, and Why. Link the Issue explicitly (e.g., `Closes #xx`).
- Target branch: Always open PRs against the `dev` branch; do not target `main`.
- No fabricated references: Do not invent or reuse incorrect Issue numbers. If new scope emerges, open a separate Issue and submit a separate PR.
- Scope discipline: Keep changes in a PR limited to the Issue’s scope. If the work spans multiple concerns, split into multiple Issues and PRs.
