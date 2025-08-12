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
