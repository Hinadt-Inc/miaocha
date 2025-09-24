## What's Changed

This version includes several improvements and bug fixes based on community feedback.

* [ISSUE #181] Fix _source column cache logic when re-displaying after deletion (#182) by @飞雨
* [ISSUE #179] Normalize Logstash deploy path handling and cleanup (#180) by @Misaki030112
* [ISSUE #175] Fix duplicate API requests in Logstash process edit causing 'module ID cannot be empty' error (#176) by @飞雨
* [ISSUE #129] Make LogTail single-request SSE; fix multi-node routing and graceful disconnect by @Copilot
* [ISSUE #171] Fix module bug (#172) by @rdd0820
* [ISSUE #163] Fix module mismatch in log field distribution and collapse issue (#170) by @rdd0820
* [ISSUE #167] Add prompt suggestions and optimize AI assistant UI (#169) by @rdd0820
* [ISSUE #164] Enforce strict Markdown/HTML preservation in AI issue analyzer (#165) by @Misaki030112
* [ISSUE #160] Codespaces Dev Container and scripts hardening (#162) by @Misaki030112
* [ISSUE #159] Implement virtual scrolling for Data Discovery available fields module (#161) by @飞雨
* [ISSUE #157] Persist chat memory in MySQL (no history) (#158) by @Misaki030112
* [ISSUE #155] AI-driven Issue triage and Issue Forms refactor (#156) by @Misaki030112
* [ISSUE #148] Email alerts when Logstash processes go offline (#147) by @Misaki030112
* [ISSUE #141] AI 能力支持，Agent 模式自动根据需求检索日志 (#142) by @Misaki030112
* [ISSUE #151] Enable console logs in all envs; remove normal file in prod; async logging (#153) by @Misaki030112
* [ISSUE #149] Fix the problem of missing fields in SQL table created based on module (#150) by @Misaki030112

## Other Changes

- Upgrade dev version 2.0.3-SNAPSHOT


**Full Changelog**: https://github.com/Hinadt-Inc/miaocha/compare/v2.0.2...v2.0.3
